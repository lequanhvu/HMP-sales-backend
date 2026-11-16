package com.hmp.sales.feature.product.service;

import com.hmp.sales.feature.product.dto.FeaturedProductCreateRequest;
import com.hmp.sales.feature.product.dto.FeaturedProductResponse;
import com.hmp.sales.feature.product.dto.FeaturedProductUpdateRequest;

import java.util.List;

/**
 * Service định nghĩa các thao tác nghiệp vụ cho phân hệ Sản phẩm nổi bật (Featured Products).
 */
public interface FeaturedProductService {

    /**
     * Lấy danh sách sản phẩm nổi bật đang kích hoạt ({@code ACTIVE}) phục vụ trang chủ và khách hàng.
     * Sắp xếp theo thứ tự hiển thị tăng dần (displayOrder ASC), sau đó theo thời gian tạo giảm dần.
     *
     * @return danh sách DTO sản phẩm nổi bật công khai
     */
    List<FeaturedProductResponse> getPublicFeaturedProducts();

    /**
     * Lấy toàn bộ danh sách sản phẩm nổi bật (bao gồm cả ACTIVE và INACTIVE) phục vụ Quản lý.
     *
     * @return danh sách tất cả sản phẩm nổi bật
     */
    List<FeaturedProductResponse> getAllFeaturedProducts();

    /**
     * Lấy chi tiết thông tin một sản phẩm nổi bật theo ID.
     *
     * @param id ID bản ghi sản phẩm nổi bật (featured_id)
     * @return DTO thông tin sản phẩm nổi bật
     */
    FeaturedProductResponse getFeaturedProductById(Long id);

    /**
     * Thêm một sản phẩm vào danh sách sản phẩm nổi bật.
     *
     * @param request dữ liệu yêu cầu thêm sản phẩm nổi bật
     * @param actorId ID người dùng thực hiện thao tác
     * @return DTO sản phẩm nổi bật vừa tạo
     */
    FeaturedProductResponse addFeaturedProduct(FeaturedProductCreateRequest request, Long actorId);

    /**
     * Cập nhật thông số hiển thị của một sản phẩm nổi bật (thứ tự, ghi chú, trạng thái).
     *
     * @param id      ID bản ghi sản phẩm nổi bật
     * @param request dữ liệu cập nhật
     * @param actorId ID người dùng thực hiện thao tác
     * @return DTO sản phẩm nổi bật sau khi cập nhật
     */
    FeaturedProductResponse updateFeaturedProduct(Long id, FeaturedProductUpdateRequest request, Long actorId);

    /**
     * Xóa mềm một bản ghi khỏi danh sách sản phẩm nổi bật.
     *
     * @param id      ID bản ghi sản phẩm nổi bật
     * @param actorId ID người dùng thực hiện thao tác
     */
    void deleteFeaturedProduct(Long id, Long actorId);
}
