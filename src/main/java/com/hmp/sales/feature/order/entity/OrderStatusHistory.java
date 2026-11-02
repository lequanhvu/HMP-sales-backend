package com.hmp.sales.feature.order.entity;

import com.hmp.sales.feature.user.entity.User;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.Objects;

/**
 * Thực thể lưu vết lịch sử chuyển đổi trạng thái của đơn hàng (OrderStatusHistory).
 * <p>
 * Lưu trữ tại bảng {@code order_status_histories}. Ghi lại toàn bộ tiến trình thay đổi
 * trạng thái từ lúc khởi tạo cho đến khi hoàn thành hoặc bị hủy.
 * </p>
 */
@Entity
@Table(name = "order_status_histories")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OrderStatusHistory {

    /**
     * Khóa chính tự tăng (BIGINT IDENTITY).
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "history_id")
    private Long id;

    /**
     * Đơn hàng liên kết.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id", nullable = false)
    private Order order;

    /**
     * Trạng thái trước khi chuyển đổi (để null cho trạng thái đầu tiên PENDING).
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "from_status", length = 30)
    private OrderStatus fromStatus;

    /**
     * Trạng thái mới sau khi chuyển đổi.
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "to_status", nullable = false, length = 30)
    private OrderStatus toStatus;

    /**
     * Người thực hiện chuyển đổi trạng thái (FK User, null nếu do hệ thống tự động chuyển).
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "changed_by")
    private User changedBy;

    /**
     * Lý do chuyển đổi trạng thái hoặc ghi chú đi kèm.
     */
    @Column(name = "reason", length = 500)
    private String reason;

    /**
     * Thời điểm chuyển đổi trạng thái.
     */
    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    /**
     * Cờ đánh dấu xóa mềm.
     */
    @Column(name = "is_deleted", nullable = false)
    @Builder.Default
    private Boolean isDeleted = false;

    @PrePersist
    public void prePersist() {
        if (this.createdAt == null) {
            this.createdAt = Instant.now();
        }
        if (this.isDeleted == null) {
            this.isDeleted = false;
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        OrderStatusHistory that = (OrderStatusHistory) o;
        return id != null && Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
}

