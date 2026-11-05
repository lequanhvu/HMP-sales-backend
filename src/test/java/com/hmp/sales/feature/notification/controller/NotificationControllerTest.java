package com.hmp.sales.feature.notification.controller;

import com.hmp.sales.common.exception.GlobalExceptionHandler;
import com.hmp.sales.common.exception.ResourceNotFoundException;
import com.hmp.sales.common.response.PageResponse;
import com.hmp.sales.config.AppProperties;
import com.hmp.sales.feature.notification.dto.MarkAllReadResponse;
import com.hmp.sales.feature.notification.dto.NotificationResponse;
import com.hmp.sales.feature.notification.dto.UnreadCountResponse;
import com.hmp.sales.feature.notification.entity.NotificationType;
import com.hmp.sales.feature.notification.service.NotificationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Controller MockMvc unit test cho {@link NotificationController}.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("NotificationController MockMvc Unit Tests")
class NotificationControllerTest {

    private MockMvc mockMvc;

    @Mock
    private NotificationService notificationService;

    @Mock
    private AppProperties appProperties;

    @InjectMocks
    private NotificationController notificationController;

    private Authentication sampleAuth;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(notificationController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();

        Jwt jwt = new Jwt(
                "mock-token-value",
                Instant.now(),
                Instant.now().plusSeconds(3600),
                Map.of("alg", "none"),
                Map.of("sub", "1", "userId", 1L)
        );
        sampleAuth = new JwtAuthenticationToken(jwt);
    }

    @Test
    @DisplayName("GET /api/v1/notifications - Thành công trả về danh sách phân trang")
    void getOwnNotifications_success() throws Exception {
        NotificationResponse item = new NotificationResponse(
                5001L,
                NotificationType.ORDER_CONFIRMED,
                "Đơn hàng đã được xác nhận",
                "Đơn hàng #10025 đã được Quản lý xác nhận giá.",
                "ORDER",
                10025L,
                false,
                Instant.now(),
                null
        );

        PageResponse.PageMeta meta = new PageResponse.PageMeta(0, 20, 1L, 1);
        PageResponse<NotificationResponse> pageResponse = new PageResponse<>(List.of(item), meta);

        when(notificationService.getOwnNotifications(eq(1L), any(Pageable.class))).thenReturn(pageResponse);

        mockMvc.perform(get("/api/v1/notifications")
                        .principal(sampleAuth)
                        .param("page", "0")
                        .param("size", "20")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.statusCode").value(200))
                .andExpect(jsonPath("$.data.result[0].notificationId").value(5001))
                .andExpect(jsonPath("$.data.result[0].type").value("ORDER_CONFIRMED"))
                .andExpect(jsonPath("$.data.meta.totalElements").value(1));
    }

    @Test
    @DisplayName("GET /api/v1/notifications/unread-count - Thành công trả về số lượng chưa đọc")
    void getUnreadCount_success() throws Exception {
        when(notificationService.countUnread(1L)).thenReturn(new UnreadCountResponse(5L));

        mockMvc.perform(get("/api/v1/notifications/unread-count")
                        .principal(sampleAuth)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.statusCode").value(200))
                .andExpect(jsonPath("$.data.unreadCount").value(5));
    }

    @Test
    @DisplayName("POST /api/v1/notifications/{id}/read - Thành công đánh dấu đã đọc")
    void markAsRead_success() throws Exception {
        NotificationResponse readItem = new NotificationResponse(
                5001L,
                NotificationType.ORDER_CONFIRMED,
                "Đơn hàng đã được xác nhận",
                "Đơn hàng #10025 đã được Quản lý xác nhận giá.",
                "ORDER",
                10025L,
                true,
                Instant.now(),
                Instant.now()
        );

        when(notificationService.markAsRead(1L, 5001L)).thenReturn(readItem);

        mockMvc.perform(post("/api/v1/notifications/5001/read")
                        .principal(sampleAuth)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.statusCode").value(200))
                .andExpect(jsonPath("$.data.notificationId").value(5001))
                .andExpect(jsonPath("$.data.isRead").value(true));
    }

    @Test
    @DisplayName("POST /api/v1/notifications/{id}/read - Thất bại trả về 404 khi không tìm thấy")
    void markAsRead_notFound_returns404() throws Exception {
        when(notificationService.markAsRead(1L, 9999L))
                .thenThrow(new ResourceNotFoundException("Thông báo", 9999L));

        mockMvc.perform(post("/api/v1/notifications/9999/read")
                        .principal(sampleAuth)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.statusCode").value(404))
                .andExpect(jsonPath("$.message").value("Không tìm thấy Thông báo với mã: 9999"));
    }

    @Test
    @DisplayName("POST /api/v1/notifications/read-all - Thành công đánh dấu toàn bộ đã đọc")
    void markAllAsRead_success() throws Exception {
        when(notificationService.markAllAsRead(1L)).thenReturn(new MarkAllReadResponse(3));

        mockMvc.perform(post("/api/v1/notifications/read-all")
                        .principal(sampleAuth)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.statusCode").value(200))
                .andExpect(jsonPath("$.data.updatedCount").value(3));
    }
}
