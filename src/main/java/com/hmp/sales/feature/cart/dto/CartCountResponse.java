package com.hmp.sales.feature.cart.dto;

/**
 * Response DTO đếm số lượng món trong giỏ hàng để hiển thị huy hiệu (Badge) trên Header.
 *
 * @param totalCount tổng số lượng món
 */
public record CartCountResponse(
        long totalCount
) {}
