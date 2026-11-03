package com.hmp.sales.feature.order.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;

/**
 * Record chứa thông tin đặt từng dòng sản phẩm trong đơn hàng.
 *
 * @param productId ID sản phẩm đặt mua
 * @param variantId ID biến thể sản phẩm (nếu có)
 * @param colorId   mã ID màu sắc khách hàng chọn (phải thuộc biến thể của sản phẩm)
 * @param unitId    mã ID đơn vị tính khách hàng chọn (phải thuộc bảng giá của biến thể)
 * @param quantity  số lượng đặt mua (phải lớn hơn 0)
 * @param size      kích thước/chiều dài sản xuất (mét, phải lớn hơn 0 nếu có)
 */
public record OrderItemRequest(
        @NotNull(message = "ID sản phẩm không được để trống")
        Long productId,

        Long variantId,

        @Positive(message = "Mã màu sắc không hợp lệ")
        Long colorId,

        @Positive(message = "Mã đơn vị tính không hợp lệ")
        Long unitId,

        @NotNull(message = "Số lượng không được để trống")
        @DecimalMin(value = "0.0001", message = "Số lượng phải lớn hơn 0")
        BigDecimal quantity,

        @DecimalMin(value = "0.0001", message = "Kích thước/chiều dài phải lớn hơn 0")
        BigDecimal size
) {}
