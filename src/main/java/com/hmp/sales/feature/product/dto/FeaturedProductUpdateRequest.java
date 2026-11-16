package com.hmp.sales.feature.product.dto;

import com.hmp.sales.feature.product.entity.FeaturedProductStatus;

/**
 * Payload yêu cầu cập nhật thông tin hiển thị sản phẩm nổi bật.
 *
 * @param displayOrder thứ tự hiển thị ưu tiên (tùy chọn)
 * @param status       trạng thái hiển thị nổi bật (tùy chọn: ACTIVE hoặc INACTIVE)
 */
public record FeaturedProductUpdateRequest(
        Integer displayOrder,

        FeaturedProductStatus status
) {
}
