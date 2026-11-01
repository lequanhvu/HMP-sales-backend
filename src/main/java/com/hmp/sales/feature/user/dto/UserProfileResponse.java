package com.hmp.sales.feature.user.dto;

import com.hmp.sales.feature.user.entity.User;

/**
 * DTO phản hồi thông tin hồ sơ người dùng cá nhân (UserProfileResponse).
 *
 * @param userId   ID người dùng
 * @param fullName họ và tên đầy đủ
 * @param email    địa chỉ email
 * @param phone    số điện thoại liên hệ
 * @param status   trạng thái tài khoản (ACTIVE, LOCKED)
 */
public record UserProfileResponse(
        Long userId,
        String fullName,
        String email,
        String phone,
        String status
) {
    /**
     * Chuyển đổi từ thực thể {@link User} sang DTO {@link UserProfileResponse}.
     *
     * @param user thực thể người dùng
     * @return đối tượng phản hồi hồ sơ người dùng
     */
    public static UserProfileResponse fromEntity(User user) {
        if (user == null) {
            return null;
        }
        return new UserProfileResponse(
                user.getId(),
                user.getFullName(),
                user.getEmail(),
                user.getPhone(),
                user.getStatus() != null ? user.getStatus().name() : null
        );
    }
}

