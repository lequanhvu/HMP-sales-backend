package com.hmp.sales.feature.notification.external;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hmp.sales.config.AppProperties;
import com.hmp.sales.feature.notification.entity.NotificationType;
import com.hmp.sales.feature.user.entity.User;
import com.hmp.sales.feature.user.repository.UserRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.Map;

/**
 * Nhà cung cấp gửi thông báo qua Zalo ZNS (Zalo Notification Service).
 * Sử dụng {@link RestClient} để giao tiếp với OpenAPI của Zalo.
 * Chỉ được khởi tạo khi thuộc tính cấu hình {@code app.notification.zalo.enabled=true}.
 */
@Component
@ConditionalOnProperty(name = "app.notification.zalo.enabled", havingValue = "true")
@Slf4j
public class ZaloNotificationProvider implements ExternalNotificationProvider {

    private static final String ZALO_ZNS_URL = "https://business.openapi.zalo.me/message/template";

    private final UserRepository userRepository;
    private final AppProperties appProperties;
    private final RestClient restClient;
    private final ObjectMapper objectMapper;

    /**
     * Khởi tạo ZaloNotificationProvider với RestClient mặc định.
     *
     * @param userRepository repository truy vấn thông tin người dùng
     * @param appProperties  cấu hình ứng dụng chứa thông số Zalo OA và template mapping
     * @param objectMapper   đối tượng parse JSON
     */
    public ZaloNotificationProvider(
            UserRepository userRepository,
            AppProperties appProperties,
            ObjectMapper objectMapper
    ) {
        this(userRepository, appProperties, RestClient.create(), objectMapper);
    }

    /**
     * Khởi tạo ZaloNotificationProvider với RestClient tùy chỉnh (hỗ trợ kiểm thử).
     *
     * @param userRepository repository truy vấn thông tin người dùng
     * @param appProperties  cấu hình ứng dụng
     * @param restClient     RestClient để thực hiện HTTP request
     * @param objectMapper   đối tượng parse JSON
     */
    public ZaloNotificationProvider(
            UserRepository userRepository,
            AppProperties appProperties,
            RestClient restClient,
            ObjectMapper objectMapper
    ) {
        this.userRepository = userRepository;
        this.appProperties = appProperties;
        this.restClient = restClient;
        this.objectMapper = objectMapper;
    }

    /**
     * Gửi tin nhắn ZNS thông báo trạng thái đơn hàng tới Zalo người dùng.
     *
     * @param userId  ID định danh người nhận trong hệ thống
     * @param title   tiêu đề thông báo
     * @param message nội dung chi tiết thông báo
     * @param type    loại thông báo nghiệp vụ
     * @throws RuntimeException nếu API Zalo trả về mã lỗi hoặc gặp sự cố truyền thông
     */
    @Override
    public void send(Long userId, String title, String message, NotificationType type) {
        User user = userRepository.findById(userId).orElse(null);
        if (user == null) {
            log.warn("Không tìm thấy người dùng ID={} để gửi thông báo Zalo ZNS", userId);
            return;
        }

        String phone = user.getPhone();
        if (phone == null || phone.isBlank()) {
            log.warn("Người dùng ID={} chưa có số điện thoại để gửi Zalo ZNS", userId);
            return;
        }

        AppProperties.ZaloProperties zaloConfig = appProperties.notification().zalo();
        String templateId = zaloConfig.templates().get(type.name());
        if (templateId == null || templateId.isBlank()) {
            log.warn("Chưa cấu hình template Zalo ZNS cho loại thông báo: {}", type);
            return;
        }

        String formattedPhone = formatPhoneNumber(phone);

        Map<String, Object> payload = Map.of(
                "phone", formattedPhone,
                "template_id", templateId,
                "template_data", Map.of(
                        "title", title,
                        "content", message
                )
        );

        try {
            log.info("Bắt đầu gửi Zalo ZNS tới phone={} với templateId={}, type={}",
                    maskPhone(formattedPhone), templateId, type);

            String responseBody = restClient.post()
                    .uri(ZALO_ZNS_URL)
                    .header("access_token", zaloConfig.accessToken())
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(payload)
                    .retrieve()
                    .body(String.class);

            if (responseBody != null) {
                JsonNode root = objectMapper.readTree(responseBody);
                int errorCode = root.path("error").asInt(0);
                if (errorCode != 0) {
                    String errorMsg = root.path("message").asText("Lỗi không xác định từ Zalo ZNS");
                    log.error("Zalo ZNS trả về mã lỗi error={}, message={}", errorCode, errorMsg);
                    throw new RuntimeException("Gửi Zalo ZNS thất bại: " + errorMsg + " (error=" + errorCode + ")");
                }
            }

            log.info("Gửi Zalo ZNS thành công tới userId={}, type={}", userId, type);
        } catch (Exception ex) {
            log.error("Lỗi khi gọi API Zalo ZNS cho userId={}: {}", userId, ex.getMessage());
            if (ex.getMessage() != null && ex.getMessage().contains("Gửi Zalo ZNS thất bại")) {
                throw (RuntimeException) ex;
            }
            throw new RuntimeException("Lỗi truyền thông Zalo ZNS", ex);
        }
    }

    /**
     * Chuẩn hóa định dạng số điện thoại theo quy ước Zalo ZNS (đầu số quốc tế 84).
     *
     * @param phone số điện thoại ban đầu
     * @return số điện thoại đã chuẩn hóa (bắt đầu bằng 84)
     */
    private String formatPhoneNumber(String phone) {
        String cleaned = phone.replaceAll("[^0-9]", "");
        if (cleaned.startsWith("0")) {
            return "84" + cleaned.substring(1);
        }
        return cleaned;
    }

    /**
     * Che bớt số điện thoại để bảo mật khi ghi log.
     */
    private String maskPhone(String phone) {
        if (phone.length() <= 4) {
            return "****";
        }
        return phone.substring(0, phone.length() - 4).replaceAll(".", "*") + phone.substring(phone.length() - 4);
    }
}
