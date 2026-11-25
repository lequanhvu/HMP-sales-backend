package com.hmp.sales.feature.cart.dto;

import java.util.List;

/**
 * Response DTO chi tiết toàn bộ giỏ hàng của người dùng.
 *
 * @param cartId     ID giỏ hàng
 * @param totalItems tổng số lượng dòng sản phẩm trong giỏ
 * @param items      danh sách các món trong giỏ (sắp xếp theo thời gian cập nhật mới nhất)
 */
public record CartResponse(
        Long cartId,
        int totalItems,
        List<CartItemResponse> items
) {}
