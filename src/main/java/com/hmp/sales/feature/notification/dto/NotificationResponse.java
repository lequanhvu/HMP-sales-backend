package com.hmp.sales.feature.notification.dto;

import com.hmp.sales.feature.notification.entity.Notification;
import com.hmp.sales.feature.notification.entity.NotificationType;

import java.time.Instant;

/**
 * DTO đại diện cho thông tin chi tiết một thông báo trả về cho client.
 *
 * @param notificationId    ID thông báo
 * @param type              loại thông báo
 * @param title             tiêu đề
 * @param message           nội dung chi tiết
 * @param relatedEntityType loại thực thể liên kết (ví dụ "ORDER")
 * @param relatedEntityId   ID thực thể liên kết (ví dụ orderId)
 * @param isRead            trạng thái đã đọc hay chưa
 * @param createdAt         thời điểm tạo thông báo
 * @param readAt            thời điểm người dùng đọc thông báo
 */
public record NotificationResponse(
        Long notificationId,
        NotificationType type,
        String title,
        String message,
        String relatedEntityType,
        Long relatedEntityId,
        boolean isRead,
        Instant createdAt,
        Instant readAt
) {
    /**
     * Chuyển đổi từ JPA entity {@link Notification} sang {@link NotificationResponse}.
     *
     * @param entity thực thể thông báo
     * @return đối tượng NotificationResponse
     */
    public static NotificationResponse fromEntity(Notification entity) {
        return new NotificationResponse(
                entity.getNotificationId(),
                entity.getType(),
                entity.getTitle(),
                entity.getMessage(),
                entity.getRelatedEntityType(),
                entity.getRelatedEntityId(),
                entity.isRead(),
                entity.getCreatedAt(),
                entity.getReadAt()
        );
    }
}

