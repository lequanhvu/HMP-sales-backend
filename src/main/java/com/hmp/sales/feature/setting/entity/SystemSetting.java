package com.hmp.sales.feature.setting.entity;

import com.hmp.sales.feature.user.entity.User;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
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
 * Thực thể tham số cấu hình hệ thống (SystemSetting).
 * <p>
 * Lưu trữ các tham số vận hành có thể cấu hình động bởi Quản trị viên (Admin)
 * tại bảng {@code system_settings}, ví dụ như thời gian tối đa khách hàng được phép hủy đơn hàng.
 * </p>
 */
@Entity
@Table(name = "system_settings")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SystemSetting {

    /**
     * Khóa chính tự tăng (BIGINT IDENTITY).
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "setting_id")
    private Long id;

    /**
     * Khóa định danh duy nhất của tham số cấu hình (ví dụ: ORDER_CUSTOMER_CANCELLATION_WINDOW_MINUTES).
     */
    @Column(name = "setting_key", nullable = false, unique = true, length = 100)
    private String settingKey;

    /**
     * Giá trị tham số dạng chuỗi ký tự (hỗ trợ serialize cho các kiểu dữ liệu khác nhau).
     */
    @Column(name = "setting_value", nullable = false, length = 1000)
    private String settingValue;

    /**
     * Kiểu dữ liệu của giá trị tham số (INT, STRING, BOOLEAN, DECIMAL,...).
     */
    @Column(name = "data_type", nullable = false, length = 30)
    @Builder.Default
    private String dataType = "STRING";

    /**
     * Mô tả ý nghĩa nghiệp vụ của tham số cấu hình.
     */
    @Column(name = "description", length = 500)
    private String description;

    /**
     * Người cập nhật tham số cấu hình gần nhất (FK User).
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "updated_by")
    private User updatedBy;

    /**
     * Thời điểm cấu hình được cập nhật gần nhất.
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
     * Tự động khởi tạo thời gian cập nhật trước khi lưu vào DB.
     */
    @PrePersist
    public void prePersist() {
        if (this.updatedAt == null) {
            this.updatedAt = Instant.now();
        }
        if (this.isDeleted == null) {
            this.isDeleted = false;
        }
    }

    /**
     * Tự động làm mới thời gian cập nhật trước khi update vào DB.
     */
    @PreUpdate
    public void preUpdate() {
        this.updatedAt = Instant.now();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        SystemSetting that = (SystemSetting) o;
        return id != null && Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
}

