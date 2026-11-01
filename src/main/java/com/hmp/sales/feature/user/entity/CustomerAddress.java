package com.hmp.sales.feature.user.entity;

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
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.Objects;

/**
 * Thực thể địa chỉ giao nhận hàng của khách hàng (CustomerAddress).
 * <p>
 * Lưu trữ tại bảng {@code customer_addresses}. Một khách hàng có thể lưu nhiều địa chỉ
 * nhận hàng, trong đó có tối đa một địa chỉ được đánh dấu là mặc định ({@code isDefault = true}).
 * Khi đơn hàng được tạo, thông tin người nhận và địa chỉ sẽ được snapshot bất biến vào đơn hàng.
 */
@Entity
@Table(name = "customer_addresses")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CustomerAddress {

    /**
     * Khóa chính định danh địa chỉ (BIGINT IDENTITY).
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "address_id")
    private Long id;

    /**
     * Khách hàng sở hữu địa chỉ này.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "customer_id", nullable = false)
    private User customer;

    /**
     * Địa chỉ chi tiết nhận hàng (số nhà, đường, phường/xã, quận/huyện, tỉnh/thành phố).
     */
    @Column(name = "address_line", nullable = false, length = 500)
    private String addressLine;

    /**
     * Cờ đánh dấu địa chỉ mặc định của khách hàng.
     */
    @Column(name = "is_default", nullable = false)
    @Builder.Default
    private Boolean isDefault = false;

    /**
     * Trạng thái hoạt động của địa chỉ (ACTIVE, INACTIVE).
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    @Builder.Default
    private AddressStatus status = AddressStatus.ACTIVE;

    /**
     * Thời điểm tạo địa chỉ.
     */
    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    /**
     * Thời điểm cập nhật địa chỉ gần nhất.
     */
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    /**
     * Cờ đánh dấu xóa mềm (soft delete).
     */
    @Column(name = "is_deleted", nullable = false)
    @Builder.Default
    private Boolean isDeleted = false;

    /**
     * Tự động khởi tạo thời gian tạo và cập nhật khi persist entity.
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
        if (this.isDefault == null) {
            this.isDefault = false;
        }
        if (this.status == null) {
            this.status = AddressStatus.ACTIVE;
        }
        if (this.isDeleted == null) {
            this.isDeleted = false;
        }
    }

    /**
     * Tự động cập nhật thời gian sửa đổi trước khi cập nhật entity vào cơ sở dữ liệu.
     */
    @PreUpdate
    public void preUpdate() {
        this.updatedAt = Instant.now();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        CustomerAddress that = (CustomerAddress) o;
        return id != null && Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
}

