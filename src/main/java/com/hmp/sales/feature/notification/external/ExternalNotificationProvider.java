package com.hmp.sales.feature.notification.external;

import com.hmp.sales.feature.notification.entity.NotificationType;

/**
 * Interface trừu tượng (abstraction) cho nhà cung cấp gửi thông báo ngoại vi (Zalo ZNS, Email, SMS).
 */
public interface ExternalNotificationProvider {

    /**
     * Gửi thông báo tới người dùng qua kênh đối tác bên ngoài.
     *
     * @param userId  ID người nhận
     * @param title   tiêu đề thông báo
     * @param message nội dung thông báo
     * @param type    loại thông báo
     */
    void send(Long userId, String title, String message, NotificationType type);
}

