package com.hmp.sales.feature.setting.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Payload cập nhật giá trị một tham số cấu hình.
 *
 * @param value giá trị mới dạng chuỗi (tối đa 1000 ký tự), được kiểm tra theo kiểu dữ liệu của tham số
 */
public record UpdateSettingRequest(
        @NotBlank(message = "Giá trị cấu hình không được để trống")
        @Size(max = 1000, message = "Giá trị cấu hình tối đa 1000 ký tự")
        String value
) {
}

