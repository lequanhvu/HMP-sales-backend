package com.hmp.sales.feature.product.controller;

import com.hmp.sales.common.response.ApiSuccessResponse;
import com.hmp.sales.common.util.SecurityUtils;
import com.hmp.sales.feature.product.dto.FeaturedProductCreateRequest;
import com.hmp.sales.feature.product.dto.FeaturedProductResponse;
import com.hmp.sales.feature.product.dto.FeaturedProductUpdateRequest;
import com.hmp.sales.feature.product.service.FeaturedProductService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * REST Controller dành cho Quản lý (Manager/Admin) điều hành danh sách sản phẩm nổi bật.
 * <p>
 * Cung cấp đầy đủ các thao tác CRUD:
 * <ul>
 *   <li>Xem danh sách toàn bộ sản phẩm nổi bật (bao gồm ACTIVE và INACTIVE).</li>
 *   <li>Xem chi tiết cấu hình sản phẩm nổi bật theo ID.</li>
 *   <li>Thêm sản phẩm vào danh sách nổi bật (gán thứ tự hiển thị, ghi chú).</li>
 *   <li>Cập nhật thứ tự hiển thị, ghi chú hoặc trạng thái bật/tắt hiển thị.</li>
 *   <li>Xóa mềm sản phẩm khỏi danh sách nổi bật.</li>
 * </ul>
 */
@RestController
@RequestMapping("/api/v1/manager/featured-products")
public class ManagerFeaturedProductController {

    private final FeaturedProductService featuredProductService;

    /**
     * Khởi tạo ManagerFeaturedProductController với FeaturedProductService.
     *
     * @param featuredProductService service nghiệp vụ sản phẩm nổi bật
     */
    public ManagerFeaturedProductController(FeaturedProductService featuredProductService) {
        this.featuredProductService = featuredProductService;
    }

    /**
     * Lấy toàn bộ danh sách sản phẩm nổi bật trong hệ thống (phục vụ quản trị).
     *
     * @return danh sách tất cả sản phẩm nổi bật
     */
    @GetMapping
    public ResponseEntity<ApiSuccessResponse<List<FeaturedProductResponse>>> getAllFeaturedProducts() {
        List<FeaturedProductResponse> list = featuredProductService.getAllFeaturedProducts();
        return ResponseEntity.ok(ApiSuccessResponse.success("Lấy danh sách sản phẩm nổi bật quản lý thành công.", list));
    }

    /**
     * Lấy chi tiết thông tin cấu hình sản phẩm nổi bật theo ID.
     *
     * @param id ID bản ghi sản phẩm nổi bật (featured_id)
     * @return chi tiết sản phẩm nổi bật
     */
    @GetMapping("/{id}")
    public ResponseEntity<ApiSuccessResponse<FeaturedProductResponse>> getFeaturedProductById(@PathVariable Long id) {
        FeaturedProductResponse response = featuredProductService.getFeaturedProductById(id);
        return ResponseEntity.ok(ApiSuccessResponse.success("Lấy thông tin sản phẩm nổi bật thành công.", response));
    }

    /**
     * Thêm sản phẩm vào danh sách sản phẩm nổi bật.
     *
     * @param request dữ liệu yêu cầu thêm sản phẩm nổi bật
     * @param jwt     JWT xác thực của người dùng đăng nhập
     * @return chi tiết sản phẩm nổi bật vừa tạo
     */
    @PostMapping
    public ResponseEntity<ApiSuccessResponse<FeaturedProductResponse>> addFeaturedProduct(
            @Valid @RequestBody FeaturedProductCreateRequest request,
            @AuthenticationPrincipal Jwt jwt) {
        Long actorId = jwt != null ? SecurityUtils.extractUserId(jwt) : SecurityUtils.extractCurrentUserId();
        FeaturedProductResponse response = featuredProductService.addFeaturedProduct(request, actorId);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiSuccessResponse.created("Thêm sản phẩm nổi bật thành công.", response));
    }

    /**
     * Cập nhật thông số hiển thị của sản phẩm nổi bật (thứ tự hiển thị, ghi chú, trạng thái).
     *
     * @param id      ID bản ghi sản phẩm nổi bật cần cập nhật
     * @param request dữ liệu cập nhật
     * @param jwt     JWT xác thực của người dùng đăng nhập
     * @return chi tiết sản phẩm nổi bật sau cập nhật
     */
    @PutMapping("/{id}")
    public ResponseEntity<ApiSuccessResponse<FeaturedProductResponse>> updateFeaturedProduct(
            @PathVariable Long id,
            @Valid @RequestBody FeaturedProductUpdateRequest request,
            @AuthenticationPrincipal Jwt jwt) {
        Long actorId = jwt != null ? SecurityUtils.extractUserId(jwt) : SecurityUtils.extractCurrentUserId();
        FeaturedProductResponse response = featuredProductService.updateFeaturedProduct(id, request, actorId);
        return ResponseEntity.ok(ApiSuccessResponse.success("Cập nhật sản phẩm nổi bật thành công.", response));
    }

    /**
     * Xóa mềm một bản ghi khỏi danh sách sản phẩm nổi bật.
     *
     * @param id  ID bản ghi sản phẩm nổi bật cần xóa
     * @param jwt JWT xác thực của người dùng đăng nhập
     * @return phản hồi thành công
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<ApiSuccessResponse<Void>> deleteFeaturedProduct(
            @PathVariable Long id,
            @AuthenticationPrincipal Jwt jwt) {
        Long actorId = jwt != null ? SecurityUtils.extractUserId(jwt) : SecurityUtils.extractCurrentUserId();
        featuredProductService.deleteFeaturedProduct(id, actorId);
        return ResponseEntity.ok(ApiSuccessResponse.success("Xóa sản phẩm nổi bật thành công.", null));
    }
}
