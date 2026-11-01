package com.hmp.sales.feature.search.service;

import com.hmp.sales.feature.category.entity.Category;
import com.hmp.sales.feature.category.entity.CategoryStatus;
import com.hmp.sales.feature.category.repository.CategoryRepository;
import com.hmp.sales.feature.product.entity.Brand;
import com.hmp.sales.feature.product.entity.BrandStatus;
import com.hmp.sales.feature.product.entity.Product;
import com.hmp.sales.feature.product.entity.ProductStatus;
import com.hmp.sales.feature.product.repository.BrandRepository;
import com.hmp.sales.feature.product.repository.ProductRepository;
import com.hmp.sales.feature.search.dto.SearchResourceType;
import com.hmp.sales.feature.search.dto.SearchSuggestionResponse;
import com.hmp.sales.feature.search.event.SearchIndexInvalidationEvent;
import com.hmp.sales.feature.search.service.impl.SearchSuggestionServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("SearchSuggestionServiceImpl Unit Tests")
class SearchSuggestionServiceImplTest {

    @Mock
    private ProductRepository productRepository;

    @Mock
    private CategoryRepository categoryRepository;

    @Mock
    private BrandRepository brandRepository;

    private SearchSuggestionServiceImpl searchSuggestionService;

    private Product product1;
    private Product product2;
    private Category category1;
    private Brand brand1;

    @BeforeEach
    void setUp() {
        searchSuggestionService = new SearchSuggestionServiceImpl(
                productRepository, categoryRepository, brandRepository, null);

        // Chuẩn bị dữ liệu mẫu
        category1 = Category.builder()
                .id(1L)
                .categoryName("Phụ kiện lợp mái")
                .categoryNameUnaccent("phu kien lop mai")
                .status(CategoryStatus.ACTIVE)
                .isDeleted(false)
                .build();

        brand1 = Brand.builder()
                .id(1L)
                .brandName("Hoa Sen")
                .brandNameUnaccent("hoa sen")
                .status(BrandStatus.ACTIVE)
                .isDeleted(false)
                .build();

        product1 = Product.builder()
                .id(101L)
                .productName("Tôn sóng lợp mái Hoa Sen")
                .productNameUnaccent("ton song lop mai hoa sen")
                .status(ProductStatus.ACTIVE)
                .isDeleted(false)
                .category(category1)
                .images(new ArrayList<>())
                .build();

        product2 = Product.builder()
                .id(102L)
                .productName("Tôn Đông Á lạnh màu")
                .productNameUnaccent("ton dong a lanh mau")
                .status(ProductStatus.ACTIVE)
                .isDeleted(false)
                .category(category1)
                .images(new ArrayList<>())
                .build();

        Product product3 = Product.builder()
                .id(103L)
                .productName("Tôn Hoa Sen 11 sóng vuông")
                .productNameUnaccent("ton hoa sen 11 song vuong")
                .status(ProductStatus.ACTIVE)
                .isDeleted(false)
                .category(category1)
                .images(new ArrayList<>())
                .build();

        when(productRepository.findAllActiveForSearchIndex()).thenReturn(List.of(product1, product2, product3));
        when(categoryRepository.findAll()).thenReturn(List.of(category1));
        when(brandRepository.findAll()).thenReturn(List.of(brand1));

        // Nạp chỉ mục ban đầu
        searchSuggestionService.reloadAll();
    }

    @Nested
    @DisplayName("Tests tìm kiếm không dấu (Unaccent Search)")
    class UnaccentSearchTests {

        @Test
        @DisplayName("Tìm từ khóa 'ton' -> tìm thấy các sản phẩm Tôn sóng, Tôn Đông Á, Tôn Hoa Sen")
        void shouldFindProductsWithUnaccentKeyword() {
            List<SearchSuggestionResponse> result = searchSuggestionService.getSuggestions("ton", SearchResourceType.PRODUCT, 10);

            assertThat(result).hasSize(3);
        }

        @Test
        @DisplayName("Gõ có dấu 'tôn' -> vẫn tìm thấy các sản phẩm")
        void shouldFindProductsWithAccentedKeyword() {
            List<SearchSuggestionResponse> result = searchSuggestionService.getSuggestions("tôn", SearchResourceType.PRODUCT, 10);

            assertThat(result).hasSize(3);
        }

        @Test
        @DisplayName("Gõ sai nguyên âm 'lọp' (thay vì 'lợp') -> tìm thấy 'Tôn sóng lợp mái'")
        void shouldFindProductWhenTypingWrongVowelLop() {
            List<SearchSuggestionResponse> result = searchSuggestionService.getSuggestions("lọp", SearchResourceType.PRODUCT, 10);

            assertThat(result).hasSize(1);
            assertThat(result.get(0).name()).isEqualTo("Tôn sóng lợp mái Hoa Sen");
        }

        @Test
        @DisplayName("Tìm kiếm thương hiệu 'dong a' -> tìm thấy 'Tôn Đông Á'")
        void shouldFindProductWithUnaccentBrandName() {
            List<SearchSuggestionResponse> result = searchSuggestionService.getSuggestions("dong a", SearchResourceType.PRODUCT, 10);

            assertThat(result).hasSize(1);
            assertThat(result.get(0).name()).isEqualTo("Tôn Đông Á lạnh màu");
        }
    }

    @Nested
    @DisplayName("Tests sửa lỗi gõ nhầm bàn phím (Fuzzy Typo Tolerance)")
    class FuzzyTypoToleranceTests {

