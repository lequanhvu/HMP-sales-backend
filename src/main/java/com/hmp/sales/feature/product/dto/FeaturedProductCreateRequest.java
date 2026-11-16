package com.hmp.sales.feature.product.dto;

import com.hmp.sales.feature.product.entity.FeaturedProductStatus;
import jakarta.validation.constraints.NotNull;

/**
 * Payload yêu cầu thêm một sản phẩm vào danh sách nổi bật.
 *
 * @param productId    ID sản phẩm cần đánh dấu nổi bật (bắt buộc)
 * @param displayOrder thứ tự hiển thị ưu tiên (tùy chọn, mặc định 0)
 * @param status       trạng thái hiển thị nổi bật (tùy chọn, mặc định ACTIVE)
 */
public record FeaturedProductCreateRequest(
        @NotNull(message = "ID sản phẩm không được để trống.")
        Long productId,

        Integer displayOrder,

        FeaturedProductStatus status
) {

    /**
     * Constructor thuận tiện tạo request chỉ với ID sản phẩm.
     *
     * @param productId ID sản phẩm
     */
    public FeaturedProductCreateRequest(Long productId) {
        this(productId, 0, FeaturedProductStatus.ACTIVE);
    }
}
