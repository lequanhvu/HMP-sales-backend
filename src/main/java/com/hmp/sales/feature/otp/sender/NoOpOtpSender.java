package com.hmp.sales.feature.otp.sender;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * Bộ gửi OTP giả lập (No-Op Stub) cho môi trường Local / Development / Test.
 * <p>
 * Kích hoạt khi {@code app.sms.enabled=false} hoặc chưa cấu hình thông số viễn thông.
 * Giúp developer và tester có thể nhận được mã OTP trên console mà không phát sinh chi phí viễn thông.
 */
@Component
@ConditionalOnProperty(name = "app.sms.enabled", havingValue = "false", matchIfMissing = true)
public class NoOpOtpSender implements OtpSender {

    private static final Logger log = LoggerFactory.getLogger(NoOpOtpSender.class);

    /**
     * {@inheritDoc}
     */
    @Override
    public void sendOtp(String phone, String otpCode) {
        log.info("[OTP-DEV] Giả lập gửi tin nhắn SMS OTP thành công: phone={} code={}", phone, otpCode);
    }
}
