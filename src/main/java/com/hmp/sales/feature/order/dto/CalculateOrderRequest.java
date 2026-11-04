package com.hmp.sales.feature.order.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

import java.util.List;

/**
 * Request DTO phục vụ nghiệp vụ tính giá đơn hàng của Quản lý.
 * <p>
 * Cho phép tùy chọn truyền đơn giá (unitPrice) và giảm giá (discountAmount) cho từng dòng sản phẩm,
 * hoặc truyền giảm giá tổng cho toàn bộ đơn hàng (discountAmount), hoặc kết hợp cả hai.
 * Thao tác này chỉ tính toán và cập nhật số tiền, không làm thay đổi trạng thái đơn hàng.
 * </p>
 *
 * @param discountAmount số tiền giảm giá cho toàn bộ đơn hàng (VND, số nguyên không âm; tùy chọn)
 * @param items          danh sách tùy chỉnh đơn giá và giảm giá cho từng dòng sản phẩm (tùy chọn)
 * @param note           ghi chú thêm của Quản lý khi tính giá (tùy chọn)
 */
public record CalculateOrderRequest(
        @PositiveOrZero(message = "Số tiền giảm giá đơn hàng không được âm")
        Long discountAmount,

        List<@Valid OrderItemPriceRequest> items,

        @Size(max = 1000, message = "Ghi chú không được vượt quá 1000 ký tự")
        String note
) {
}

