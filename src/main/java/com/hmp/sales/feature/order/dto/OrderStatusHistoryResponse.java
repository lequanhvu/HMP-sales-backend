package com.hmp.sales.feature.order.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.hmp.sales.feature.order.entity.OrderStatus;
import com.hmp.sales.feature.order.entity.OrderStatusHistory;

import java.time.Instant;

/**
 * Record chứa thông tin lịch sử chuyển trạng thái của đơn hàng.
 *
 * @param historyId  ID bản ghi lịch sử
 * @param fromStatus trạng thái trước chuyển đổi
 * @param toStatus   trạng thái mới sau chuyển đổi
 * @param changedBy  ID người thực hiện chuyển đổi (null nếu hệ thống tự động chuyển)
 * @param reason     lý do hoặc ghi chú chuyển trạng thái
 * @param createdAt  thời điểm chuyển trạng thái
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record OrderStatusHistoryResponse(
        Long historyId,
        OrderStatus fromStatus,
        OrderStatus toStatus,
        Long changedBy,
        String reason,
        Instant createdAt
) {

    /**
     * Chuyển đổi từ entity {@link OrderStatusHistory} sang DTO {@link OrderStatusHistoryResponse}.
     *
     * @param history entity lịch sử trạng thái
     * @return đối tượng DTO tương ứng
     */
    public static OrderStatusHistoryResponse fromEntity(OrderStatusHistory history) {
        if (history == null) {
            return null;
        }
        return new OrderStatusHistoryResponse(
                history.getId(),
                history.getFromStatus(),
                history.getToStatus(),
                history.getChangedBy() != null ? history.getChangedBy().getId() : null,
                history.getReason(),
                history.getCreatedAt()
        );
    }
}

