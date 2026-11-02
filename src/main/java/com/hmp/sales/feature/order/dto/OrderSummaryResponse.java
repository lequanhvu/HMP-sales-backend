package com.hmp.sales.feature.order.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.hmp.sales.feature.order.entity.Order;
import com.hmp.sales.feature.order.entity.OrderStatus;

import java.time.Instant;

/**
 * Record tóm tắt thông tin đơn hàng, phục vụ danh sách phân trang.
 *
 * @param orderId      ID đơn hàng
 * @param status       trạng thái hiện tại
 * @param totalAmount  tổng số tiền đơn hàng (null nếu chưa tính giá)
 * @param receiverName họ tên người nhận hàng
 * @param createdAt    thời điểm tạo đơn hàng
 * @param confirmedAt  thời điểm Quản lý chốt giá (nếu có)
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record OrderSummaryResponse(
        Long orderId,
        OrderStatus status,
        Long totalAmount,
        String receiverName,
        Instant createdAt,
        Instant confirmedAt
) {

    /**
     * Chuyển đổi từ entity {@link Order} sang DTO {@link OrderSummaryResponse}.
     *
     * @param order entity đơn hàng
     * @return đối tượng DTO tóm tắt
     */
    public static OrderSummaryResponse fromEntity(Order order) {
        if (order == null) {
            return null;
        }
        return new OrderSummaryResponse(
                order.getId(),
                order.getStatus(),
                order.getTotalAmount(),
                order.getReceiverName(),
                order.getCreatedAt(),
                order.getConfirmedAt()
        );
    }
}

