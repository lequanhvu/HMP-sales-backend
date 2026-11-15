package com.hmp.sales.feature.product.service;

import com.hmp.sales.common.exception.BusinessRuleException;
import com.hmp.sales.common.exception.ResourceNotFoundException;
import com.hmp.sales.feature.product.dto.ProductCreateRequest;
import com.hmp.sales.feature.product.dto.ProductCreateUploadRequest;
import com.hmp.sales.feature.product.dto.ProductDetailResponse;
import com.hmp.sales.feature.product.dto.ProductFilterParams;
import com.hmp.sales.feature.product.dto.ProductImageBatchUploadRequest;
import com.hmp.sales.feature.product.dto.ProductImageResponse;
import com.hmp.sales.feature.product.dto.ProductImageUploadMultipartRequest;
import com.hmp.sales.feature.product.dto.ProductImageUploadRequest;
import com.hmp.sales.feature.product.dto.ProductSummaryResponse;
import com.hmp.sales.feature.product.dto.ProductUpdateRequest;
import com.hmp.sales.feature.product.dto.ProductVariantRequest;
import com.hmp.sales.feature.product.dto.ProductVariantResponse;
import org.springframework.data.domain.Page;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/**
 * Giao diện nghiệp vụ quản lý sản phẩm tôn sóng và biến thể sản phẩm (Product).
 * <p>
 * Bao gồm:
 * <ul>
 *   <li>Tìm kiếm/lọc sản phẩm công khai với kiểm soát hiển thị giá ({@code PRODUCT_PRICE_VIEW}).</li>
 *   <li>CRUD sản phẩm, quản lý ảnh (trực tiếp qua Cloudflare R2 hoặc URL) và biến thể bởi Manager.</li>
 *   <li>Khóa/mở khóa sản phẩm.</li>
 *   <li>Xóa mềm với kiểm tra đơn hàng dở dang.</li>
 * </ul>
 */
public interface ProductService {

    /**
     * Tìm kiếm và lọc sản phẩm với phân trang. Giá sản phẩm chỉ hiển thị khi caller có quyền
     * {@code PRODUCT_PRICE_VIEW}. Không cho phép {@code sortBy=price} khi thiếu quyền.
     *
     * @param params tham số lọc, sắp xếp và phân trang
     * @return trang sản phẩm tóm tắt
     * @throws BusinessRuleException nếu cố sắp xếp theo giá mà thiếu quyền
     */
    Page<ProductSummaryResponse> searchProducts(ProductFilterParams params);

    /**
     * Lấy thông tin chi tiết sản phẩm theo ID, bao gồm ảnh và biến thể.
     * Giá được che giấu nếu caller thiếu quyền {@code PRODUCT_PRICE_VIEW}.
     *
     * @param productId ID sản phẩm cần lấy
     * @return DTO chi tiết sản phẩm
     * @throws ResourceNotFoundException nếu không tìm thấy
     */
    ProductDetailResponse getProductDetail(Long productId);

    /**
     * Tạo mới sản phẩm trong một danh mục, bao gồm khởi tạo biến thể ban đầu và ảnh có sẵn.
     * Thực hiện trong một transaction atomic.
     *
     * @param categoryId ID danh mục chứa sản phẩm
     * @param request    payload tạo sản phẩm và các biến thể
     * @param managerId  ID Manager tạo sản phẩm
     * @return DTO chi tiết sản phẩm vừa tạo
     * @throws ResourceNotFoundException nếu danh mục hoặc thương hiệu không tồn tại
     */
    ProductDetailResponse createProduct(Long categoryId, ProductCreateRequest request, Long managerId);

