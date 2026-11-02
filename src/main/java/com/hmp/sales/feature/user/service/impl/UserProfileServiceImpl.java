package com.hmp.sales.feature.user.service.impl;

import com.hmp.sales.common.exception.BusinessRuleException;
import com.hmp.sales.common.exception.ResourceNotFoundException;
import com.hmp.sales.feature.otp.service.PhoneOtpService;
import com.hmp.sales.feature.user.dto.UpdateProfileRequest;
import com.hmp.sales.feature.user.dto.UserProfileResponse;
import com.hmp.sales.feature.user.entity.User;
import com.hmp.sales.feature.user.repository.UserRepository;
import com.hmp.sales.feature.user.service.UserProfileService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Objects;

/**
 * Triển khai dịch vụ quản lý hồ sơ cá nhân của người dùng (UserProfileService).
 */
@Slf4j
@Service
public class UserProfileServiceImpl implements UserProfileService {

    private final UserRepository userRepository;
    private final PhoneOtpService phoneOtpService;

    /**
     * Khởi tạo UserProfileServiceImpl với các phụ thuộc cần thiết.
     *
     * @param userRepository  repository truy xuất người dùng
     * @param phoneOtpService service xác thực OTP số điện thoại
     */
    public UserProfileServiceImpl(UserRepository userRepository, PhoneOtpService phoneOtpService) {
        this.userRepository = userRepository;
        this.phoneOtpService = phoneOtpService;
    }

    @Override
    @Transactional(readOnly = true)
    public UserProfileResponse getProfile(Long userId) {
        User user = findActiveUser(userId);
        return UserProfileResponse.fromEntity(user);
    }

    @Override
    @Transactional
    public UserProfileResponse updateProfile(Long userId, UpdateProfileRequest request) {
        User user = findActiveUser(userId);

        if (request.fullName() != null && !request.fullName().isBlank()) {
            user.setFullName(request.fullName().trim());
        }

        if (request.phone() != null && !request.phone().isBlank()) {
            String newPhone = request.phone().trim();
            if (!Objects.equals(user.getPhone(), newPhone)) {
                if (!phoneOtpService.isVerified(newPhone)) {
                    throw new BusinessRuleException("Số điện thoại cần được xác thực OTP trước khi cập nhật.");
                }
                user.setPhone(newPhone);
            }
        }

        User savedUser = userRepository.save(user);
        log.info("[UserProfileService] Đã cập nhật hồ sơ cho người dùng ID: {}", userId);
        return UserProfileResponse.fromEntity(savedUser);
    }

    private User findActiveUser(Long userId) {
        if (userId == null) {
            throw new ResourceNotFoundException("Không tìm thấy người dùng");
        }
        return userRepository.findById(userId)
                .filter(u -> !Boolean.TRUE.equals(u.getIsDeleted()))
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy người dùng ID: " + userId));
    }
}

