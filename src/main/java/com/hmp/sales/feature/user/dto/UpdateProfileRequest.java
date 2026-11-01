package com.hmp.sales.feature.user.dto;

import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * DTO yêu cầu cập nhật thông tin hồ sơ người dùng cá nhân (UpdateProfileRequest).
 *
 * @param fullName họ và tên mới (tùy chọn)
 * @param phone    số điện thoại mới (tùy chọn, cần xác thực OTP nếu nhập mới hoặc thay đổi)
 */
public record UpdateProfileRequest(
        @Size(min = 2, max = 150, message = "Họ và tên phải từ 2 đến 150 ký tự")
        String fullName,

        @Pattern(regexp = "^0\\d{9}$", message = "Số điện thoại phải gồm 10 chữ số và bắt đầu bằng số 0")
        String phone
) {
}

