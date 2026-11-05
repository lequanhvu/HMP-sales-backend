package com.hmp.sales.feature.notification.dto;

/**
 * DTO phản hồi số lượng thông báo chưa đọc của người dùng.
 *
 * @param unreadCount số thông báo chưa đọc
 */
public record UnreadCountResponse(
        long unreadCount
) {
}