    /**
     * Tạo mới sản phẩm trong danh mục kèm tệp ảnh đại diện và danh sách ảnh phụ (1 bước duy nhất).
     * <p>
     * Nếu truyền tệp {@code primaryImage} hoặc {@code secondaryImages}, hệ thống sẽ tự động tải tệp lên
     * Cloudflare R2 trong thư mục {@code "products"} và lưu URL công khai vào cơ sở dữ liệu.
     *
     * @param categoryId      ID danh mục chứa sản phẩm
     * @param request         payload thông tin sản phẩm và các biến thể
     * @param primaryImage    tệp ảnh đại diện tải lên trực tiếp (tùy chọn)
     * @param secondaryImages danh sách tệp ảnh phụ tải lên trực tiếp (tùy chọn)
     * @param managerId       ID Manager tạo sản phẩm
     * @return DTO chi tiết sản phẩm vừa tạo kèm toàn bộ ảnh đã lưu
     * @throws ResourceNotFoundException nếu danh mục hoặc thương hiệu không tồn tại
     * @throws BusinessRuleException    nếu vượt quá giới hạn ảnh hoặc tệp không hợp lệ
     */
    ProductDetailResponse createProduct(Long categoryId, ProductCreateRequest request,
                                        MultipartFile primaryImage, List<MultipartFile> secondaryImages,
                                        Long managerId);

    /**
     * Tạo mới sản phẩm trong danh mục kèm tệp ảnh qua DTO request (1 bước duy nhất).
     *
     * @param categoryId ID danh mục chứa sản phẩm
     * @param request    DTO chứa thông tin sản phẩm và các tệp ảnh tải lên
     * @param managerId  ID Manager tạo sản phẩm
     * @return DTO chi tiết sản phẩm vừa tạo kèm toàn bộ ảnh đã lưu
     * @throws ResourceNotFoundException nếu danh mục hoặc thương hiệu không tồn tại
     * @throws BusinessRuleException    nếu vượt quá giới hạn ảnh hoặc tệp không hợp lệ
     */
    ProductDetailResponse createProduct(Long categoryId, ProductCreateUploadRequest request, Long managerId);

    /**
     * Cập nhật thông tin cơ bản của sản phẩm (tên, mô tả, thương hiệu, danh mục).
     *
     * @param productId ID sản phẩm cần cập nhật
     * @param request   payload chứa thông tin cập nhật
     * @return DTO chi tiết sản phẩm sau khi cập nhật
     * @throws ResourceNotFoundException nếu không tìm thấy
     */
    ProductDetailResponse updateProduct(Long productId, ProductUpdateRequest request);

    /**
     * Khóa sản phẩm (chuyển sang trạng thái INACTIVE, không cho đặt hàng mới).
     *
     * @param productId ID sản phẩm cần khóa
     * @return DTO chi tiết sản phẩm sau khi khóa
     * @throws ResourceNotFoundException nếu không tìm thấy
     */
    ProductDetailResponse lockProduct(Long productId);

    /**
     * Mở khóa sản phẩm (chuyển về trạng thái ACTIVE).
     *
     * @param productId ID sản phẩm cần mở khóa
     * @return DTO chi tiết sản phẩm sau khi mở khóa
     * @throws ResourceNotFoundException nếu không tìm thấy
     */
    ProductDetailResponse unlockProduct(Long productId);

    /**
     * Xóa mềm sản phẩm ({@code isDeleted = true}).
     * Cho phép xóa kể cả khi có đơn hàng lịch sử đã hoàn thành/hủy.
     * Chỉ chặn xóa (409 Conflict) khi sản phẩm đang trong đơn hàng chưa hoàn thành.
     *
     * @param productId ID sản phẩm cần xóa mềm
     * @throws ResourceNotFoundException nếu không tìm thấy
     * @throws BusinessRuleException    nếu có đơn hàng chưa hoàn thành
     */
    void deleteProduct(Long productId);

    /**
     * Thêm ảnh cho sản phẩm bằng URL có sẵn. Kiểm tra ràng buộc tối đa 1 ảnh chính và 10 ảnh phụ.
     *
     * @param productId ID sản phẩm cần thêm ảnh
     * @param request   payload URL ảnh và loại ảnh
     * @return DTO chi tiết ảnh vừa thêm
     * @throws ResourceNotFoundException nếu không tìm thấy sản phẩm
     * @throws BusinessRuleException    nếu vượt quá giới hạn ảnh
     */
    ProductImageResponse uploadProductImage(Long productId, ProductImageUploadRequest request);

