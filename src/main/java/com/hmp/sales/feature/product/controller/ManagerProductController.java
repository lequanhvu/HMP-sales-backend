package com.hmp.sales.feature.product.controller;

import com.hmp.sales.common.response.ApiSuccessResponse;
import com.hmp.sales.common.util.SecurityUtils;
import com.hmp.sales.feature.product.dto.ProductCreateRequest;
import com.hmp.sales.feature.product.dto.ProductCreateUploadRequest;
import com.hmp.sales.feature.product.dto.ProductDetailResponse;
import com.hmp.sales.feature.product.dto.ProductImageBatchUploadRequest;
import com.hmp.sales.feature.product.dto.ProductImageResponse;
import com.hmp.sales.feature.product.dto.ProductImageUploadMultipartRequest;
import com.hmp.sales.feature.product.dto.ProductImageUploadRequest;
import com.hmp.sales.feature.product.dto.ProductUpdateRequest;
import com.hmp.sales.feature.product.dto.ProductVariantRequest;
import com.hmp.sales.feature.product.dto.ProductVariantResponse;
import com.hmp.sales.feature.product.service.ProductService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * REST Controller dành cho Quản lý (Manager) điều hành sản phẩm tôn sóng.
 * <p>
 * Cung cấp các thao tác:
 * <ul>
 *   <li>Tạo sản phẩm trực tiếp dưới một danh mục theo mô hình file-system (hỗ trợ JSON và multipart kèm tải ảnh lên Cloudflare R2).</li>
 *   <li>Cập nhật thông tin cơ bản sản phẩm, khóa/mở khóa và xóa mềm.</li>
 *   <li>Quản lý danh sách hình ảnh (tối đa 1 ảnh chính và 10 ảnh phụ).</li>
 *   <li>Quản lý danh sách biến thể sản phẩm, màu sắc và bảng giá theo đơn vị tính.</li>
 * </ul>
 */
@RestController
@RequestMapping("/api/v1/manager")
public class ManagerProductController {

    private final ProductService productService;

    /**
     * Khởi tạo ManagerProductController với ProductService.
     *
     * @param productService service quản lý sản phẩm
     */
    public ManagerProductController(ProductService productService) {
        this.productService = productService;
    }

    /**
     * Tạo mới sản phẩm trong một thư mục/danh mục cụ thể bằng JSON payload.
     *
     * @param categoryId ID danh mục chứa sản phẩm
     * @param request    payload thông tin sản phẩm và các biến thể ban đầu
     * @param jwt        JWT xác thực của Manager
     * @return chi tiết sản phẩm vừa tạo
     */
    @PostMapping(value = "/categories/{categoryId}/products", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ApiSuccessResponse<ProductDetailResponse>> createProduct(
            @PathVariable Long categoryId,
            @Valid @RequestBody ProductCreateRequest request,
            @AuthenticationPrincipal Jwt jwt) {
        Long managerId = SecurityUtils.extractUserId(jwt);
        ProductDetailResponse response = productService.createProduct(categoryId, request, managerId);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiSuccessResponse.created("Tạo sản phẩm thành công.", response));
    }

    /**
     * Tạo mới sản phẩm trong danh mục kèm tệp ảnh đại diện và ảnh phụ qua DTO request (1 bước duy nhất).
     *
     * @param categoryId ID danh mục chứa sản phẩm
     * @param request    DTO chứa thông tin sản phẩm và các tệp ảnh tải lên
     * @param jwt        JWT xác thực của Manager
     * @return chi tiết sản phẩm vừa tạo
     */
    @PostMapping(value = "/categories/{categoryId}/products", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiSuccessResponse<ProductDetailResponse>> createProductWithFiles(
            @PathVariable Long categoryId,
            @Valid @ModelAttribute ProductCreateUploadRequest request,
            @AuthenticationPrincipal Jwt jwt) {
        Long managerId = SecurityUtils.extractUserId(jwt);
        ProductDetailResponse response = productService.createProduct(categoryId, request, managerId);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiSuccessResponse.created("Tạo sản phẩm thành công.", response));
    }

    /**
     * Cập nhật thông tin cơ bản của sản phẩm.
     *
     * @param productId ID sản phẩm cần cập nhật
     * @param request   payload cập nhật
     * @return chi tiết sản phẩm sau khi cập nhật
     */
    @PatchMapping("/products/{productId}")
    public ResponseEntity<ApiSuccessResponse<ProductDetailResponse>> updateProduct(
            @PathVariable Long productId,
            @Valid @RequestBody ProductUpdateRequest request) {
        ProductDetailResponse response = productService.updateProduct(productId, request);
        return ResponseEntity.ok(ApiSuccessResponse.success("Cập nhật sản phẩm thành công.", response));
    }

    /**
     * Khóa sản phẩm (chuyển sang trạng thái INACTIVE, không cho đặt hàng mới).
     *
     * @param productId ID sản phẩm cần khóa
     * @return chi tiết sản phẩm sau khi khóa
     */
    @PostMapping("/products/{productId}/lock")
    public ResponseEntity<ApiSuccessResponse<ProductDetailResponse>> lockProduct(
            @PathVariable Long productId) {
        ProductDetailResponse response = productService.lockProduct(productId);
        return ResponseEntity.ok(ApiSuccessResponse.success("Khóa sản phẩm thành công.", response));
    }

