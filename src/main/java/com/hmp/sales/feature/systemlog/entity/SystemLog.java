package com.hmp.sales.feature.systemlog.entity;

import com.hmp.sales.feature.user.entity.User;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
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
 * Thực thể nhật ký hệ thống (bảng {@code system_logs}).
 * <p>
 * Là audit trail chỉ-ghi-thêm (append-only) cho các hành động quản trị và nghiệp vụ quan trọng.
 * Không có API sửa hay xóa bản ghi. Tuyệt đối không lưu mật khẩu, JWT, OTP hay secret
 * vào {@code oldValue}/{@code newValue}.
 */
@Entity
@Table(name = "system_logs", indexes = {
        @Index(name = "ix_system_logs_user_created_at", columnList = "user_id, created_at"),
        @Index(name = "ix_system_logs_entity", columnList = "entity_type, entity_id"),
        @Index(name = "ix_system_logs_action", columnList = "action")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SystemLog {

    /** Khóa chính tự tăng (BIGINT IDENTITY). */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "log_id")
    private Long id;

    /** Người thực hiện hành động; {@code null} nếu do hệ thống tự thực hiện (ví dụ scheduler). */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    /** Mã hành động dạng máy đọc được (ví dụ {@code PRODUCT_UPDATED}). */
    @Column(name = "action", nullable = false, length = 100)
    private String action;

    /** Loại thực thể bị ảnh hưởng (ví dụ {@code PRODUCT}). */
    @Column(name = "entity_type", length = 50)
    private String entityType;

    /** ID thực thể bị ảnh hưởng. */
    @Column(name = "entity_id")
    private Long entityId;

    /** Mô tả hành động bằng tiếng Việt. */
    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    /** Trạng thái trước thay đổi (chuỗi JSON ngắn, đã loại bỏ dữ liệu nhạy cảm). */
    @Column(name = "old_value", columnDefinition = "TEXT")
    private String oldValue;

    /** Trạng thái sau thay đổi (chuỗi JSON ngắn, đã loại bỏ dữ liệu nhạy cảm). */
    @Column(name = "new_value", columnDefinition = "TEXT")
    private String newValue;

    /** Thời điểm ghi nhận. */
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    /** Cờ xóa mềm (dành cho chính sách lưu trữ trong tương lai, không có API xóa). */
    @Column(name = "is_deleted", nullable = false)
    @Builder.Default
    private Boolean isDeleted = false;

    /** Khởi tạo thời gian tạo và cờ xóa mềm trước khi lưu. */
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
        SystemLog that = (SystemLog) o;
        return id != null && Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
}