        @Test
        @DisplayName("Gõ thiếu chữ 'ton sog' (khoảng cách 1 so với 'ton song') -> sửa lỗi tìm ra sản phẩm tôn sóng")
        void shouldFindWithMissingLetterTypo() {
            List<SearchSuggestionResponse> result = searchSuggestionService.getSuggestions("ton sog", SearchResourceType.PRODUCT, 10);

            assertThat(result).isNotEmpty();
            assertThat(result.get(0).name()).isEqualTo("Tôn sóng lợp mái Hoa Sen");
        }

        @Test
        @DisplayName("Gõ thừa chữ cuối 'ton hoa sen 11 song vuongg' -> sửa lỗi tìm ra 'Tôn Hoa Sen 11 sóng vuông'")
        void shouldFindWithLongMultiWordTypoKeyword() {
            List<SearchSuggestionResponse> result = searchSuggestionService.getSuggestions(
                    "ton hoa sen 11 song vuongg", SearchResourceType.PRODUCT, 10);

            assertThat(result).isNotEmpty();
            assertThat(result.get(0).name()).isEqualTo("Tôn Hoa Sen 11 sóng vuông");
        }

        @Test
        @DisplayName("Gõ thừa chữ 'hoa senn' -> sửa lỗi tìm ra thương hiệu 'Hoa Sen'")
        void shouldFindWithExtraLetterTypo() {
            List<SearchSuggestionResponse> result = searchSuggestionService.getSuggestions("hoa senn", SearchResourceType.BRAND, 10);

            assertThat(result).isNotEmpty();
            assertThat(result.get(0).name()).isEqualTo("Hoa Sen");
        }

        @Test
        @DisplayName("Từ khóa ngắn < 3 ký tự không kích hoạt Levenshtein để tránh gợi ý rác")
        void shouldNotTriggerFuzzyOnVeryShortKeywords() {
            List<SearchSuggestionResponse> result = searchSuggestionService.getSuggestions("xy", SearchResourceType.PRODUCT, 10);

            assertThat(result).isEmpty();
        }
    }

    @Nested
    @DisplayName("Tests tìm kiếm tổng hợp và giới hạn kết quả (Filtering & Limiting)")
    class FilterAndLimitingTests {

        @Test
        @DisplayName("Tìm kiếm tổng hợp không truyền type -> trả về cả sản phẩm, danh mục, thương hiệu")
        void shouldReturnMixedResultsWhenTypeIsNull() {
            List<SearchSuggestionResponse> result = searchSuggestionService.getSuggestions("hoa sen", null, 10);

            // Cả sản phẩm "Tôn sóng lợp mái Hoa Sen", "Tôn Hoa Sen 11 sóng vuông" và thương hiệu "Hoa Sen" đều có mặt
            assertThat(result).hasSize(3);
            assertThat(result).extracting(SearchSuggestionResponse::type)
                    .contains(SearchResourceType.PRODUCT, SearchResourceType.BRAND);
        }

        @Test
        @DisplayName("Tôn trọng tham số limit tối đa")
        void shouldRespectLimitParam() {
            List<SearchSuggestionResponse> result = searchSuggestionService.getSuggestions("ton", SearchResourceType.PRODUCT, 1);

            assertThat(result).hasSize(1);
        }

        @Test
        @DisplayName("Từ khóa rỗng hoặc null -> trả về danh sách trống")
        void shouldReturnEmptyForBlankKeyword() {
            assertThat(searchSuggestionService.getSuggestions(null, null, 10)).isEmpty();
            assertThat(searchSuggestionService.getSuggestions("   ", null, 10)).isEmpty();
        }
    }

    @Nested
    @DisplayName("Tests đồng bộ hóa sự kiện chỉ mục (Cache Invalidation)")
    class CacheInvalidationTests {

        @Test
        @DisplayName("Khi nhận sự kiện DELETE sản phẩm -> lập tức xóa khỏi danh sách gợi ý trong RAM")
        void shouldRemoveItemOnDeleteEvent() {
            // Trước khi xóa: tìm thấy 3 sản phẩm
            assertThat(searchSuggestionService.getSuggestions("ton", SearchResourceType.PRODUCT, 10)).hasSize(3);

            // Bắn event xóa sản phẩm 101
            searchSuggestionService.handleInvalidation(new SearchIndexInvalidationEvent(
                    SearchResourceType.PRODUCT, 101L, SearchIndexInvalidationEvent.Operation.DELETE));

            // Sau khi xóa: chỉ còn 2 sản phẩm
            List<SearchSuggestionResponse> result = searchSuggestionService.getSuggestions("ton", SearchResourceType.PRODUCT, 10);
            assertThat(result).hasSize(2);
            assertThat(result).extracting(SearchSuggestionResponse::id).containsExactly(102L, 103L);
        }

        @Test
        @DisplayName("Khi nhận sự kiện UPSERT sản phẩm mới -> cập nhật thêm vào RAM")
        void shouldAddItemOnUpsertEvent() {
            Product newProduct = Product.builder()
                    .id(103L)
                    .productName("Tôn Nam Kim cách nhiệt")
                    .productNameUnaccent("ton nam kim cach nhiet")
                    .status(ProductStatus.ACTIVE)
                    .isDeleted(false)
                    .images(new ArrayList<>())
                    .build();

            when(productRepository.findByIdForSearchIndex(103L)).thenReturn(Optional.of(newProduct));

            // Bắn event thêm mới sản phẩm 103
            searchSuggestionService.handleInvalidation(new SearchIndexInvalidationEvent(
                    SearchResourceType.PRODUCT, 103L, SearchIndexInvalidationEvent.Operation.UPSERT));

            List<SearchSuggestionResponse> result = searchSuggestionService.getSuggestions("nam kim", SearchResourceType.PRODUCT, 10);
            assertThat(result).hasSize(1);
            assertThat(result.get(0).name()).isEqualTo("Tôn Nam Kim cách nhiệt");
        }
    }
}

