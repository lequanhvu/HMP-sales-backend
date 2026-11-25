package com.hmp.sales.feature.cart.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

/**
 * Request DTO cập nhật số lượng hoặc quy cách sản phẩm trong giỏ hàng.
 *
 * @param quantity   số lượng cập nhật (bắt buộc, lớn hơn 0)
 * @param size       kích thước tính tiền cập nhật (nếu có)
 * @param dimensions kích thước quy cách chi tiết cập nhật (nếu có)
 */
public record CartItemUpdateRequest(
        @NotNull(message = "Số lượng không được để trống")
        @DecimalMin(value = "0.0001", message = "Số lượng phải lớn hơn 0")
        BigDecimal quantity,

        @DecimalMin(value = "0.0001", message = "Kích thước/chiều dài phải lớn hơn 0")
        BigDecimal size,

        String dimensions
) {}
