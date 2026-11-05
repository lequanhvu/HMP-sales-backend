package com.hmp.sales.feature.notification.external;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hmp.sales.config.AppProperties;
import com.hmp.sales.feature.notification.entity.NotificationType;
import com.hmp.sales.feature.user.entity.User;
import com.hmp.sales.feature.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.http.MediaType;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

/**
 * Unit tests cho {@link ZaloNotificationProvider}.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("ZaloNotificationProvider Unit Tests")
class ZaloNotificationProviderTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private RestClient restClient;

    @Mock
    private RestClient.RequestBodyUriSpec requestBodyUriSpec;

    @Mock
    private RestClient.RequestBodySpec requestBodySpec;

    @Mock
    private RestClient.ResponseSpec responseSpec;

    private AppProperties appProperties;
    private ObjectMapper objectMapper;
    private ZaloNotificationProvider provider;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        AppProperties.ZaloProperties zalo = new AppProperties.ZaloProperties(
                true,
                "zalo-app-123",
                "secret-xyz",
                "access-token-abc",
                Map.of(
                        NotificationType.ORDER_CONFIRMED.name(), "tmpl_confirmed_123",
                        NotificationType.ORDER_COMPLETED.name(), "tmpl_completed_456"
                )
        );
        AppProperties.NotificationProperties notification = new AppProperties.NotificationProperties(
                zalo,
                new AppProperties.EmailProperties(false, "", "", "")
        );
        appProperties = new AppProperties(
                null, null, null, null, null, null, notification
        );

        provider = new ZaloNotificationProvider(userRepository, appProperties, restClient, objectMapper);
    }

    @Test
    @DisplayName("send: Thành công khi người dùng có SĐT và Zalo trả về error=0")
    void send_success() {
        User user = User.builder()
                .id(1L)
                .phone("0912345678")
                .fullName("Nguyễn Văn A")
                .build();
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));

        when(restClient.post()).thenReturn(requestBodyUriSpec);
        when(requestBodyUriSpec.uri(anyString())).thenReturn(requestBodySpec);
        when(requestBodySpec.header(anyString(), anyString())).thenReturn(requestBodySpec);
        when(requestBodySpec.contentType(any(MediaType.class))).thenReturn(requestBodySpec);
        doReturn(requestBodySpec).when(requestBodySpec).body(any(Object.class));
        when(requestBodySpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.body(String.class)).thenReturn("{\"error\":0,\"message\":\"Success\"}");

        assertThatCode(() -> provider.send(1L, "Đơn hàng xác nhận", "Nội dung", NotificationType.ORDER_CONFIRMED))
                .doesNotThrowAnyException();

        verify(restClient).post();
    }

    @Test
    @DisplayName("send: Bỏ qua khi không tìm thấy người dùng")
    void send_userNotFound_skips() {
        when(userRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatCode(() -> provider.send(99L, "Tiêu đề", "Nội dung", NotificationType.ORDER_CONFIRMED))
                .doesNotThrowAnyException();

        verify(restClient, never()).post();
    }

    @Test
    @DisplayName("send: Bỏ qua khi người dùng chưa có số điện thoại")
    void send_phoneNullOrBlank_skips() {
        User user = User.builder()
                .id(2L)
                .phone(null)
                .fullName("Trần Thị B")
                .build();
        when(userRepository.findById(2L)).thenReturn(Optional.of(user));

        assertThatCode(() -> provider.send(2L, "Tiêu đề", "Nội dung", NotificationType.ORDER_CONFIRMED))
                .doesNotThrowAnyException();

        verify(restClient, never()).post();
    }

    @Test
    @DisplayName("send: Bỏ qua khi loại thông báo chưa được cấu hình template")
    void send_templateNotConfigured_skips() {
        User user = User.builder()
                .id(3L)
                .phone("0987654321")
                .fullName("Lê Văn C")
                .build();
        when(userRepository.findById(3L)).thenReturn(Optional.of(user));

        // ORDER_CANCELLED không có trong map cấu hình
        assertThatCode(() -> provider.send(3L, "Đã hủy", "Nội dung", NotificationType.ORDER_CANCELLED))
                .doesNotThrowAnyException();

        verify(restClient, never()).post();
    }

    @Test
    @DisplayName("send: Ném RuntimeException khi Zalo API trả về error != 0")
    void send_apiError_throwsException() {
        User user = User.builder()
                .id(1L)
                .phone("0912345678")
                .fullName("Nguyễn Văn A")
                .build();
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));

        when(restClient.post()).thenReturn(requestBodyUriSpec);
        when(requestBodyUriSpec.uri(anyString())).thenReturn(requestBodySpec);
        when(requestBodySpec.header(anyString(), anyString())).thenReturn(requestBodySpec);
        when(requestBodySpec.contentType(any(MediaType.class))).thenReturn(requestBodySpec);
        doReturn(requestBodySpec).when(requestBodySpec).body(any(Object.class));
        when(requestBodySpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.body(String.class)).thenReturn("{\"error\":-216,\"message\":\"Template invalid\"}");

        assertThatThrownBy(() -> provider.send(1L, "Tiêu đề", "Nội dung", NotificationType.ORDER_CONFIRMED))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Template invalid");
    }

    @Test
    @DisplayName("send: Ném RuntimeException khi RestClient gặp sự cố mạng/HTTP")
    void send_httpError_throwsException() {
        User user = User.builder()
                .id(1L)
                .phone("0912345678")
                .fullName("Nguyễn Văn A")
                .build();
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));

        when(restClient.post()).thenReturn(requestBodyUriSpec);
        when(requestBodyUriSpec.uri(anyString())).thenReturn(requestBodySpec);
        when(requestBodySpec.header(anyString(), anyString())).thenReturn(requestBodySpec);
        when(requestBodySpec.contentType(any(MediaType.class))).thenReturn(requestBodySpec);
        doReturn(requestBodySpec).when(requestBodySpec).body(any(Object.class));
        when(requestBodySpec.retrieve()).thenThrow(new RestClientException("Connection timeout"));

        assertThatThrownBy(() -> provider.send(1L, "Tiêu đề", "Nội dung", NotificationType.ORDER_CONFIRMED))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Lỗi truyền thông Zalo ZNS");
    }
}
