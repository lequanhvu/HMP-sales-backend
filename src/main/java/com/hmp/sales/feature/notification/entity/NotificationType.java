package com.hmp.sales.feature.notification.entity;

/**
 * Phân loại loại thông báo gửi tới người dùng trong hệ thống.
 */
public enum NotificationType {
    /**
     * Đơn hàng đã được tạo thành công bởi khách hàng (đang chờ Quản lý xác nhận).
     */
    ORDER_CREATED,

    /**
     * Đơn hàng đã được xác nhận (Quản lý duyệt đơn và chốt giá).
     */
    ORDER_CONFIRMED,

    /**
     * Đơn hàng bắt đầu được chuyển sang quy trình chuẩn bị hàng.
     */
    ORDER_PROCESSING,

    /**
     * Đơn hàng đang được vận chuyển giao tới khách hàng.
     */
    ORDER_SHIPPING,

    /**
     * Đơn hàng đã giao thành công và hoàn tất.
     */
    ORDER_COMPLETED,

    /**
     * Đơn hàng đã bị hủy.
     */
    ORDER_CANCELLED
}

