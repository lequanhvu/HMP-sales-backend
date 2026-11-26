package com.hmp.sales.feature.otp.sender;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hmp.sales.common.exception.BusinessRuleException;
import com.hmp.sales.config.AppProperties;
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

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

/**
 * Unit tests cho {@link EsmsOtpSender}.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("EsmsOtpSender Unit Tests")
class EsmsOtpSenderTest {

    @Mock
    private RestClient restClient;

    @Mock
    private RestClient.RequestBodyUriSpec requestBodyUriSpec;

    @Mock
    private RestClient.RequestBodySpec requestBodySpec;

    @Mock
    private RestClient.ResponseSpec responseSpec;

    private ObjectMapper objectMapper;
    private static final String PHONE = "0987654321";
    private static final String OTP_CODE = "123456";

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        when(restClient.post()).thenReturn(requestBodyUriSpec);
        when(requestBodyUriSpec.uri(anyString())).thenReturn(requestBodySpec);
        when(requestBodySpec.contentType(any(MediaType.class))).thenReturn(requestBodySpec);
        doReturn(requestBodySpec).when(requestBodySpec).body(any(Object.class));
        when(requestBodySpec.retrieve()).thenReturn(responseSpec);
    }

    private AppProperties createAppProperties(boolean enabled, String apiKey, String secretKey, String brandname) {
        AppProperties.OtpProperties otpProps = new AppProperties.OtpProperties(300, 3, 60);
        AppProperties.SmsProperties smsProps = new AppProperties.SmsProperties(enabled, "esms", apiKey, secretKey, brandname, 5);
        return new AppProperties(null, otpProps, null, null, null, null, null, null, smsProps);
    }

    @Test
    @DisplayName("sendOtp - Gửi thành công khi có Brandname riêng (CodeResult = 100)")
    void sendOtp_success_withBrandname() {
        AppProperties appProperties = createAppProperties(true, "test-api-key", "test-secret-key", "HMP_SALES");
        EsmsOtpSender sender = new EsmsOtpSender(appProperties, restClient, objectMapper);

        String successJson = "{\"CodeResult\":\"100\",\"SMSID\":\"sms_12345\"}";
        when(responseSpec.body(String.class)).thenReturn(successJson);

        assertThatCode(() -> sender.sendOtp(PHONE, OTP_CODE)).doesNotThrowAnyException();
        verify(restClient).post();
    }

    @Test
    @DisplayName("sendOtp - Gửi thành công khi không có Brandname (SmsType = 8)")
    void sendOtp_success_withoutBrandname() {
        AppProperties appProperties = createAppProperties(true, "test-api-key", "test-secret-key", "");
        EsmsOtpSender sender = new EsmsOtpSender(appProperties, restClient, objectMapper);

        String successJson = "{\"CodeResult\":\"100\",\"SMSID\":\"sms_67890\"}";
        when(responseSpec.body(String.class)).thenReturn(successJson);

        assertThatCode(() -> sender.sendOtp(PHONE, OTP_CODE)).doesNotThrowAnyException();
        verify(restClient).post();
    }

    @Test
    @DisplayName("sendOtp - Thiếu API Key hoặc Secret Key → ném BusinessRuleException")
    void sendOtp_missingApiKey_throwsException() {
        AppProperties appProperties = createAppProperties(true, "", "", "HMP_SALES");
        EsmsOtpSender sender = new EsmsOtpSender(appProperties, restClient, objectMapper);

        assertThatThrownBy(() -> sender.sendOtp(PHONE, OTP_CODE))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("chưa được cấu hình đầy đủ");

        verify(restClient, never()).post();
    }

    @Test
    @DisplayName("sendOtp - eSMS Gateway trả mã lỗi (CodeResult != 100) → ném BusinessRuleException")
    void sendOtp_gatewayReturnsError_throwsException() {
        AppProperties appProperties = createAppProperties(true, "test-api-key", "test-secret-key", "HMP_SALES");
        EsmsOtpSender sender = new EsmsOtpSender(appProperties, restClient, objectMapper);

        String errorJson = "{\"CodeResult\":\"103\",\"ErrorMessage\":\"Tai khoan khong du tien\"}";
        when(responseSpec.body(String.class)).thenReturn(errorJson);

        assertThatThrownBy(() -> sender.sendOtp(PHONE, OTP_CODE))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("103");
    }

    @Test
    @DisplayName("sendOtp - eSMS Gateway trả phản hồi rỗng → ném BusinessRuleException")
    void sendOtp_emptyResponse_throwsException() {
        AppProperties appProperties = createAppProperties(true, "test-api-key", "test-secret-key", "HMP_SALES");
        EsmsOtpSender sender = new EsmsOtpSender(appProperties, restClient, objectMapper);

        when(responseSpec.body(String.class)).thenReturn("");

        assertThatThrownBy(() -> sender.sendOtp(PHONE, OTP_CODE))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("Không nhận được phản hồi");
    }

    @Test
    @DisplayName("sendOtp - Lỗi kết nối HTTP (RestClientException) → ném BusinessRuleException")
    void sendOtp_networkException_throwsException() {
        AppProperties appProperties = createAppProperties(true, "test-api-key", "test-secret-key", "HMP_SALES");
        EsmsOtpSender sender = new EsmsOtpSender(appProperties, restClient, objectMapper);

        when(responseSpec.body(String.class)).thenThrow(new RestClientException("Connection timed out"));

        assertThatThrownBy(() -> sender.sendOtp(PHONE, OTP_CODE))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("Lỗi kết nối");
    }
}
