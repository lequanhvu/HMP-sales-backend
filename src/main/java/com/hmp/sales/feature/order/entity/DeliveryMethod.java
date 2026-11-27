package com.hmp.sales.feature.order.entity;

/**
 * Hình thức nhận hàng của đơn hàng.
 */
public enum DeliveryMethod {
    /**
     * Giao hàng tận nơi (Công trình / Kho của khách hàng).
     * Bắt buộc có tọa độ ghim trên bản đồ và kiểm tra giới hạn bán kính phục vụ của xưởng.
     */
    SHIPPING,

    /**
     * Khách hàng tự vận chuyển / Nhận hàng trực tiếp tại xưởng.
     * Bỏ qua kiểm tra bán kính giao hàng.
     */
    PICKUP
}
