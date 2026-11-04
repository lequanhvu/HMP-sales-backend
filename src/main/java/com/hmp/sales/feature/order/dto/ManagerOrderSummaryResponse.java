package com.hmp.sales.feature.order.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.hmp.sales.feature.order.entity.Order;
import com.hmp.sales.feature.order.entity.OrderStatus;

import java.time.Instant;

/**
 * Response DTO tóm tắt thông tin đơn hàng dành cho danh sách quản lý của Quản lý / Admin.
 *
 * @param orderId        ID đơn hàng
 * @param customer       thông tin tài khoản khách hàng đặt đơn
 * @param receiverName   họ tên người nhận hàng
 * @param receiverPhone  số điện thoại người nhận hàng
 * @param status         trạng thái hiện tại của đơn hàng
 * @param subtotalAmount tổng tiền hàng trước giảm giá đơn (null nếu chưa tính giá)
 * @param discountAmount số tiền giảm giá đơn hàng
 * @param totalAmount    tổng số tiền thanh toán thực tế (null nếu chưa tính giá)
 * @param createdAt      thời điểm đặt đơn
 * @param confirmedAt    thời điểm Quản lý chốt giá
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record ManagerOrderSummaryResponse(
        Long orderId,
        CustomerSummaryDto customer,
        String receiverName,
        String receiverPhone,
        OrderStatus status,
        Long subtotalAmount,
        Long discountAmount,
        Long totalAmount,
        Instant createdAt,
        Instant confirmedAt
) {

    /**
     * Chuyển đổi từ entity {@link Order} sang DTO {@link ManagerOrderSummaryResponse}.
     *
     * @param order entity đơn hàng
     * @return DTO tóm tắt đơn hàng cho Quản lý
     */
    public static ManagerOrderSummaryResponse fromEntity(Order order) {
        if (order == null) {
            return null;
        }
        return new ManagerOrderSummaryResponse(
                order.getId(),
                CustomerSummaryDto.fromEntity(order.getCustomer()),
                order.getReceiverName(),
                order.getReceiverPhone(),
                order.getStatus(),
                order.getSubtotalAmount(),
                order.getDiscountAmount(),
                order.getTotalAmount(),
                order.getCreatedAt(),
                order.getConfirmedAt()
        );
    }
}

