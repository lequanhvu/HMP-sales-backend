package com.hmp.sales.feature.setting.controller;

import com.hmp.sales.common.response.ApiSuccessResponse;
import com.hmp.sales.common.util.SecurityUtils;
import com.hmp.sales.feature.setting.dto.SettingResponse;
import com.hmp.sales.feature.setting.dto.UpdateSettingRequest;
import com.hmp.sales.feature.setting.service.SystemSettingService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * REST Controller cho Admin xem và cập nhật các tham số cấu hình hệ thống.
 */
@RestController
@RequestMapping("/api/v1/admin/settings")
public class AdminSettingController {

    private final SystemSettingService systemSettingService;

    /**
     * Khởi tạo controller với service cấu hình hệ thống.
     *
     * @param systemSettingService service quản lý tham số cấu hình
     */
    public AdminSettingController(SystemSettingService systemSettingService) {
        this.systemSettingService = systemSettingService;
    }

    /**
     * Liệt kê toàn bộ tham số cấu hình hệ thống.
     *
     * @return danh sách tham số cấu hình
     */
    @GetMapping
    public ResponseEntity<ApiSuccessResponse<List<SettingResponse>>> listSettings() {
        return ResponseEntity.ok(ApiSuccessResponse.success(systemSettingService.listSettings()));
    }

    /**
     * Cập nhật giá trị một tham số cấu hình đã tồn tại. Mọi thay đổi đều được ghi nhật ký hệ thống.
     *
     * @param key            khóa tham số cần cập nhật
     * @param request        payload chứa giá trị mới
     * @param authentication thông tin xác thực của Admin đang thao tác
     * @return tham số sau khi cập nhật
     */
    @PutMapping("/{key}")
    public ResponseEntity<ApiSuccessResponse<SettingResponse>> updateSetting(
            @PathVariable String key,
            @Valid @RequestBody UpdateSettingRequest request,
            Authentication authentication) {
        Long actorId = SecurityUtils.extractUserId(authentication);
        return ResponseEntity.ok(ApiSuccessResponse.success(
                systemSettingService.updateSetting(actorId, key, request.value())));
    }
}

