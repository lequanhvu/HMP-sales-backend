package com.hmp.sales.feature.cart.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;

/**
 * Request DTO thêm sản phẩm vào giỏ hàng.
 *
 * @param productId  ID sản phẩm (bắt buộc)
 * @param variantId  ID biến thể (nếu có)
 * @param colorId    ID màu sắc (nếu có)
 * @param unitId     ID đơn vị tính (nếu có)
 * @param size       kích thước tính tiền (chiều dài mét, diện tích...), lớn hơn 0 nếu có
 * @param dimensions kích thước quy cách bẻ chấn chi tiết (nếu có)
 * @param quantity   số lượng đặt mua (bắt buộc, lớn hơn 0)
 */
public record CartItemAddRequest(
        @NotNull(message = "ID sản phẩm không được để trống")
        Long productId,

        Long variantId,

        @Positive(message = "Mã màu sắc không hợp lệ")
        Long colorId,

        @Positive(message = "Mã đơn vị tính không hợp lệ")
        Long unitId,

        @DecimalMin(value = "0.0001", message = "Kích thước/chiều dài phải lớn hơn 0")
        BigDecimal size,

        String dimensions,

        @NotNull(message = "Số lượng không được để trống")
        @DecimalMin(value = "0.0001", message = "Số lượng phải lớn hơn 0")
        BigDecimal quantity
) {}
