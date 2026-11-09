package com.hmp.sales.feature.systemlog.dto;

import java.time.Instant;

/**
 * Bản ghi nhật ký rút gọn trong danh sách tìm kiếm.
 *
 * @param logId       ID bản ghi
 * @param userId      ID người thực hiện ({@code null} nếu do hệ thống)
 * @param activity    mã hành động
 * @param description mô tả hành động
 * @param createdAt   thời điểm ghi nhận
 */
public record SystemLogSummaryResponse(
        Long logId,
        Long userId,
        String activity,
        String description,
        Instant createdAt
) {
}