    /**
     * Mở khóa sản phẩm (chuyển về trạng thái ACTIVE).
     *
     * @param productId ID sản phẩm cần mở khóa
     * @return chi tiết sản phẩm sau khi mở khóa
     */
    @PostMapping("/products/{productId}/unlock")
    public ResponseEntity<ApiSuccessResponse<ProductDetailResponse>> unlockProduct(
            @PathVariable Long productId) {
        ProductDetailResponse response = productService.unlockProduct(productId);
        return ResponseEntity.ok(ApiSuccessResponse.success("Mở khóa sản phẩm thành công.", response));
    }

    /**
     * Xóa mềm sản phẩm ({@code isDeleted = true}).
     * <p>
     * Cho phép xóa kể cả khi sản phẩm đã có trong các đơn hàng lịch sử (COMPLETED, CANCELLED).
     * Chặn xóa (409 Conflict) khi sản phẩm đang nằm trong đơn hàng chưa hoàn thành (PENDING, CONFIRMED, PROCESSING, SHIPPING).
     *
     * @param productId ID sản phẩm cần xóa
     * @return thông báo xóa thành công
     */
    @DeleteMapping("/products/{productId}")
    public ResponseEntity<ApiSuccessResponse<Void>> deleteProduct(@PathVariable Long productId) {
        productService.deleteProduct(productId);
        return ResponseEntity.ok(ApiSuccessResponse.success("Xóa sản phẩm thành công.", null));
    }

