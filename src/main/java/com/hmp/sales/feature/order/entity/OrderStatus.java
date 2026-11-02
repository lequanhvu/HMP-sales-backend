package com.hmp.sales.feature.order.entity;

/**
 * Enum đại diện cho các trạng thái trong vòng đời của đơn hàng.
 */
public enum OrderStatus {

    /**
     * Đơn hàng mới tạo, đang chờ Quản lý tiếp nhận và tính toán đơn giá.
     */
    PENDING,

    /**
     * Đơn hàng đã được Quản lý tính giá và chốt giá.
     * Khách hàng có thể vào xem tổng tiền và có quyền hủy trong thời hạn cancellationWindowMinutes.
     */
    CONFIRMED,

    /**
     * Đơn hàng đang trong quá trình gia công sản xuất tôn hoặc chuẩn bị hàng.
     * Tự động chuyển từ CONFIRMED sau khi hết thời hạn hủy hoặc do Quản lý chủ động thao tác.
     */
    PROCESSING,

    /**
     * Đơn hàng đang được đóng gói và giao đến địa chỉ khách hàng.
     */
    SHIPPING,

    /**
     * Đơn hàng đã giao thành công và hoàn tất.
     */
    COMPLETED,

    /**
     * Đơn hàng đã bị hủy (bởi Khách hàng hoặc Quản lý).
     */
    CANCELLED
}

