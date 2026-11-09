package com.hmp.sales.feature.systemlog.dto;

import java.time.Instant;

/**
 * Chi tiết một bản ghi nhật ký hệ thống, gồm trạng thái trước/sau thay đổi.
 *
 * @param logId       ID bản ghi
 * @param userId      ID người thực hiện ({@code null} nếu do hệ thống)
 * @param activity    mã hành động
 * @param entityType  loại thực thể bị ảnh hưởng
 * @param entityId    ID thực thể bị ảnh hưởng
 * @param description mô tả hành động
 * @param oldValue    trạng thái trước thay đổi (đã loại bỏ dữ liệu nhạy cảm)
 * @param newValue    trạng thái sau thay đổi (đã loại bỏ dữ liệu nhạy cảm)
 * @param createdAt   thời điểm ghi nhận
 */
public record SystemLogDetailResponse(
        Long logId,
        Long userId,
        String activity,
        String entityType,
        Long entityId,
        String description,
        String oldValue,
        String newValue,
        Instant createdAt
) {
}