    /**
     * Thêm ảnh mới cho sản phẩm từ tệp nhị phân hoặc URL công khai trực tiếp.
     * <p>
     * Nếu truyền {@code imageFile}, hệ thống sẽ tải tệp lên Cloudflare R2 trong thư mục {@code "products"}
     * và lưu URL công khai vào cơ sở dữ liệu. Nếu không truyền tệp, sẽ sử dụng {@code imageUrl}.
     *
     * @param productId    ID sản phẩm cần thêm ảnh
     * @param imageFile    tệp ảnh tải lên từ máy tính (tùy chọn)
     * @param imageUrl     đường dẫn URL ảnh trực tiếp (tùy chọn, dùng nếu không truyền tệp)
     * @param isPrimary    true nếu đây là ảnh đại diện (mặc định false)
     * @param displayOrder thứ tự hiển thị (tùy chọn)
     * @return DTO chi tiết ảnh vừa thêm
     * @throws ResourceNotFoundException nếu không tìm thấy sản phẩm
     * @throws BusinessRuleException    nếu vượt quá giới hạn ảnh hoặc tệp không hợp lệ
     */
    ProductImageResponse uploadProductImage(Long productId, MultipartFile imageFile, String imageUrl,
                                            Boolean isPrimary, Integer displayOrder);

    /**
     * Thêm ảnh mới cho sản phẩm qua DTO request (1 bước duy nhất).
     *
     * @param productId ID sản phẩm cần thêm ảnh
     * @param request   DTO chứa tệp ảnh và cấu hình hiển thị
     * @return DTO chi tiết ảnh vừa thêm
     * @throws ResourceNotFoundException nếu không tìm thấy sản phẩm
     * @throws BusinessRuleException    nếu vượt quá giới hạn ảnh hoặc tệp không hợp lệ
     */
    ProductImageResponse uploadProductImage(Long productId, ProductImageUploadMultipartRequest request);

    /**
     * Thêm hàng loạt ảnh cho sản phẩm bằng danh sách URL (hỗ trợ upload nhiều ảnh phụ cùng một lúc).
     * Kiểm tra số lượng ảnh tải lên không vượt quá cấu hình tối đa và tổng số ảnh không vượt quá giới hạn.
     *
     * @param productId ID sản phẩm cần thêm ảnh
     * @param requests  danh sách thông tin ảnh cần tải lên
     * @return danh sách chi tiết các ảnh vừa được thêm
     * @throws ResourceNotFoundException nếu không tìm thấy sản phẩm
     * @throws BusinessRuleException    nếu danh sách rỗng hoặc vượt quá giới hạn ảnh
     */
    List<ProductImageResponse> uploadProductImages(Long productId, List<ProductImageUploadRequest> requests);

    /**
     * Thêm hàng loạt ảnh mới cho sản phẩm từ danh sách tệp nhị phân.
     * <p>
     * Toàn bộ tệp sẽ được tải lên Cloudflare R2 trong thư mục {@code "products"}.
     *
     * @param productId         ID sản phẩm cần thêm ảnh
     * @param imageFiles        danh sách tệp ảnh tải lên
     * @param isPrimary         true nếu các ảnh này là ảnh đại diện (thường là false khi tải hàng loạt)
     * @param startDisplayOrder thứ tự hiển thị bắt đầu (tùy chọn, tự động tính nối tiếp nếu null)
     * @return danh sách DTO chi tiết các ảnh vừa được thêm
     * @throws ResourceNotFoundException nếu không tìm thấy sản phẩm
     * @throws BusinessRuleException    nếu danh sách tệp rỗng hoặc vượt quá giới hạn ảnh
     */
    List<ProductImageResponse> uploadProductImages(Long productId, List<MultipartFile> imageFiles,
                                                   Boolean isPrimary, Integer startDisplayOrder);