    /**
     * Thêm ảnh mới cho sản phẩm bằng tệp tải lên trực tiếp qua DTO request (1 bước duy nhất).
     *
     * @param productId ID sản phẩm cần thêm ảnh
     * @param request   DTO chứa thông tin ảnh tải lên và các cấu hình hiển thị
     * @return thông tin ảnh vừa tạo
     */
    @PostMapping(value = "/products/{productId}/images", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiSuccessResponse<ProductImageResponse>> uploadProductImageWithFile(
            @PathVariable Long productId,
            @Valid @ModelAttribute ProductImageUploadMultipartRequest request) {
        ProductImageResponse response = productService.uploadProductImage(productId, request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiSuccessResponse.created("Thêm ảnh sản phẩm thành công.", response));
    }

    /**
     * Thêm ảnh mới cho sản phẩm bằng JSON payload chứa URL ảnh sẵn có.
     * <p>
     * Ràng buộc: Mỗi sản phẩm chỉ có tối đa 1 ảnh chính và 10 ảnh phụ (tối đa 11 ảnh).
     *
     * @param productId ID sản phẩm cần thêm ảnh
     * @param request   payload ảnh
     * @return thông tin ảnh vừa tạo
     */
    @PostMapping(value = "/products/{productId}/images", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ApiSuccessResponse<ProductImageResponse>> uploadProductImage(
            @PathVariable Long productId,
            @Valid @RequestBody ProductImageUploadRequest request) {
        ProductImageResponse response = productService.uploadProductImage(productId, request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiSuccessResponse.created("Thêm ảnh sản phẩm thành công.", response));
    }

    /**
     * Thêm hàng loạt ảnh cho sản phẩm bằng danh sách tệp tải lên trực tiếp qua DTO request.
     *
     * @param productId ID sản phẩm cần thêm ảnh
     * @param request   DTO chứa danh sách tệp ảnh tải lên và các cấu hình hiển thị
     * @return danh sách chi tiết các ảnh vừa tạo
     */
    @PostMapping(value = "/products/{productId}/images/batch", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiSuccessResponse<List<ProductImageResponse>>> uploadProductImagesBatchWithFiles(
            @PathVariable Long productId,
            @Valid @ModelAttribute ProductImageBatchUploadRequest request) {
        List<ProductImageResponse> response = productService.uploadProductImages(productId, request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiSuccessResponse.created("Tải lên danh sách ảnh sản phẩm thành công.", response));
    }

    /**
     * Thêm hàng loạt ảnh cho sản phẩm bằng JSON payload (hỗ trợ upload nhiều ảnh phụ cùng một lúc).
     *
     * @param productId ID sản phẩm cần thêm ảnh
     * @param requests  danh sách thông tin ảnh cần tải lên (tối đa theo cấu hình app.product.max-secondary-images)
     * @return danh sách chi tiết các ảnh vừa tạo
     */
    @PostMapping(value = "/products/{productId}/images/batch", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ApiSuccessResponse<List<ProductImageResponse>>> uploadProductImagesBatch(
            @PathVariable Long productId,
            @Valid @RequestBody List<ProductImageUploadRequest> requests) {
        List<ProductImageResponse> response = productService.uploadProductImages(productId, requests);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiSuccessResponse.created("Tải lên danh sách ảnh sản phẩm thành công.", response));
    }

    /**
     * Xóa mềm một ảnh của sản phẩm.
     *
     * @param productId ID sản phẩm
     * @param imageId   ID ảnh cần xóa
     * @return thông báo xóa ảnh thành công
     */
    @DeleteMapping("/products/{productId}/images/{imageId}")
    public ResponseEntity<ApiSuccessResponse<Void>> deleteProductImage(
            @PathVariable Long productId,
            @PathVariable Long imageId) {
        productService.deleteProductImage(productId, imageId);
        return ResponseEntity.ok(ApiSuccessResponse.success("Xóa ảnh sản phẩm thành công.", null));
    }

    /**
     * Xóa mềm hàng loạt ảnh của sản phẩm theo danh sách ID ảnh.
     *
     * @param productId ID sản phẩm
     * @param imageIds  danh sách ID ảnh cần xóa mềm (truyền qua request body hoặc query param 'ids')
     * @param idsParam  danh sách ID ảnh truyền qua query param
     * @return thông báo xóa thành công
     */
    @DeleteMapping("/products/{productId}/images/batch")
    public ResponseEntity<ApiSuccessResponse<Void>> deleteProductImagesBatch(
            @PathVariable Long productId,
            @RequestBody(required = false) List<Long> imageIds,
            @RequestParam(name = "ids", required = false) List<Long> idsParam) {
        List<Long> targets = (imageIds != null && !imageIds.isEmpty()) ? imageIds : idsParam;
        productService.deleteProductImages(productId, targets);
        return ResponseEntity.ok(ApiSuccessResponse.success("Xóa danh sách ảnh sản phẩm thành công.", null));
    }

    /**
     * Thêm biến thể mới cho sản phẩm (kèm danh sách màu và bảng giá theo đơn vị tính).
     *
     * @param productId ID sản phẩm
     * @param request   payload thông tin biến thể
     * @return chi tiết biến thể vừa tạo
     */
    @PostMapping("/products/{productId}/variants")
    public ResponseEntity<ApiSuccessResponse<ProductVariantResponse>> addVariant(
            @PathVariable Long productId,
            @Valid @RequestBody ProductVariantRequest request) {
        ProductVariantResponse response = productService.addVariant(productId, request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiSuccessResponse.created("Thêm biến thể thành công.", response));
    }

    /**
     * Cập nhật thông tin biến thể sản phẩm.
     *
     * @param productId ID sản phẩm
     * @param variantId ID biến thể cần cập nhật
     * @param request   payload cập nhật
     * @return chi tiết biến thể sau khi cập nhật
     */
    @PatchMapping("/products/{productId}/variants/{variantId}")
    public ResponseEntity<ApiSuccessResponse<ProductVariantResponse>> updateVariant(
            @PathVariable Long productId,
            @PathVariable Long variantId,
            @Valid @RequestBody ProductVariantRequest request) {
        ProductVariantResponse response = productService.updateVariant(productId, variantId, request);
        return ResponseEntity.ok(ApiSuccessResponse.success("Cập nhật biến thể thành công.", response));
    }

    /**
     * Khóa biến thể sản phẩm (chuyển sang LOCKED).
     *
     * @param productId ID sản phẩm
     * @param variantId ID biến thể cần khóa
     * @return chi tiết biến thể sau khi khóa
     */
    @PostMapping("/products/{productId}/variants/{variantId}/lock")
    public ResponseEntity<ApiSuccessResponse<ProductVariantResponse>> lockVariant(
            @PathVariable Long productId,
            @PathVariable Long variantId) {
        ProductVariantResponse response = productService.lockVariant(productId, variantId);
        return ResponseEntity.ok(ApiSuccessResponse.success("Khóa biến thể thành công.", response));
    }

    /**
     * Mở khóa biến thể sản phẩm (chuyển về ACTIVE).
     *
     * @param productId ID sản phẩm
     * @param variantId ID biến thể cần mở khóa
     * @return chi tiết biến thể sau khi mở khóa
     */
    @PostMapping("/products/{productId}/variants/{variantId}/unlock")
    public ResponseEntity<ApiSuccessResponse<ProductVariantResponse>> unlockVariant(
            @PathVariable Long productId,
            @PathVariable Long variantId) {
        ProductVariantResponse response = productService.unlockVariant(productId, variantId);
        return ResponseEntity.ok(ApiSuccessResponse.success("Mở khóa biến thể thành công.", response));
    }

    /**
     * Xóa mềm biến thể sản phẩm (cascade xóa màu sắc và bảng giá liên kết).
     *
     * @param productId ID sản phẩm
     * @param variantId ID biến thể cần xóa
     * @return thông báo xóa thành công
     */
    @DeleteMapping("/products/{productId}/variants/{variantId}")
    public ResponseEntity<ApiSuccessResponse<Void>> deleteVariant(
            @PathVariable Long productId,
            @PathVariable Long variantId) {
        productService.deleteVariant(productId, variantId);
        return ResponseEntity.ok(ApiSuccessResponse.success("Xóa biến thể thành công.", null));
    }
}
