package com.hmp.sales.feature.order.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.hmp.sales.feature.order.entity.Order;
import com.hmp.sales.feature.order.entity.OrderStatus;

import java.time.Instant;
import java.util.List;

/**
 * Record chứa toàn bộ thông tin chi tiết của một đơn hàng.
 *
 * @param orderId            ID đơn hàng
 * @param status             trạng thái hiện tại
 * @param totalAmount        tổng số tiền thanh toán (null nếu chưa tính giá)
 * @param receiverName       họ tên người nhận hàng
 * @param receiverPhone      số điện thoại người nhận hàng
 * @param shippingAddress    địa chỉ giao hàng chi tiết
 * @param note               ghi chú đơn hàng
 * @param cancellationReason lý do hủy đơn (nếu có)
 * @param createdAt          thời điểm đặt hàng
 * @param confirmedAt        thời điểm Quản lý chốt giá
 * @param items              danh sách các dòng sản phẩm trong đơn
 * @param statusHistory      lịch sử các lần chuyển trạng thái đơn hàng
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record OrderDetailResponse(
        Long orderId,
        OrderStatus status,
        Long totalAmount,
        String receiverName,
        String receiverPhone,
        String shippingAddress,
        String note,
        String cancellationReason,
        Instant createdAt,
        Instant confirmedAt,
        List<OrderItemResponse> items,
        List<OrderStatusHistoryResponse> statusHistory
) {

    /**
     * Chuyển đổi từ entity {@link Order} cùng danh sách items và lịch sử sang DTO {@link OrderDetailResponse}.
     *
     * @param order   entity đơn hàng
     * @param items   danh sách DTO dòng sản phẩm
     * @param history danh sách DTO lịch sử chuyển trạng thái
     * @return đối tượng DTO chi tiết
     */
    public static OrderDetailResponse fromEntity(Order order,
                                                 List<OrderItemResponse> items,
                                                 List<OrderStatusHistoryResponse> history) {
        if (order == null) {
            return null;
        }
        return new OrderDetailResponse(
                order.getId(),
                order.getStatus(),
                order.getTotalAmount(),
                order.getReceiverName(),
                order.getReceiverPhone(),
                order.getShippingAddress(),
                order.getNote(),
                order.getCancellationReason(),
                order.getCreatedAt(),
                order.getConfirmedAt(),
                items,
                history
        );
    }
}

