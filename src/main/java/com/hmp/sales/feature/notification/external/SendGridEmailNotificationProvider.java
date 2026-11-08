package com.hmp.sales.feature.notification.external;

import com.hmp.sales.config.AppProperties;
import com.hmp.sales.feature.notification.entity.NotificationType;
import com.hmp.sales.feature.user.entity.User;
import com.hmp.sales.feature.user.repository.UserRepository;
import com.sendgrid.Method;
import com.sendgrid.Request;
import com.sendgrid.Response;
import com.sendgrid.SendGrid;
import com.sendgrid.helpers.mail.Mail;
import com.sendgrid.helpers.mail.objects.Content;
import com.sendgrid.helpers.mail.objects.Email;
import com.sendgrid.helpers.mail.objects.Personalization;
import lombok.extern.slf4j.Slf4j;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

/**
 * Nhà cung cấp gửi thông báo qua Email sử dụng SendGrid API và template Thymeleaf HTML.
 * Có nội dung plain text dự phòng kèm theo HTML trực quan.
 * Chỉ được khởi tạo khi thuộc tính cấu hình {@code app.notification.email.enabled=true}.
 */
@Component
@ConditionalOnProperty(name = "app.notification.email.enabled", havingValue = "true")
@Slf4j
public class SendGridEmailNotificationProvider implements ExternalNotificationProvider {

    private final UserRepository userRepository;
    private final AppProperties appProperties;
    private final TemplateEngine templateEngine;
    private final SendGrid sendGrid;

    /**
     * Khởi tạo SendGridEmailNotificationProvider với SendGrid client khởi tạo từ API Key cấu hình.
     *
     * @param userRepository repository truy vấn thông tin người dùng
     * @param appProperties  cấu hình ứng dụng chứa API key và địa chỉ người gửi SendGrid
     * @param templateEngine engine render HTML email từ file template Thymeleaf
     */
    @Autowired 
    public SendGridEmailNotificationProvider(
            UserRepository userRepository,
            AppProperties appProperties,
            TemplateEngine templateEngine
    ) {
        this(
                userRepository,
                appProperties,
                templateEngine,
                new SendGrid(appProperties.notification().email().sendgridApiKey())
        );
    }

    /**
     * Khởi tạo SendGridEmailNotificationProvider với SendGrid client tùy chỉnh (hỗ trợ kiểm thử).
     *
     * @param userRepository repository truy vấn thông tin người dùng
     * @param appProperties  cấu hình ứng dụng
     * @param templateEngine engine render HTML email
     * @param sendGrid       SendGrid client để thực hiện gọi API
     */
    public SendGridEmailNotificationProvider(
            UserRepository userRepository,
            AppProperties appProperties,
            TemplateEngine templateEngine,
            SendGrid sendGrid
    ) {
        this.userRepository = userRepository;
        this.appProperties = appProperties;
        this.templateEngine = templateEngine;
        this.sendGrid = sendGrid;
    }

    /**
     * Gửi thông báo đơn hàng qua Email tới hòm thư của người dùng.
     *
     * @param userId  ID định danh người nhận trong hệ thống
     * @param title   tiêu đề email
     * @param message nội dung tóm tắt thông báo (sử dụng cho plain text và nhúng vào template)
     * @param type    loại thông báo nghiệp vụ
     * @throws RuntimeException nếu SendGrid trả về HTTP status lỗi hoặc xảy ra lỗi kết nối
     */
    @Override
    public void send(Long userId, String title, String message, NotificationType type) {
        User user = userRepository.findById(userId).orElse(null);
        if (user == null) {
            log.warn("Không tìm thấy người dùng ID={} để gửi email thông báo", userId);
            return;
        }

        String recipientEmail = user.getEmail();
        if (recipientEmail == null || recipientEmail.isBlank()) {
            log.warn("Người dùng ID={} chưa có địa chỉ email để gửi thông báo", userId);
            return;
        }

        AppProperties.EmailProperties emailConfig = appProperties.notification().email();
        String customerName = user.getFullName() != null && !user.getFullName().isBlank()
                ? user.getFullName()
                : "Quý khách";

        String templateName = resolveTemplateName(type);

        Context thymeleafContext = new Context();
        thymeleafContext.setVariable("customerName", customerName);
        thymeleafContext.setVariable("title", title);
        thymeleafContext.setVariable("message", message);
        thymeleafContext.setVariable("type", type.name());

        String htmlContent = templateEngine.process(templateName, thymeleafContext);

        Email from = new Email(emailConfig.fromAddress(), emailConfig.fromName());
        Email to = new Email(recipientEmail);

        Mail mail = new Mail();
        mail.setFrom(from);
        mail.setSubject(title);

        Personalization personalization = new Personalization();
        personalization.addTo(to);
        mail.addPersonalization(personalization);

        mail.addContent(new Content("text/plain", message));
        mail.addContent(new Content("text/html", htmlContent));

        try {
            log.info("Bắt đầu gửi email SendGrid tới recipient={}, type={}, template={}",
                    maskEmail(recipientEmail), type, templateName);

            Request request = new Request();
            request.setMethod(Method.POST);
            request.setEndpoint("mail/send");
            request.setBody(mail.build());

            Response response = sendGrid.api(request);

            if (response.getStatusCode() < 200 || response.getStatusCode() >= 300) {
                log.error("SendGrid API trả về mã lỗi: statusCode={}, body={}",
                        response.getStatusCode(), response.getBody());
                throw new RuntimeException("Gửi email SendGrid thất bại với HTTP status: " + response.getStatusCode());
            }

            log.info("Gửi email SendGrid thành công tới userId={}, statusCode={}", userId, response.getStatusCode());
        } catch (Exception ex) {
            log.error("Lỗi khi gửi email thông báo qua SendGrid cho userId={}: {}", userId, ex.getMessage());
            if (ex.getMessage() != null && ex.getMessage().contains("Gửi email SendGrid thất bại")) {
                throw (RuntimeException) ex;
            }
            throw new RuntimeException("Lỗi truyền thông SendGrid email", ex);
        }
    }

    /**
     * Xác định tên file template Thymeleaf tương ứng với loại thông báo.
     *
     * @param type loại thông báo đơn hàng
     * @return đường dẫn template tương đối trong resources/templates
     */
    private String resolveTemplateName(NotificationType type) {
        return switch (type) {
            case ORDER_CREATED -> "email/order-created";
            case ORDER_CONFIRMED -> "email/order-confirmed";
            case ORDER_PROCESSING -> "email/order-processing";
            case ORDER_SHIPPING -> "email/order-shipping";
            case ORDER_COMPLETED -> "email/order-completed";
            case ORDER_CANCELLED -> "email/order-cancelled";
            case ORDER_RESTORED -> "email/order-restored";
        };
    }

    /**
     * Che bớt địa chỉ email để bảo vệ quyền riêng tư khi ghi log.
     */
    private String maskEmail(String email) {
        int atIndex = email.indexOf('@');
        if (atIndex <= 2) {
            return "***" + email.substring(Math.max(0, atIndex));
        }
        return email.substring(0, 2) + "***" + email.substring(atIndex);
    }
}
