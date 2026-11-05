package com.hmp.sales.feature.notification.external;

import com.hmp.sales.feature.notification.entity.NotificationType;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * Triển khai mặc định (No-Op Stub) cho ExternalNotificationProvider ở phiên bản V1.
 * Ghi nhận log debug và không thực hiện cuộc gọi mạng ngoại vi thực tế.
 */
@Component
@Slf4j
public class NoOpExternalNotificationProvider implements ExternalNotificationProvider {

    @Override
    public void send(Long userId, String title, String message, NotificationType type) {
        log.debug("[NoOpExternalNotificationProvider] Thông báo ngoại vi được bỏ qua ở V1: userId={}, type={}, title='{}'",
                userId, type, title);
    }
}

