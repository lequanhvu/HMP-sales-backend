package com.hmp.sales.feature.order.dto;

import com.hmp.sales.feature.order.entity.OrderStatus;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * Request DTO phục vụ cập nhật trạng thái đơn hàng của Quản lý.
 *
 * @param status trạng thái mới cần cập nhật (bắt buộc)
 * @param reason lý do chuyển trạng thái (tùy chọn)
 */
public record UpdateOrderStatusRequest(
        @NotNull(message = "Trạng thái đơn hàng không được để trống")
        OrderStatus status,

        @Size(max = 500, message = "Lý do không được vượt quá 500 ký tự")
        String reason
) {
}

