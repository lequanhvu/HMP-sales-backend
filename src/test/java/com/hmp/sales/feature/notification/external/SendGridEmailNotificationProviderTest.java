package com.hmp.sales.feature.notification.external;

import com.hmp.sales.config.AppProperties;
import com.hmp.sales.feature.notification.entity.NotificationType;
import com.hmp.sales.feature.user.entity.User;
import com.hmp.sales.feature.user.repository.UserRepository;
import com.sendgrid.Request;
import com.sendgrid.Response;
import com.sendgrid.SendGrid;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import java.io.IOException;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * Unit tests cho {@link SendGridEmailNotificationProvider}.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("SendGridEmailNotificationProvider Unit Tests")
class SendGridEmailNotificationProviderTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private TemplateEngine templateEngine;

    @Mock
    private SendGrid sendGrid;

    private AppProperties appProperties;
    private SendGridEmailNotificationProvider provider;

    @BeforeEach
    void setUp() {
        AppProperties.EmailProperties email = new AppProperties.EmailProperties(
                true,
                "no-reply@hmp.vn",
                "HMP Sales",
                "SG.test_key_123"
        );
        AppProperties.NotificationProperties notification = new AppProperties.NotificationProperties(
                new AppProperties.ZaloProperties(false, "", "", "", java.util.Map.of()),
                email
        );
        appProperties = new AppProperties(
                null, null, null, null, null, null, notification
        );

        provider = new SendGridEmailNotificationProvider(userRepository, appProperties, templateEngine, sendGrid);
    }

    @Test
    @DisplayName("send: Thành công khi người dùng có Email và SendGrid trả về 202")
    void send_success() throws IOException {
        User user = User.builder()
                .id(1L)
                .email("customer@example.com")
                .fullName("Nguyễn Văn A")
                .build();
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(templateEngine.process(eq("email/order-confirmed"), any(Context.class)))
                .thenReturn("<html><body>Nội dung email</body></html>");

        Response response = new Response();
        response.setStatusCode(202);
        response.setBody("Accepted");
        when(sendGrid.api(any(Request.class))).thenReturn(response);

        assertThatCode(() -> provider.send(1L, "Đơn hàng đã được xác nhận", "Chi tiết đơn hàng", NotificationType.ORDER_CONFIRMED))
                .doesNotThrowAnyException();

        verify(sendGrid).api(any(Request.class));
    }

    @Test
    @DisplayName("send: Thành công với loại ORDER_CREATED sử dụng template email/order-created")
    void send_orderCreated_success() throws IOException {
        User user = User.builder()
                .id(1L)
                .email("customer@example.com")
                .fullName("Nguyễn Văn A")
                .build();
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(templateEngine.process(eq("email/order-created"), any(Context.class)))
                .thenReturn("<html><body>Đặt hàng thành công</body></html>");

        Response response = new Response();
        response.setStatusCode(202);
        response.setBody("Accepted");
        when(sendGrid.api(any(Request.class))).thenReturn(response);

        assertThatCode(() -> provider.send(1L, "Đặt hàng thành công", "Chi tiết đơn hàng", NotificationType.ORDER_CREATED))
                .doesNotThrowAnyException();

        verify(sendGrid).api(any(Request.class));
    }

    @Test
    @DisplayName("send: Thành công với loại ORDER_RESTORED sử dụng template email/order-restored")
    void send_orderRestored_success() throws IOException {
        User user = User.builder()
                .id(1L)
                .email("customer@example.com")
                .fullName("Nguyễn Văn A")
                .build();
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(templateEngine.process(eq("email/order-restored"), any(Context.class)))
                .thenReturn("<html><body>Đơn hàng đã khôi phục</body></html>");

        Response response = new Response();
        response.setStatusCode(202);
        response.setBody("Accepted");
        when(sendGrid.api(any(Request.class))).thenReturn(response);

        assertThatCode(() -> provider.send(1L, "Đơn hàng đã được khôi phục", "Khôi phục thành công", NotificationType.ORDER_RESTORED))
                .doesNotThrowAnyException();

        verify(templateEngine).process(eq("email/order-restored"), any(Context.class));
    }

    @Test
    @DisplayName("send: Bỏ qua khi không tìm thấy người dùng")
    void send_userNotFound_skips() throws IOException {
        when(userRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatCode(() -> provider.send(99L, "Tiêu đề", "Nội dung", NotificationType.ORDER_CONFIRMED))
                .doesNotThrowAnyException();

        verify(sendGrid, never()).api(any(Request.class));
    }

    @Test
    @DisplayName("send: Bỏ qua khi người dùng chưa có địa chỉ email")
    void send_emailNullOrBlank_skips() throws IOException {
        User user = User.builder()
                .id(2L)
                .email(null)
                .fullName("Trần Thị B")
                .build();
        when(userRepository.findById(2L)).thenReturn(Optional.of(user));

        assertThatCode(() -> provider.send(2L, "Tiêu đề", "Nội dung", NotificationType.ORDER_CONFIRMED))
                .doesNotThrowAnyException();

        verify(sendGrid, never()).api(any(Request.class));
    }

    @Test
    @DisplayName("send: Ném RuntimeException khi SendGrid API trả về HTTP status lỗi (401)")
    void send_apiError_throwsException() throws IOException {
        User user = User.builder()
                .id(1L)
                .email("customer@example.com")
                .fullName("Nguyễn Văn A")
                .build();
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(templateEngine.process(anyString(), any(Context.class)))
                .thenReturn("<html></html>");

        Response response = new Response();
        response.setStatusCode(401);
        response.setBody("{\"errors\":[{\"message\":\"The provided authorization grant is invalid\"}]}");
        when(sendGrid.api(any(Request.class))).thenReturn(response);

        assertThatThrownBy(() -> provider.send(1L, "Tiêu đề", "Nội dung", NotificationType.ORDER_SHIPPING))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("401");
    }

    @Test
    @DisplayName("send: Ném RuntimeException khi xảy ra lỗi I/O kết nối tới SendGrid")
    void send_ioException_throwsException() throws IOException {
        User user = User.builder()
                .id(1L)
                .email("customer@example.com")
                .fullName("Nguyễn Văn A")
                .build();
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(templateEngine.process(anyString(), any(Context.class)))
                .thenReturn("<html></html>");

        when(sendGrid.api(any(Request.class))).thenThrow(new IOException("Connect timed out"));

        assertThatThrownBy(() -> provider.send(1L, "Tiêu đề", "Nội dung", NotificationType.ORDER_COMPLETED))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Lỗi truyền thông SendGrid email");
    }
}

