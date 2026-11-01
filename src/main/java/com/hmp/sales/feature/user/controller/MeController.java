package com.hmp.sales.feature.user.controller;

import com.hmp.sales.common.response.ApiSuccessResponse;
import com.hmp.sales.common.util.SecurityUtils;
import com.hmp.sales.feature.user.dto.AddressRequest;
import com.hmp.sales.feature.user.dto.AddressResponse;
import com.hmp.sales.feature.user.dto.UpdateProfileRequest;
import com.hmp.sales.feature.user.dto.UserProfileResponse;
import com.hmp.sales.feature.user.service.CustomerAddressService;
import com.hmp.sales.feature.user.service.UserProfileService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * REST Controller quản lý thông tin hồ sơ cá nhân và sổ địa chỉ giao nhận hàng của khách hàng (/api/v1/me).
 * <p>
 * Yêu cầu người dùng phải đăng nhập hợp lệ với JWT token.
 * Controller giữ vai trò tiếp nhận HTTP request và ủy quyền xử lý nghiệp vụ cho các service.
 */
@RestController
@RequestMapping("/api/v1/me")
public class MeController {

    private final UserProfileService userProfileService;
    private final CustomerAddressService customerAddressService;

    /**
     * Khởi tạo MeController với các service nghiệp vụ tương ứng.
     *
     * @param userProfileService     service quản lý thông tin hồ sơ
     * @param customerAddressService service quản lý sổ địa chỉ giao hàng
     */
    public MeController(UserProfileService userProfileService,
                        CustomerAddressService customerAddressService) {
        this.userProfileService = userProfileService;
        this.customerAddressService = customerAddressService;
    }

    /**
     * Lấy thông tin hồ sơ cá nhân của người dùng đang đăng nhập.
     *
     * @param authentication thông tin xác thực của người dùng
     * @return phản hồi chuẩn {@link ApiSuccessResponse} chứa {@link UserProfileResponse}
     */
    @GetMapping
    public ResponseEntity<ApiSuccessResponse<UserProfileResponse>> getProfile(Authentication authentication) {
        Long userId = SecurityUtils.extractUserId(authentication);
        UserProfileResponse profile = userProfileService.getProfile(userId);
        return ResponseEntity.ok(ApiSuccessResponse.success(profile));
    }

    /**
     * Cập nhật thông tin hồ sơ cá nhân (họ tên, số điện thoại).
     *
     * @param request        dữ liệu yêu cầu cập nhật hồ sơ
     * @param authentication thông tin xác thực của người dùng
     * @return phản hồi chuẩn {@link ApiSuccessResponse} chứa {@link UserProfileResponse}
     */
    @PatchMapping
    public ResponseEntity<ApiSuccessResponse<UserProfileResponse>> updateProfile(
            @Valid @RequestBody UpdateProfileRequest request,
            Authentication authentication) {
        Long userId = SecurityUtils.extractUserId(authentication);
        UserProfileResponse profile = userProfileService.updateProfile(userId, request);
        return ResponseEntity.ok(ApiSuccessResponse.success(profile));
    }

    /**
     * Lấy danh sách địa chỉ nhận hàng cá nhân của khách hàng.
     *
     * @param authentication thông tin xác thực của khách hàng
     * @return phản hồi chuẩn {@link ApiSuccessResponse} chứa danh sách {@link AddressResponse}
     */
    @GetMapping("/addresses")
    public ResponseEntity<ApiSuccessResponse<List<AddressResponse>>> getAddresses(Authentication authentication) {
        Long userId = SecurityUtils.extractUserId(authentication);
        List<AddressResponse> addresses = customerAddressService.getAddresses(userId);
        return ResponseEntity.ok(ApiSuccessResponse.success(addresses));
    }

    /**
     * Thêm mới một địa chỉ vào sổ địa chỉ cá nhân của khách hàng.
     *
     * @param request        dữ liệu địa chỉ mới
     * @param authentication thông tin xác thực của khách hàng
     * @return phản hồi chuẩn 201 Created chứa thông tin {@link AddressResponse}
     */
    @PostMapping("/addresses")
    public ResponseEntity<ApiSuccessResponse<AddressResponse>> createAddress(
            @Valid @RequestBody AddressRequest request,
            Authentication authentication) {
        Long userId = SecurityUtils.extractUserId(authentication);
        AddressResponse address = customerAddressService.createAddress(userId, request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiSuccessResponse.created("Thành công", address));
    }

    /**
     * Cập nhật thông tin một địa chỉ nhận hàng của khách hàng.
     *
     * @param addressId      ID địa chỉ cần cập nhật
     * @param request        dữ liệu địa chỉ cập nhật
     * @param authentication thông tin xác thực của khách hàng
     * @return phản hồi chuẩn {@link ApiSuccessResponse} chứa {@link AddressResponse}
     */
    @PatchMapping("/addresses/{addressId}")
    public ResponseEntity<ApiSuccessResponse<AddressResponse>> updateAddress(
            @PathVariable Long addressId,
            @Valid @RequestBody AddressRequest request,
            Authentication authentication) {
        Long userId = SecurityUtils.extractUserId(authentication);
        AddressResponse address = customerAddressService.updateAddress(userId, addressId, request);
        return ResponseEntity.ok(ApiSuccessResponse.success(address));
    }

    /**
     * Xóa mềm một địa chỉ khỏi sổ địa chỉ của khách hàng.
     *
     * @param addressId      ID địa chỉ cần xóa
     * @param authentication thông tin xác thực của khách hàng
     * @return phản hồi chuẩn {@link ApiSuccessResponse} rỗng
     */
    @DeleteMapping("/addresses/{addressId}")
    public ResponseEntity<ApiSuccessResponse<Void>> deleteAddress(
            @PathVariable Long addressId,
            Authentication authentication) {
        Long userId = SecurityUtils.extractUserId(authentication);
        customerAddressService.deleteAddress(userId, addressId);
        return ResponseEntity.ok(ApiSuccessResponse.success(null));
    }
}