    /**
     * Thêm hàng loạt ảnh mới cho sản phẩm từ DTO request tải lên danh sách tệp trực tiếp.
     *
     * @param productId ID sản phẩm cần thêm ảnh
     * @param request   DTO chứa danh sách tệp ảnh và cấu hình hiển thị
     * @return danh sách DTO chi tiết các ảnh vừa được thêm
     * @throws ResourceNotFoundException nếu không tìm thấy sản phẩm
     * @throws BusinessRuleException    nếu danh sách tệp rỗng hoặc vượt quá giới hạn ảnh
     */
    List<ProductImageResponse> uploadProductImages(Long productId, ProductImageBatchUploadRequest request);

    /**
     * Quá tải tiện ích phương thức tải lên nhiều ảnh với tên uploadProductImage.
     *
     * @param productId ID sản phẩm cần thêm ảnh
     * @param requests  danh sách thông tin ảnh cần tải lên
     * @return danh sách chi tiết các ảnh vừa được thêm
     */
    default List<ProductImageResponse> uploadProductImage(Long productId, List<ProductImageUploadRequest> requests) {
        return uploadProductImages(productId, requests);
    }

    /**
     * Xóa mềm ảnh sản phẩm theo ID.
     *
     * @param productId ID sản phẩm chứa ảnh
     * @param imageId   ID ảnh cần xóa
     * @throws ResourceNotFoundException nếu không tìm thấy sản phẩm hoặc ảnh
     */
    void deleteProductImage(Long productId, Long imageId);

    /**
     * Xóa mềm hàng loạt ảnh của sản phẩm theo danh sách ID ảnh.
     *
     * @param productId ID sản phẩm chứa ảnh
     * @param imageIds  danh sách ID ảnh cần xóa mềm
     * @throws ResourceNotFoundException nếu không tìm thấy sản phẩm hoặc ảnh
     * @throws BusinessRuleException    nếu danh sách ID rỗng
     */
    void deleteProductImages(Long productId, List<Long> imageIds);

    /**
     * Quá tải tiện ích phương thức xóa nhiều ảnh với tên deleteProductImage.
     *
     * @param productId ID sản phẩm chứa ảnh
     * @param imageIds  danh sách ID ảnh cần xóa
     */
    default void deleteProductImage(Long productId, List<Long> imageIds) {
        deleteProductImages(productId, imageIds);
    }

    /**
     * Thêm biến thể mới cho sản phẩm, bao gồm màu sắc và bảng giá theo đơn vị tính.
     *
     * @param productId ID sản phẩm cần thêm biến thể
     * @param request   payload thông tin biến thể
     * @return DTO chi tiết biến thể vừa tạo
     * @throws ResourceNotFoundException nếu không tìm thấy sản phẩm
     */
    ProductVariantResponse addVariant(Long productId, ProductVariantRequest request);

    /**
     * Cập nhật thông tin biến thể sản phẩm (tên, loại sóng, độ dày, màu, giá).
     *
     * @param productId ID sản phẩm chứa biến thể
     * @param variantId ID biến thể cần cập nhật
     * @param request   payload thông tin cập nhật
     * @return DTO chi tiết biến thể sau khi cập nhật
     * @throws ResourceNotFoundException nếu không tìm thấy
     */
    ProductVariantResponse updateVariant(Long productId, Long variantId, ProductVariantRequest request);

    /**
     * Khóa biến thể sản phẩm (chuyển sang LOCKED).
     *
     * @param productId ID sản phẩm
     * @param variantId ID biến thể
     * @return DTO biến thể sau khi khóa
     */
    ProductVariantResponse lockVariant(Long productId, Long variantId);

    /**
     * Mở khóa biến thể sản phẩm (chuyển sang ACTIVE).
     *
     * @param productId ID sản phẩm
     * @param variantId ID biến thể
     * @return DTO biến thể sau khi mở khóa
     */
    ProductVariantResponse unlockVariant(Long productId, Long variantId);

    /**
     * Xóa mềm biến thể sản phẩm kèm màu sắc và bảng giá liên quan.
     *
     * @param productId ID sản phẩm chứa biến thể
     * @param variantId ID biến thể cần xóa
     * @throws ResourceNotFoundException nếu không tìm thấy
     */
    void deleteVariant(Long productId, Long variantId);
}
