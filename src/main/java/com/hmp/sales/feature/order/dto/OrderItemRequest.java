package com.hmp.sales.feature.order.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

/**
 * Record chứa thông tin đặt từng dòng sản phẩm trong đơn hàng.
 *
 * @param productId ID sản phẩm đặt mua
 * @param variantId ID biến thể sản phẩm (nếu có)
 * @param color     màu sắc khách hàng chọn
 * @param unit      tên đơn vị tính (m, tấm, cái,...)
 * @param quantity  số lượng đặt mua (phải lớn hơn 0)
 * @param size      kích thước/chiều dài sản xuất (mét, phải lớn hơn 0 nếu có)
 */
public record OrderItemRequest(
        @NotNull(message = "ID sản phẩm không được để trống")
        Long productId,

        Long variantId,

        @Size(max = 100, message = "Màu sắc không được vượt quá 100 ký tự")
        String color,

        @Size(max = 50, message = "Đơn vị tính không được vượt quá 50 ký tự")
        String unit,

        @NotNull(message = "Số lượng không được để trống")
        @DecimalMin(value = "0.0001", message = "Số lượng phải lớn hơn 0")
        BigDecimal quantity,

        @DecimalMin(value = "0.0001", message = "Kích thước/chiều dài phải lớn hơn 0")
        BigDecimal size
) {}

