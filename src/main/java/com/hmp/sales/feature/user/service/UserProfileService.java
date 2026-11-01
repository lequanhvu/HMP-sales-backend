package com.hmp.sales.feature.user.service;

import com.hmp.sales.feature.user.dto.UpdateProfileRequest;
import com.hmp.sales.feature.user.dto.UserProfileResponse;

/**
 * Service quản lý thông tin hồ sơ người dùng cá nhân (Profile).
 */
public interface UserProfileService {

    /**
     * Lấy thông tin hồ sơ của chính người dùng đang đăng nhập.
     *
     * @param userId ID người dùng
     * @return thông tin hồ sơ cá nhân {@link UserProfileResponse}
     * @throws com.hmp.sales.common.exception.ResourceNotFoundException nếu không tìm thấy người dùng
     */
    UserProfileResponse getProfile(Long userId);

    /**
     * Cập nhật thông tin hồ sơ của người dùng đang đăng nhập.
     * <p>
     * Nếu cập nhật số điện thoại (lần đầu hoặc thay đổi), yêu cầu số điện thoại đã được xác thực OTP
     * hợp lệ (tồn tại bảo chứng xác thực trên Redis).
     *
     * @param userId  ID người dùng
     * @param request dữ liệu yêu cầu cập nhật hồ sơ
     * @return thông tin hồ sơ sau khi cập nhật thành công {@link UserProfileResponse}
     * @throws com.hmp.sales.common.exception.ResourceNotFoundException nếu không tìm thấy người dùng
     * @throws com.hmp.sales.common.exception.BusinessRuleException     nếu số điện thoại mới chưa xác thực OTP
     */
    UserProfileResponse updateProfile(Long userId, UpdateProfileRequest request);
}

