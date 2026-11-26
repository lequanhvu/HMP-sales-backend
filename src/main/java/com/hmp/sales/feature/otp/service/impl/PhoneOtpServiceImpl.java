package com.hmp.sales.feature.otp.service.impl;

import com.hmp.sales.common.exception.BusinessRuleException;
import com.hmp.sales.common.exception.OtpVerificationException;
import com.hmp.sales.common.util.AuditValueUtils;
import com.hmp.sales.config.AppProperties;
import com.hmp.sales.feature.otp.sender.OtpSender;
import com.hmp.sales.feature.otp.service.PhoneOtpService;
import com.hmp.sales.feature.user.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.util.concurrent.TimeUnit;

/**
 * Triển khai dịch vụ OTP sử dụng Redis cho TTL, rate-limiting và attempt-tracking.
 * <p>
 * Redis key schema:
 * <ul>
 *   <li>{@code otp:phone:{phone}} — mã OTP 6 số (TTL = otpExpiry)</li>
 *   <li>{@code otp:rate:{phone}} — cooldown chống spam (TTL = resendInterval)</li>
 *   <li>{@code otp:daily:{phone}} — bộ đếm số lần nhận OTP trong ngày (TTL = 86400s)</li>
 *   <li>{@code otp:attempts:{phone}} — bộ đếm sai (TTL = otpExpiry)</li>
 *   <li>{@code otp:verified:{phone}} — bảo chứng xác thực (TTL = 600s)</li>
 * </ul>
 * Khi xác thực thành công, lưu số điện thoại vào bảng users trong PostgreSQL nếu có userId.
 */
@Service
public class PhoneOtpServiceImpl implements PhoneOtpService {

    private static final Logger log = LoggerFactory.getLogger(PhoneOtpServiceImpl.class);

    private static final String OTP_KEY_PREFIX = "otp:phone:";
    private static final String RATE_KEY_PREFIX = "otp:rate:";
    private static final String DAILY_KEY_PREFIX = "otp:daily:";
    private static final String ATTEMPTS_KEY_PREFIX = "otp:attempts:";
    private static final String VERIFIED_KEY_PREFIX = "otp:verified:";

    /** TTL của bảo chứng xác thực thành công (10 phút). */
    private static final long VERIFIED_TTL_SECONDS = 600L;
    /** TTL của bộ đếm số lần nhận OTP trong ngày (24 giờ). */
    private static final long DAILY_TTL_SECONDS = 86400L;

    private final StringRedisTemplate redisTemplate;
    private final AppProperties appProperties;
    private final UserRepository userRepository;
    private final OtpSender otpSender;
    private final SecureRandom secureRandom = new SecureRandom();

    /**
     * Khởi tạo service với đầy đủ Redis template, cấu hình, UserRepository và OtpSender.
     *
     * @param redisTemplate  Redis template dùng để thao tác key-value
     * @param appProperties  cấu hình OTP & SMS
     * @param userRepository repository truy xuất và lưu thông tin người dùng
     * @param otpSender      bộ gửi mã OTP (NoOp giả lập hoặc eSMS thật)
     */
    @Autowired
    public PhoneOtpServiceImpl(StringRedisTemplate redisTemplate,
                               AppProperties appProperties,
                               UserRepository userRepository,
                               OtpSender otpSender) {
        this.redisTemplate = redisTemplate;
        this.appProperties = appProperties;
        this.userRepository = userRepository;
        this.otpSender = otpSender;
    }

