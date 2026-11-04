package com.hmp.sales.feature.order.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;

/**
 * Request DTO chứa thông tin đơn giá và chiết khấu tùy chỉnh cho một dòng sản phẩm trong đơn hàng.
 *
 * @param orderItemId    ID dòng sản phẩm (bắt buộc)
 * @param unitPrice      đơn giá tùy chỉnh (VND, số nguyên dương; nếu null sẽ tự tra cứu từ ProductPrice)
 * @param discountAmount số tiền giảm giá cho dòng sản phẩm (VND, số nguyên không âm; mặc định 0)
 */
public record OrderItemPriceRequest(
        @NotNull(message = "ID dòng sản phẩm không được để trống")
        @Positive(message = "ID dòng sản phẩm phải là số nguyên dương")
        Long orderItemId,

        @Positive(message = "Đơn giá phải là số nguyên dương")
        Integer unitPrice,

        @PositiveOrZero(message = "Số tiền giảm giá dòng sản phẩm không được âm")
        Long discountAmount
) {
}

