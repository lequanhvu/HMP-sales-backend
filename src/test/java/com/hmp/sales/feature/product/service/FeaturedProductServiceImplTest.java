package com.hmp.sales.feature.product.service;

import com.hmp.sales.common.exception.BusinessRuleException;
import com.hmp.sales.common.exception.ResourceNotFoundException;
import com.hmp.sales.feature.category.entity.Category;
import com.hmp.sales.feature.product.dto.FeaturedProductCreateRequest;
import com.hmp.sales.feature.product.dto.FeaturedProductResponse;
import com.hmp.sales.feature.product.dto.FeaturedProductUpdateRequest;
import com.hmp.sales.feature.product.entity.Brand;
import com.hmp.sales.feature.product.entity.FeaturedProduct;
import com.hmp.sales.feature.product.entity.FeaturedProductStatus;
import com.hmp.sales.feature.product.entity.Product;
import com.hmp.sales.feature.product.entity.ProductStatus;
import com.hmp.sales.feature.product.repository.FeaturedProductRepository;
import com.hmp.sales.feature.product.repository.ProductRepository;
import com.hmp.sales.feature.product.service.impl.FeaturedProductServiceImpl;
import com.hmp.sales.feature.systemlog.service.SystemLogService;
import com.hmp.sales.feature.user.entity.User;
import com.hmp.sales.feature.user.repository.UserRepository;
import com.hmp.sales.security.PermissionEvaluationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests cho {@link FeaturedProductServiceImpl}.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("FeaturedProductServiceImpl Unit Tests")
class FeaturedProductServiceImplTest {

    @Mock
    private FeaturedProductRepository featuredProductRepository;

    @Mock
    private ProductRepository productRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private PermissionEvaluationService permissionEvaluationService;

    @Mock
    private SystemLogService systemLogService;

    @InjectMocks
    private FeaturedProductServiceImpl featuredProductService;

    private Product product;
    private FeaturedProduct featuredProduct;
    private User manager;

    @BeforeEach
    void setUp() {
        Category category = Category.builder()
                .id(10L)
                .categoryName("Tôn sóng vuông")
                .isDeleted(false)
                .build();

        Brand brand = Brand.builder()
                .id(5L)
                .brandName("Tôn Hoa Sen")
                .isDeleted(false)
                .build();

        product = Product.builder()
                .id(100L)
                .productName("Tôn Hoa Sen 11 sóng")
                .category(category)
                .brand(brand)
                .status(ProductStatus.ACTIVE)
                .isDeleted(false)
                .images(new ArrayList<>())
                .build();

        manager = User.builder()
                .id(1L)
                .fullName("Quản lý 1")
                .email("manager1@hmp.vn")
                .isDeleted(false)
                .build();

        featuredProduct = FeaturedProduct.builder()
                .id(1L)
                .product(product)
                .displayOrder(1)
                .status(FeaturedProductStatus.ACTIVE)
                .createdBy(manager)
                .createdAt(Instant.now())
                .isDeleted(false)
                .build();
    }

    @Test
    @DisplayName("getPublicFeaturedProducts - Lấy danh sách sản phẩm nổi bật công khai thành công")
    void getPublicFeaturedProducts_success() {
        when(permissionEvaluationService.canViewProductPrice()).thenReturn(true);
        when(featuredProductRepository.findByStatusAndIsDeletedFalseOrderByDisplayOrderAscCreatedAtDesc(FeaturedProductStatus.ACTIVE))
                .thenReturn(List.of(featuredProduct));

        List<FeaturedProductResponse> responses = featuredProductService.getPublicFeaturedProducts();

        assertThat(responses).hasSize(1);
        FeaturedProductResponse resp = responses.get(0);
        assertThat(resp.id()).isEqualTo(1L);
        assertThat(resp.productId()).isEqualTo(100L);
        assertThat(resp.productName()).isEqualTo("Tôn Hoa Sen 11 sóng");
        assertThat(resp.displayOrder()).isEqualTo(1);
        assertThat(resp.priceVisible()).isTrue();
    }

    @Test
    @DisplayName("getAllFeaturedProducts - Quản lý lấy toàn bộ danh sách sản phẩm nổi bật")
    void getAllFeaturedProducts_success() {
        when(permissionEvaluationService.canViewProductPrice()).thenReturn(false);
        when(featuredProductRepository.findByIsDeletedFalseOrderByDisplayOrderAscCreatedAtDesc())
                .thenReturn(List.of(featuredProduct));

        List<FeaturedProductResponse> responses = featuredProductService.getAllFeaturedProducts();

        assertThat(responses).hasSize(1);
        assertThat(responses.get(0).priceVisible()).isFalse();
    }

    @Test
    @DisplayName("getFeaturedProductById - Tìm thấy bản ghi trả về đúng DTO")
    void getFeaturedProductById_success() {
        when(featuredProductRepository.findByIdAndIsDeletedFalse(1L)).thenReturn(Optional.of(featuredProduct));
        when(permissionEvaluationService.canViewProductPrice()).thenReturn(true);

        FeaturedProductResponse resp = featuredProductService.getFeaturedProductById(1L);

        assertThat(resp.id()).isEqualTo(1L);
        assertThat(resp.productName()).isEqualTo("Tôn Hoa Sen 11 sóng");
    }

