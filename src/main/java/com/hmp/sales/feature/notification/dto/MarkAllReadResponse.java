package com.hmp.sales.feature.notification.dto;

/**
 * DTO phản hồi kết quả thao tác đánh dấu tất cả thông báo là đã đọc.
 *
 * @param updatedCount số lượng thông báo được cập nhật trạng thái
 */
public record MarkAllReadResponse(
        int updatedCount
) {
}

