package com.hmp.sales.feature.notification.controller;

import com.hmp.sales.common.response.ApiSuccessResponse;
import com.hmp.sales.common.response.PageResponse;
import com.hmp.sales.common.util.PaginationUtils;
import com.hmp.sales.config.AppProperties;
import com.hmp.sales.feature.notification.dto.MarkAllReadResponse;
import com.hmp.sales.feature.notification.dto.NotificationResponse;
import com.hmp.sales.feature.notification.dto.UnreadCountResponse;
import com.hmp.sales.feature.notification.service.NotificationService;
import com.hmp.sales.common.util.SecurityUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

/**
 * Controller cung cấp các RESTful API quản lý thông báo dành cho người dùng đăng nhập.
 */
@RestController
@RequestMapping("/api/v1/notifications")
public class NotificationController {

    private final NotificationService notificationService;
    private final AppProperties appProperties;

    /**
     * Khởi tạo NotificationController với service và cấu hình phân trang.
     *
     * @param notificationService service quản lý thông báo
     * @param appProperties       cấu hình hệ thống bao gồm phân trang
     */
    @Autowired
    public NotificationController(NotificationService notificationService,
                                  @Autowired(required = false) AppProperties appProperties) {
        this.notificationService = notificationService;
        this.appProperties = appProperties;
    }

    /**
     * Khởi tạo NotificationController đơn giản phục vụ unit test.
     *
     * @param notificationService service quản lý thông báo
     */
    public NotificationController(NotificationService notificationService) {
        this(notificationService, null);
    }

    /**
     * Lấy danh sách thông báo phân trang của người dùng hiện tại.
     *
     * @param page           chỉ số trang (mặc định cấu hình app.pagination.default-page)
     * @param size           kích thước trang (mặc định cấu hình app.pagination.default-page-size)
     * @param authentication thông tin xác thực của người dùng
     * @return danh sách thông báo phân trang
     */
    @GetMapping
    public ResponseEntity<ApiSuccessResponse<PageResponse<NotificationResponse>>> getOwnNotifications(
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size,
            Authentication authentication
    ) {
        Long userId = SecurityUtils.extractUserId(authentication);
        int pageNumber = PaginationUtils.resolvePage(page, appProperties);
        int pageSize = PaginationUtils.resolveSize(size, appProperties);
        Pageable pageable = PageRequest.of(pageNumber, pageSize);
        PageResponse<NotificationResponse> response = notificationService.getOwnNotifications(userId, pageable);
        return ResponseEntity.ok(ApiSuccessResponse.success(response));
    }

    /**
     * Lấy số lượng thông báo chưa đọc của người dùng hiện tại (dùng cho badge hiển thị).
     *
     * @param authentication thông tin xác thực của người dùng
     * @return DTO chứa số lượng thông báo chưa đọc
     */
    @GetMapping("/unread-count")
    public ResponseEntity<ApiSuccessResponse<UnreadCountResponse>> getUnreadCount(Authentication authentication) {
        Long userId = SecurityUtils.extractUserId(authentication);
        UnreadCountResponse response = notificationService.countUnread(userId);
        return ResponseEntity.ok(ApiSuccessResponse.success(response));
    }

    /**
     * Đánh dấu một thông báo cụ thể là đã đọc.
     *
     * @param notificationId ID thông báo
     * @param authentication thông tin xác thực của người dùng
     * @return chi tiết thông báo sau khi đánh dấu
     */
    @PostMapping("/{notificationId}/read")
    public ResponseEntity<ApiSuccessResponse<NotificationResponse>> markAsRead(
            @PathVariable Long notificationId,
            Authentication authentication
    ) {
        Long userId = SecurityUtils.extractUserId(authentication);
        NotificationResponse response = notificationService.markAsRead(userId, notificationId);
        return ResponseEntity.ok(ApiSuccessResponse.success(response));
    }

    /**
     * Đánh dấu toàn bộ thông báo chưa đọc của người dùng là đã đọc.
     *
     * @param authentication thông tin xác thực của người dùng
     * @return số lượng thông báo đã được cập nhật
     */
    @PostMapping("/read-all")
    public ResponseEntity<ApiSuccessResponse<MarkAllReadResponse>> markAllAsRead(Authentication authentication) {
        Long userId = SecurityUtils.extractUserId(authentication);
        MarkAllReadResponse response = notificationService.markAllAsRead(userId);
        return ResponseEntity.ok(ApiSuccessResponse.success(response));
    }
}
