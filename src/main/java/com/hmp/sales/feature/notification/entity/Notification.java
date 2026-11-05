package com.hmp.sales.feature.notification.entity;

import com.hmp.sales.feature.user.entity.User;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;
import java.util.Objects;

/**
 * Thực thể lưu trữ thông báo in-app gửi tới người dùng trong cơ sở dữ liệu PostgreSQL.
 */
@Entity
@Table(name = "notifications", indexes = {
        @Index(name = "IX_Notification_User_IsRead_CreatedAt", columnList = "user_id, is_read, created_at")
})
@Getter
@Setter
@NoArgsConstructor
public class Notification {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "notification_id")
    private Long notificationId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false, length = 50)
    private NotificationType type;

    @Column(name = "title", nullable = false, length = 250)
    private String title;

    @Column(name = "message", nullable = false, columnDefinition = "TEXT")
    private String message;

    @Column(name = "related_entity_type", length = 50)
    private String relatedEntityType;

    @Column(name = "related_entity_id")
    private Long relatedEntityId;

    @Column(name = "is_read", nullable = false)
    private boolean isRead = false;

    @Column(name = "read_at")
    private Instant readAt;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "is_deleted", nullable = false)
    private boolean isDeleted = false;

    /**
     * Khởi tạo thông báo mới với đầy đủ thông tin nghiệp vụ.
     *
     * @param user              người nhận thông báo
     * @param type              loại thông báo
     * @param title             tiêu đề thông báo
     * @param message           nội dung chi tiết thông báo
     * @param relatedEntityType loại thực thể liên kết (ví dụ "ORDER")
     * @param relatedEntityId   ID của thực thể liên kết (ví dụ orderId)
     */
    public Notification(User user, NotificationType type, String title, String message, String relatedEntityType, Long relatedEntityId) {
        this.user = user;
        this.type = type;
        this.title = title;
        this.message = message;
        this.relatedEntityType = relatedEntityType;
        this.relatedEntityId = relatedEntityId;
        this.isRead = false;
        this.isDeleted = false;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Notification that = (Notification) o;
        return notificationId != null && Objects.equals(notificationId, that.notificationId);
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
}

