package com.hmp.sales.feature.order.entity;

import com.hmp.sales.feature.user.entity.User;
import jakarta.persistence.CascadeType;
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
import jakarta.persistence.OneToMany;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Thực thể Đơn hàng (Order).
 * <p>
 * Lưu trữ tại bảng vật lý {@code orders} trong PostgreSQL (Source of Truth).
 * Chứa thông tin tổng quan, trạng thái, snapshot người nhận và lịch sử tính giá.
 * </p>
 */
@Entity
@Table(name = "orders")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Order {

    /**
     * Khóa chính tự tăng của đơn hàng (BIGINT IDENTITY).
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "order_id")
    private Long id;

    /**
     * Khách hàng đặt đơn hàng này.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "customer_id", nullable = false)
    private User customer;

    /**
     * Trạng thái hiện tại của đơn hàng.
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 30)
    @Builder.Default
    private OrderStatus status = OrderStatus.PENDING;

    /**
     * Tổng số tiền thanh toán của đơn hàng (VND, số nguyên).
     * Để {@code null} khi mới tạo, được cập nhật sau khi Quản lý tính giá.
     */
    @Column(name = "total_amount")
    private Long totalAmount;

    /**
     * Snapshot họ tên người nhận hàng.
     */
    @Column(name = "receiver_name", nullable = false, length = 150)
    private String receiverName;

    /**
     * Snapshot số điện thoại người nhận hàng.
     */
    @Column(name = "receiver_phone", nullable = false, length = 20)
    private String receiverPhone;

    /**
     * Snapshot địa chỉ nhận hàng chi tiết.
     */
    @Column(name = "shipping_address", nullable = false, length = 1000)
    private String shippingAddress;

    /**
     * Ghi chú của khách hàng khi đặt đơn.
     */
    @Column(name = "note", length = 1000)
    private String note;

    /**
     * Lý do hủy đơn hàng (tùy chọn cho mọi vai trò).
     */
    @Column(name = "cancellation_reason", length = 500)
    private String cancellationReason;

    /**
     * Người thực hiện hủy đơn (FK User).
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cancelled_by")
    private User cancelledBy;

    /**
     * Thời điểm đơn hàng bị hủy.
     */
    @Column(name = "cancelled_at")
    private Instant cancelledAt;

    /**
     * Quản lý thực hiện tính giá đơn hàng (FK User).
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "calculated_by")
    private User calculatedBy;

    /**
     * Thời điểm tính giá đơn hàng.
     */
    @Column(name = "calculated_at")
    private Instant calculatedAt;

    /**
     * Thời điểm đơn hàng được xác nhận giá (Quản lý confirm giá).
     * Dùng làm mốc thời gian để tính thời hạn hủy đơn (cancellationWindowMinutes).
     */
    @Column(name = "confirmed_at")
    private Instant confirmedAt;

    /**
     * Thời điểm đơn hàng hoàn tất.
     */
    @Column(name = "completed_at")
    private Instant completedAt;

    /**
     * Danh sách các dòng sản phẩm trong đơn hàng.
     */
    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @Builder.Default
    private List<OrderItem> items = new ArrayList<>();

    /**
     * Thời điểm tạo đơn hàng.
     */
    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    /**
     * Thời điểm cập nhật đơn hàng gần nhất.
     */
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    /**
     * Cờ đánh dấu xóa mềm.
     */
    @Column(name = "is_deleted", nullable = false)
    @Builder.Default
    private Boolean isDeleted = false;

    /**
     * Tự động khởi tạo thời gian tạo và cập nhật khi persist.
     */
    @PrePersist
    public void prePersist() {
        Instant now = Instant.now();
        if (this.createdAt == null) {
            this.createdAt = now;
        }
        if (this.updatedAt == null) {
            this.updatedAt = now;
        }
        if (this.isDeleted == null) {
            this.isDeleted = false;
        }
        if (this.status == null) {
            this.status = OrderStatus.PENDING;
        }
    }

    /**
     * Tự động làm mới thời gian cập nhật khi update.
     */
    @PreUpdate
    public void preUpdate() {
        this.updatedAt = Instant.now();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Order order = (Order) o;
        return id != null && Objects.equals(id, order.id);
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
}