    /**
     * Khởi tạo service dự phòng (tương thích ngược với các bài kiểm thử cũ).
     *
     * @param redisTemplate  Redis template
     * @param appProperties  cấu hình ứng dụng
     * @param userRepository repository người dùng
     */
    public PhoneOtpServiceImpl(StringRedisTemplate redisTemplate,
                               AppProperties appProperties,
                               UserRepository userRepository) {
        this(redisTemplate, appProperties, userRepository, null);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void sendOtp(String phone) {
        int maxDaily = (appProperties.sms() != null) ? appProperties.sms().maxDailyPerPhone() : 5;
        String dailyKey = DAILY_KEY_PREFIX + phone;
        String dailyCountStr = redisTemplate.opsForValue().get(dailyKey);
        int dailyCount = dailyCountStr != null ? Integer.parseInt(dailyCountStr) : 0;
        if (dailyCount >= maxDaily) {
            throw new BusinessRuleException(
                    "Số điện thoại này đã vượt quá giới hạn nhận mã OTP trong ngày (tối đa "
                    + maxDaily + " lần/ngày). Vui lòng thử lại vào ngày mai.");
        }

        String rateKey = RATE_KEY_PREFIX + phone;
        if (Boolean.TRUE.equals(redisTemplate.hasKey(rateKey))) {
            throw new BusinessRuleException(
                    "Vui lòng chờ ít nhất " + appProperties.otp().resendIntervalSeconds()
                    + " giây trước khi yêu cầu gửi lại mã OTP.");
        }

        // Sinh OTP 6 chữ số (đảm bảo leading zero nếu cần)
        int otpInt = secureRandom.nextInt(1_000_000);
        String otpCode = String.format("%06d", otpInt);

        int expirySeconds = appProperties.otp().expirySeconds();
        int resendInterval = appProperties.otp().resendIntervalSeconds();

        // Lưu OTP vào Redis
        redisTemplate.opsForValue().set(OTP_KEY_PREFIX + phone, otpCode, expirySeconds, TimeUnit.SECONDS);
        // Đặt cooldown rate-limit
        redisTemplate.opsForValue().set(rateKey, "1", resendInterval, TimeUnit.SECONDS);
        // Reset bộ đếm lần nhập sai
        redisTemplate.delete(ATTEMPTS_KEY_PREFIX + phone);

        // Tăng bộ đếm hạn mức nhận OTP trong ngày
        Long newDailyCount = redisTemplate.opsForValue().increment(dailyKey);
        if (newDailyCount != null && newDailyCount == 1L) {
            redisTemplate.expire(dailyKey, DAILY_TTL_SECONDS, TimeUnit.SECONDS);
        }

        // Gửi qua OtpSender (eSMS Gateway khi production, hoặc NoOp giả lập khi dev)
        if (otpSender != null) {
            otpSender.sendOtp(phone, otpCode);
        } else {
            log.debug("[OTP] phone={} code=[HIDDEN]", AuditValueUtils.maskPhone(phone));
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @Transactional
    public void verifyOtp(String phone, String otp, Long userId) {
        String attemptsKey = ATTEMPTS_KEY_PREFIX + phone;
        String otpKey = OTP_KEY_PREFIX + phone;

        // Kiểm tra số lần nhập sai
        String attemptsStr = redisTemplate.opsForValue().get(attemptsKey);
        int attempts = attemptsStr != null ? Integer.parseInt(attemptsStr) : 0;

        if (attempts >= appProperties.otp().maxAttempts()) {
            // Xóa OTP để bắt buộc gửi lại
            redisTemplate.delete(otpKey);
            redisTemplate.delete(attemptsKey);
            throw new OtpVerificationException(
                    "Bạn đã nhập sai mã OTP quá " + appProperties.otp().maxAttempts()
                    + " lần. Vui lòng yêu cầu gửi mã mới.");
        }

        String storedOtp = redisTemplate.opsForValue().get(otpKey);
        if (storedOtp == null) {
            throw new OtpVerificationException("Mã OTP đã hết hạn hoặc không tồn tại. Vui lòng yêu cầu gửi mã mới.");
        }

        if (!storedOtp.equals(otp)) {
            // Tăng bộ đếm sai, TTL bằng TTL của OTP
            redisTemplate.opsForValue().increment(attemptsKey);
            redisTemplate.expire(attemptsKey, appProperties.otp().expirySeconds(), TimeUnit.SECONDS);
            int remaining = appProperties.otp().maxAttempts() - attempts - 1;
            throw new OtpVerificationException("Mã OTP không đúng. Bạn còn " + remaining + " lần thử.");
        }

        // OTP đúng: xóa ngay (single-use), đánh dấu verified
        redisTemplate.delete(otpKey);
        redisTemplate.delete(attemptsKey);
        redisTemplate.opsForValue().set(VERIFIED_KEY_PREFIX + phone, "1", VERIFIED_TTL_SECONDS, TimeUnit.SECONDS);

        // Lưu số điện thoại vào cơ sở dữ liệu nếu có userId
        if (userId != null && userRepository != null) {
            userRepository.findById(userId).ifPresent(user -> {
                user.setPhone(phone);
                userRepository.save(user);
                log.info("[OTP] Đã cập nhật số điện thoại '{}' vào DB cho người dùng ID={}", phone, userId);
            });
        }

        log.debug("Xác thực OTP thành công cho phone={}", phone);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @Transactional
    public void verifyOtp(String phone, String otp) {
        verifyOtp(phone, otp, null);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isVerified(String phone) {
        return Boolean.TRUE.equals(redisTemplate.hasKey(VERIFIED_KEY_PREFIX + phone));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void consumeVerification(String phone) {
        redisTemplate.delete(VERIFIED_KEY_PREFIX + phone);
    }
}
