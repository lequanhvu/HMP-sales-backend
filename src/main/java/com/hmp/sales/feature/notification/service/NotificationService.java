package com.hmp.sales.feature.notification.service;

import com.hmp.sales.common.response.PageResponse;
import com.hmp.sales.feature.notification.dto.MarkAllReadResponse;
import com.hmp.sales.feature.notification.dto.NotificationResponse;
import com.hmp.sales.feature.notification.dto.UnreadCountResponse;
import com.hmp.sales.feature.notification.event.OrderStatusChangedEvent;
import org.springframework.data.domain.Pageable;

/**
 * Service định nghĩa các hợp đồng nghiệp vụ quản lý và phân phát thông báo tới người dùng.
 */
public interface NotificationService {

    /**
     * Lấy danh sách thông báo phân trang của người dùng hiện tại.
     *
     * @param userId   ID người dùng
     * @param pageable thông tin phân trang
     * @return danh sách thông báo phân trang
     */
    PageResponse<NotificationResponse> getOwnNotifications(Long userId, Pageable pageable);

    /**
     * Đếm số lượng thông báo chưa đọc của người dùng.
     *
     * @param userId ID người dùng
     * @return DTO chứa số lượng thông báo chưa đọc
     */
    UnreadCountResponse countUnread(Long userId);

    /**
     * Đánh dấu một thông báo cụ thể là đã đọc.
     *
     * @param userId         ID người dùng
     * @param notificationId ID thông báo cần đánh dấu
     * @return thông tin chi tiết thông báo sau khi cập nhật
     */
    NotificationResponse markAsRead(Long userId, Long notificationId);

    /**
     * Đánh dấu tất cả thông báo chưa đọc của người dùng là đã đọc.
     *
     * @param userId ID người dùng
     * @return DTO chứa số lượng bản ghi đã được cập nhật
     */
    MarkAllReadResponse markAllAsRead(Long userId);

    /**
     * Lắng nghe sự kiện thay đổi trạng thái đơn hàng và điều phối tạo thông báo.
     *
     * @param event sự kiện thay đổi trạng thái đơn hàng
     */
    void onOrderStatusChanged(OrderStatusChangedEvent event);
}

