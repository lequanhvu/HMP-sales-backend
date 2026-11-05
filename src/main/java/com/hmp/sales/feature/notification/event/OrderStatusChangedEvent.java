package com.hmp.sales.feature.notification.event;

import com.hmp.sales.feature.order.entity.OrderStatus;

/**
 * Sự kiện thông báo thay đổi trạng thái đơn hàng trong hệ thống (Spring Application Event).
 *
 * @param orderId    ID đơn hàng
 * @param customerId ID khách hàng sở hữu đơn hàng
 * @param fromStatus trạng thái trước khi thay đổi (có thể null nếu là khởi tạo)
 * @param toStatus   trạng thái mới sau khi thay đổi
 * @param changedBy  ID người thực hiện thay đổi (null nếu do scheduler hệ thống)
 */
public record OrderStatusChangedEvent(
        Long orderId,
        Long customerId,
        OrderStatus fromStatus,
        OrderStatus toStatus,
        Long changedBy
) {
}

