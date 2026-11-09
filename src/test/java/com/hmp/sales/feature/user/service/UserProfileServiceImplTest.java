package com.hmp.sales.feature.user.service;

import com.hmp.sales.common.exception.BusinessRuleException;
import com.hmp.sales.common.exception.ResourceNotFoundException;
import com.hmp.sales.feature.otp.service.PhoneOtpService;
import com.hmp.sales.feature.user.dto.UpdateProfileRequest;
import com.hmp.sales.feature.user.dto.UserProfileResponse;
import com.hmp.sales.feature.user.entity.AuthProvider;
import com.hmp.sales.feature.user.entity.User;
import com.hmp.sales.feature.user.entity.UserStatus;
import com.hmp.sales.feature.user.repository.UserRepository;
import com.hmp.sales.feature.user.service.impl.UserProfileServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Unit test cho {@link UserProfileServiceImpl}.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("UserProfileServiceImpl Unit Tests")
class UserProfileServiceImplTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PhoneOtpService phoneOtpService;

    @Mock
    private com.hmp.sales.feature.systemlog.service.SystemLogService systemLogService;

    @InjectMocks
    private UserProfileServiceImpl userProfileService;

    private User sampleUser;

    @BeforeEach
    void setUp() {
        sampleUser = User.builder()
                .id(1L)
                .fullName("Nguyễn Văn A")
                .email("user@example.com")
                .phone("0987654321")
                .authProvider(AuthProvider.GOOGLE)
                .providerSubject("google-sub-123")
                .status(UserStatus.ACTIVE)
                .isDeleted(false)
                .build();
    }

    @Test
    @DisplayName("getProfile_success - Lấy thông tin hồ sơ thành công")
    void getProfile_success() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(sampleUser));

        UserProfileResponse response = userProfileService.getProfile(1L);

        assertThat(response).isNotNull();
        assertThat(response.userId()).isEqualTo(1L);
        assertThat(response.fullName()).isEqualTo("Nguyễn Văn A");
        assertThat(response.email()).isEqualTo("user@example.com");
        assertThat(response.phone()).isEqualTo("0987654321");
        assertThat(response.status()).isEqualTo("ACTIVE");
    }

    @Test
    @DisplayName("getProfile_userNotFound - Ném ResourceNotFoundException khi không tìm thấy user")
    void getProfile_userNotFound() {
        when(userRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userProfileService.getProfile(99L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Không tìm thấy người dùng ID: 99");
    }

    @Test
    @DisplayName("getProfile_userDeleted - Ném ResourceNotFoundException khi user đã bị xóa mềm")
    void getProfile_userDeleted() {
        sampleUser.setIsDeleted(true);
        when(userRepository.findById(1L)).thenReturn(Optional.of(sampleUser));

        assertThatThrownBy(() -> userProfileService.getProfile(1L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Không tìm thấy người dùng ID: 1");
    }

    @Test
    @DisplayName("updateProfile_updateFullNameOnly - Cập nhật họ tên thành công mà không cần kiểm tra OTP")
    void updateProfile_updateFullNameOnly() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(sampleUser));
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        UpdateProfileRequest request = new UpdateProfileRequest("Nguyễn Văn Mới", null);
        UserProfileResponse response = userProfileService.updateProfile(1L, request);

        assertThat(response.fullName()).isEqualTo("Nguyễn Văn Mới");
        assertThat(response.phone()).isEqualTo("0987654321");
        verifyNoInteractions(phoneOtpService);
    }

    @Test
    @DisplayName("updateProfile_samePhone_noOtpCheck - Giữ nguyên số điện thoại cũ không kích hoạt kiểm tra OTP")
    void updateProfile_samePhone_noOtpCheck() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(sampleUser));
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        UpdateProfileRequest request = new UpdateProfileRequest("Nguyễn Văn A", "0987654321");
        UserProfileResponse response = userProfileService.updateProfile(1L, request);

        assertThat(response.phone()).isEqualTo("0987654321");
        verifyNoInteractions(phoneOtpService);
    }

    @Test
    @DisplayName("updateProfile_newPhoneVerified_success - Đổi số điện thoại khi đã có OTP xác minh thành công")
    void updateProfile_newPhoneVerified_success() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(sampleUser));
        when(phoneOtpService.isVerified("0911223344")).thenReturn(true);
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        UpdateProfileRequest request = new UpdateProfileRequest("Nguyễn Văn B", "0911223344");
        UserProfileResponse response = userProfileService.updateProfile(1L, request);

        assertThat(response.fullName()).isEqualTo("Nguyễn Văn B");
        assertThat(response.phone()).isEqualTo("0911223344");
        verify(phoneOtpService).isVerified("0911223344");
    }

    @Test
    @DisplayName("updateProfile_newPhoneNotVerified_throwsBusinessRuleException - Đổi số điện thoại chưa xác minh OTP ném BusinessRuleException")
    void updateProfile_newPhoneNotVerified_throwsBusinessRuleException() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(sampleUser));
        when(phoneOtpService.isVerified("0911223344")).thenReturn(false);

        UpdateProfileRequest request = new UpdateProfileRequest("Nguyễn Văn B", "0911223344");

        assertThatThrownBy(() -> userProfileService.updateProfile(1L, request))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("Số điện thoại cần được xác thực OTP trước khi cập nhật.");

        verify(userRepository, never()).save(any());
    }
}

