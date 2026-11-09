package com.hmp.sales.feature.setting.dto;

import java.time.Instant;

/**
 * Thông tin một tham số cấu hình hệ thống trả về cho Admin.
 *
 * @param key         khóa tham số
 * @param value       giá trị dạng chuỗi
 * @param dataType    kiểu dữ liệu ({@code INT}, {@code BOOLEAN}, {@code DECIMAL}, {@code STRING})
 * @param description mô tả ý nghĩa nghiệp vụ
 * @param updatedAt   thời điểm cập nhật gần nhất
 */
public record SettingResponse(
        String key,
        String value,
        String dataType,
        String description,
        Instant updatedAt
) {
}

