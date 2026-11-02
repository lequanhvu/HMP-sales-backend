package com.hmp.sales.feature.order.dto;

import jakarta.validation.constraints.Size;

/**
 * Record chứa thông tin yêu cầu hủy đơn hàng.
 *
 * @param reason lý do hủy đơn hàng (tùy chọn)
 */
public record CancelOrderRequest(
        @Size(max = 500, message = "Lý do hủy đơn không được vượt quá 500 ký tự")
        String reason
) {}

