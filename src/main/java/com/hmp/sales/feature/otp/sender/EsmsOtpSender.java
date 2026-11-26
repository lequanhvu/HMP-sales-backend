package com.hmp.sales.feature.otp.sender;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hmp.sales.common.exception.BusinessRuleException;
import com.hmp.sales.common.util.AuditValueUtils;
import com.hmp.sales.config.AppProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.util.HashMap;
import java.util.Map;

/**
 * Nhà cung cấp gửi SMS OTP qua cổng eSMS.vn (hoặc SMS Gateway tương thích tại Việt Nam).
 * <p>
 * Kích hoạt khi {@code app.sms.enabled=true}.
 * Hỗ trợ linh hoạt cả 2 chế độ:
 * <ul>
 *   <li><b>SmsType = 2</b>: Gửi qua SMS Brandname riêng của doanh nghiệp (khi có {@code brandname}).</li>
 *   <li><b>SmsType = 8</b>: Gửi qua đầu số cố định chuyên dụng cho OTP (khi chưa đăng ký Brandname riêng).</li>
 * </ul>
 */
@Component
@ConditionalOnProperty(name = "app.sms.enabled", havingValue = "true")
public class EsmsOtpSender implements OtpSender {

    private static final Logger log = LoggerFactory.getLogger(EsmsOtpSender.class);
    private static final String ESMS_URL = "http://rest.esms.vn/MainService.svc/json/SendMultipleMessage_V4_post_json";
    private static final String SUCCESS_CODE = "100";

    private final AppProperties appProperties;
    private final RestClient restClient;
    private final ObjectMapper objectMapper;

    /**
     * Khởi tạo EsmsOtpSender với RestClient mặc định.
     *
     * @param appProperties cấu hình ứng dụng chứa thông số SMS API
     * @param objectMapper  bộ phân giải JSON
     */
    public EsmsOtpSender(AppProperties appProperties, ObjectMapper objectMapper) {
        this(appProperties, RestClient.create(), objectMapper);
    }

    /**
     * Khởi tạo EsmsOtpSender với RestClient tùy biến (dành cho kiểm thử hoặc custom timeout).
     *
     * @param appProperties cấu hình ứng dụng chứa thông số SMS API
     * @param restClient    HTTP RestClient
     * @param objectMapper  bộ phân giải JSON
     */
    public EsmsOtpSender(AppProperties appProperties, RestClient restClient, ObjectMapper objectMapper) {
        this.appProperties = appProperties;
        this.restClient = restClient;
        this.objectMapper = objectMapper;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void sendOtp(String phone, String otpCode) {
        AppProperties.SmsProperties smsProps = appProperties.sms();
        if (smsProps == null || smsProps.apiKey().isBlank() || smsProps.secretKey().isBlank()) {
            log.error("[SMS-ESMS] Cấu hình SMS API Key hoặc Secret Key bị trống trong môi trường Production.");
            throw new BusinessRuleException("Cổng dịch vụ tin nhắn SMS chưa được cấu hình đầy đủ. Vui lòng liên hệ quản trị viên.");
        }

        String brandname = smsProps.brandname() != null ? smsProps.brandname().trim() : "";
        boolean hasBrandname = !brandname.isEmpty();
        String smsType = hasBrandname ? "2" : "8";

        int expiryMinutes = Math.max(1, appProperties.otp().expirySeconds() / 60);
        String content = "Ma xac thuc OTP cua ban tai HMP la " + otpCode + ". Ma co hieu luc trong " + expiryMinutes + " phut.";

        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("ApiKey", smsProps.apiKey());
        requestBody.put("SecretKey", smsProps.secretKey());
        requestBody.put("Content", content);
        requestBody.put("Phone", phone);
        requestBody.put("SmsType", smsType);
        if (hasBrandname) {
            requestBody.put("Brandname", brandname);
        }

        String maskedPhone = AuditValueUtils.maskPhone(phone);

        try {
            String responseStr = restClient.post()
                    .uri(ESMS_URL)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(requestBody)
                    .retrieve()
                    .body(String.class);

            if (responseStr == null || responseStr.isBlank()) {
                log.error("[SMS-ESMS] Nhận phản hồi rỗng từ eSMS Gateway cho phone={}", maskedPhone);
                throw new BusinessRuleException("Không nhận được phản hồi từ cổng tin nhắn SMS. Vui lòng thử lại sau.");
            }

            JsonNode responseJson = objectMapper.readTree(responseStr);
            String codeResult = responseJson.path("CodeResult").asText();
            String errorMessage = responseJson.path("ErrorMessage").asText("");

            if (!SUCCESS_CODE.equals(codeResult)) {
                log.error("[SMS-ESMS] Gửi SMS thất bại cho phone={}: CodeResult={}, ErrorMessage={}",
                        maskedPhone, codeResult, errorMessage);
                throw new BusinessRuleException("Cổng SMS từ chối gửi tin nhắn (mã lỗi: " + codeResult + "). Vui lòng thử lại sau.");
            }

            log.info("[SMS-ESMS] Gửi SMS OTP thành công cho phone={}, SMSID={}",
                    maskedPhone, responseJson.path("SMSID").asText("N/A"));

        } catch (RestClientException e) {
            log.error("[SMS-ESMS] Lỗi kết nối HTTP khi gọi eSMS Gateway cho phone={}: {}", maskedPhone, e.getMessage());
            throw new BusinessRuleException("Lỗi kết nối tới cổng dịch vụ tin nhắn SMS. Vui lòng thử lại sau.");
        } catch (BusinessRuleException e) {
            throw e;
        } catch (Exception e) {
            log.error("[SMS-ESMS] Lỗi xử lý dữ liệu phản hồi từ eSMS Gateway cho phone={}: {}", maskedPhone, e.getMessage(), e);
            throw new BusinessRuleException("Lỗi xử lý phản hồi từ cổng SMS. Vui lòng thử lại sau.");
        }
    }
}
