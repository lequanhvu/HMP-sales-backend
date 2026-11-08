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
 * @param subtotalAmount     tổng tiền hàng trước khi giảm giá đơn (null nếu chưa tính giá)
 * @param discountAmount     số tiền giảm giá toàn bộ đơn hàng (null nếu chưa tính giá hoặc 0)
 * @param totalAmount        tổng số tiền thanh toán thực tế (null nếu chưa tính giá)
 * @param receiverName       họ tên người nhận hàng
 * @param receiverPhone      số điện thoại người nhận hàng
 * @param shippingAddress    địa chỉ giao hàng chi tiết
 * @param note               ghi chú đơn hàng của khách hàng
 * @param noteForDiscount    ghi chú của Quản lý khi tính giá và áp dụng chiết khấu
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
        Long subtotalAmount,
        Long discountAmount,
        Long totalAmount,
        String receiverName,
        String receiverPhone,
        String shippingAddress,
        String note,
        String noteForDiscount,
        String cancellationReason,
        Instant createdAt,
        Instant confirmedAt,
        List<OrderItemResponse> items,
        List<OrderStatusHistoryResponse> statusHistory
) {

    /**
     * Constructor tương thích ngược không có noteForDiscount (mặc định null).
     */
    public OrderDetailResponse(
            Long orderId,
            OrderStatus status,
            Long subtotalAmount,
            Long discountAmount,
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
        this(
                orderId,
                status,
                subtotalAmount,
                discountAmount,
                totalAmount,
                receiverName,
                receiverPhone,
                shippingAddress,
                note,
                null,
                cancellationReason,
                createdAt,
                confirmedAt,
                items,
                historyOrEmpty(statusHistory)
        );
    }

    private static List<OrderStatusHistoryResponse> historyOrEmpty(List<OrderStatusHistoryResponse> statusHistory) {
        return statusHistory;
    }

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
        return fromEntity(order, items, history, false);
    }

    /**
     * Chuyển đổi từ entity {@link Order} cùng danh sách items và lịch sử sang DTO {@link OrderDetailResponse} kèm tùy chọn ẩn giá.
     *
     * @param order     entity đơn hàng
     * @param items     danh sách DTO dòng sản phẩm
     * @param history   danh sách DTO lịch sử chuyển trạng thái
     * @param hidePrice cờ ẩn thông tin giá tiền
     * @return đối tượng DTO chi tiết
     */
    public static OrderDetailResponse fromEntity(Order order,
                                                 List<OrderItemResponse> items,
                                                 List<OrderStatusHistoryResponse> history,
                                                 boolean hidePrice) {
        if (order == null) {
            return null;
        }
        return new OrderDetailResponse(
                order.getId(),
                order.getStatus(),
                hidePrice ? null : order.getSubtotalAmount(),
                hidePrice ? null : order.getDiscountAmount(),
                hidePrice ? null : order.getTotalAmount(),
                order.getReceiverName(),
                order.getReceiverPhone(),
                order.getShippingAddress(),
                order.getNote(),
                order.getNoteForDiscount(),
                order.getCancellationReason(),
                order.getCreatedAt(),
                order.getConfirmedAt(),
                items,
                history
        );
    }
}

