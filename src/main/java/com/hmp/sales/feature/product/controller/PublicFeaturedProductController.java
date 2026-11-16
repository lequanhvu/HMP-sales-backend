package com.hmp.sales.feature.product.controller;

import com.hmp.sales.common.response.ApiSuccessResponse;
import com.hmp.sales.feature.product.dto.FeaturedProductResponse;
import com.hmp.sales.feature.product.service.FeaturedProductService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * REST Controller công khai phục vụ tra cứu danh sách sản phẩm nổi bật (Featured Products).
 * <p>
 * Khách vãng lai và người dùng có thể xem danh sách sản phẩm nổi bật trên trang chủ
 * mà không cần đăng nhập.
 */
@RestController
@RequestMapping("/api/v1/featured-products")
public class PublicFeaturedProductController {

    private final FeaturedProductService featuredProductService;

    /**
     * Khởi tạo PublicFeaturedProductController với FeaturedProductService.
     *
     * @param featuredProductService service nghiệp vụ sản phẩm nổi bật
     */
    public PublicFeaturedProductController(FeaturedProductService featuredProductService) {
        this.featuredProductService = featuredProductService;
    }

    /**
     * Lấy danh sách sản phẩm nổi bật đang kích hoạt (ACTIVE) cho trang chủ và các mục đề xuất.
     * <p>
     * Danh sách được sắp xếp theo thứ tự hiển thị ưu tiên ({@code displayOrder ASC}), sau đó theo thời gian tạo giảm dần.
     * Cờ {@code priceVisible} được kiểm soát động theo quyền {@code PRODUCT_PRICE_VIEW}.
     *
     * @return danh sách sản phẩm nổi bật
     */
    @GetMapping
    public ResponseEntity<ApiSuccessResponse<List<FeaturedProductResponse>>> getFeaturedProducts() {
        List<FeaturedProductResponse> result = featuredProductService.getPublicFeaturedProducts();
        return ResponseEntity.ok(ApiSuccessResponse.success("Lấy danh sách sản phẩm nổi bật thành công.", result));
    }
}