    @Test
    @DisplayName("getFeaturedProductById - Không tìm thấy bản ghi ném ResourceNotFoundException")
    void getFeaturedProductById_notFound_throwsException() {
        when(featuredProductRepository.findByIdAndIsDeletedFalse(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> featuredProductService.getFeaturedProductById(999L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Không tìm thấy sản phẩm nổi bật");
    }

    @Test
    @DisplayName("addFeaturedProduct - Thêm mới sản phẩm nổi bật thành công")
    void addFeaturedProduct_success() {
        FeaturedProductCreateRequest request = new FeaturedProductCreateRequest(100L, 2, FeaturedProductStatus.ACTIVE);
        when(productRepository.findByIdAndIsDeletedFalse(100L)).thenReturn(Optional.of(product));
        when(featuredProductRepository.existsByProductIdAndIsDeletedFalse(100L)).thenReturn(false);
        when(userRepository.findById(1L)).thenReturn(Optional.of(manager));
        when(featuredProductRepository.save(any(FeaturedProduct.class))).thenAnswer(inv -> {
            FeaturedProduct fp = inv.getArgument(0);
            fp.setId(2L);
            fp.setCreatedAt(Instant.now());
            return fp;
        });

        FeaturedProductResponse resp = featuredProductService.addFeaturedProduct(request, 1L);

        assertThat(resp.id()).isEqualTo(2L);
        assertThat(resp.productId()).isEqualTo(100L);
        assertThat(resp.displayOrder()).isEqualTo(2);

        verify(systemLogService).log(eq(1L), eq("FEATURED_PRODUCT_ADDED"), eq("FEATURED_PRODUCT"), eq(2L), any());
    }

    @Test
    @DisplayName("addFeaturedProduct - Không tìm thấy sản phẩm ném ResourceNotFoundException")
    void addFeaturedProduct_productNotFound_throwsException() {
        FeaturedProductCreateRequest request = new FeaturedProductCreateRequest(999L);
        when(productRepository.findByIdAndIsDeletedFalse(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> featuredProductService.addFeaturedProduct(request, 1L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Không tìm thấy sản phẩm với ID: 999");
    }

    @Test
    @DisplayName("addFeaturedProduct - Sản phẩm không ở trạng thái ACTIVE ném BusinessRuleException")
    void addFeaturedProduct_productNotActive_throwsException() {
        product.setStatus(ProductStatus.INACTIVE);
        FeaturedProductCreateRequest request = new FeaturedProductCreateRequest(100L);
        when(productRepository.findByIdAndIsDeletedFalse(100L)).thenReturn(Optional.of(product));

        assertThatThrownBy(() -> featuredProductService.addFeaturedProduct(request, 1L))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessage("Chỉ có thể thêm sản phẩm đang ở trạng thái ACTIVE vào danh sách sản phẩm nổi bật.");
    }

    @Test
    @DisplayName("addFeaturedProduct - Sản phẩm đã có trong danh sách nổi bật ném BusinessRuleException")
    void addFeaturedProduct_alreadyFeatured_throwsException() {
        FeaturedProductCreateRequest request = new FeaturedProductCreateRequest(100L);
        when(productRepository.findByIdAndIsDeletedFalse(100L)).thenReturn(Optional.of(product));
        when(featuredProductRepository.existsByProductIdAndIsDeletedFalse(100L)).thenReturn(true);

        assertThatThrownBy(() -> featuredProductService.addFeaturedProduct(request, 1L))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("đã tồn tại trong danh sách nổi bật");
    }

    @Test
    @DisplayName("updateFeaturedProduct - Cập nhật sản phẩm nổi bật thành công")
    void updateFeaturedProduct_success() {
        FeaturedProductUpdateRequest request = new FeaturedProductUpdateRequest(5, FeaturedProductStatus.INACTIVE);
        when(featuredProductRepository.findByIdAndIsDeletedFalse(1L)).thenReturn(Optional.of(featuredProduct));
        when(featuredProductRepository.save(any(FeaturedProduct.class))).thenReturn(featuredProduct);

        FeaturedProductResponse resp = featuredProductService.updateFeaturedProduct(1L, request, 1L);

        assertThat(resp.displayOrder()).isEqualTo(5);
        assertThat(resp.status()).isEqualTo(FeaturedProductStatus.INACTIVE);

        verify(systemLogService).log(eq(1L), eq("FEATURED_PRODUCT_UPDATED"), eq("FEATURED_PRODUCT"), eq(1L), any());
    }

    @Test
    @DisplayName("updateFeaturedProduct - Không tìm thấy bản ghi ném ResourceNotFoundException")
    void updateFeaturedProduct_notFound_throwsException() {
        FeaturedProductUpdateRequest request = new FeaturedProductUpdateRequest(5, FeaturedProductStatus.ACTIVE);
        when(featuredProductRepository.findByIdAndIsDeletedFalse(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> featuredProductService.updateFeaturedProduct(999L, request, 1L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Không tìm thấy sản phẩm nổi bật");
    }

    @Test
    @DisplayName("deleteFeaturedProduct - Xóa mềm sản phẩm nổi bật thành công")
    void deleteFeaturedProduct_success() {
        when(featuredProductRepository.findByIdAndIsDeletedFalse(1L)).thenReturn(Optional.of(featuredProduct));

        featuredProductService.deleteFeaturedProduct(1L, 1L);

        assertThat(featuredProduct.getIsDeleted()).isTrue();
        verify(featuredProductRepository).save(featuredProduct);
        verify(systemLogService).log(eq(1L), eq("FEATURED_PRODUCT_DELETED"), eq("FEATURED_PRODUCT"), eq(1L), any());
    }

    @Test
    @DisplayName("deleteFeaturedProduct - Không tìm thấy bản ghi ném ResourceNotFoundException")
    void deleteFeaturedProduct_notFound_throwsException() {
        when(featuredProductRepository.findByIdAndIsDeletedFalse(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> featuredProductService.deleteFeaturedProduct(999L, 1L))
                .isInstanceOf(ResourceNotFoundException.class);
    }
}
