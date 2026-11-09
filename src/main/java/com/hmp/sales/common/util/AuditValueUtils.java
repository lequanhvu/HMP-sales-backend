package com.hmp.sales.common.util;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * Tiện ích chuẩn hóa dữ liệu trước khi ghi vào nhật ký hệ thống ({@code system_logs}).
 * <p>
 * Sử dụng Jackson {@link ObjectMapper} để serialize dữ liệu thành JSON chuẩn RFC 8259,
 * tự động loại bỏ các khóa nhạy cảm (mật khẩu, token, OTP, secret) và che thông tin liên hệ cá nhân.
 */
public final class AuditValueUtils {

    private static final Logger log = LoggerFactory.getLogger(AuditValueUtils.class);

    /**
     * Singleton ObjectMapper cấu hình tự động đăng ký modules (JavaTime, v.v.), thread-safe.
     */
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper()
            .findAndRegisterModules();

    /** Các từ khóa (không phân biệt hoa thường) khiến một trường bị loại khỏi nhật ký. */
    private static final Set<String> FORBIDDEN_KEY_FRAGMENTS =
            Set.of("password", "token", "otp", "secret", "authorization", "credential");

    private AuditValueUtils() {
        // Lớp tiện ích
    }

    /**
     * Dựng chuỗi JSON từ các cặp khóa-giá trị, giữ nguyên thứ tự chèn.
     * Bỏ qua giá trị {@code null} và mọi khóa chứa từ khóa nhạy cảm.
     *
     * @param fields các trường cần ghi (khóa → giá trị)
     * @return chuỗi JSON chuẩn dạng {@code {"k":"v"}}; {@code "{}"} nếu không còn trường hợp lệ
     */
    public static String toJson(Map<String, ?> fields) {
        if (fields == null || fields.isEmpty()) {
            return "{}";
        }

        Map<String, Object> sanitized = new LinkedHashMap<>();
        for (Map.Entry<String, ?> entry : fields.entrySet()) {
            if (entry.getKey() != null && entry.getValue() != null && !isForbiddenKey(entry.getKey())) {
                sanitized.put(entry.getKey(), entry.getValue());
            }
        }

        if (sanitized.isEmpty()) {
            return "{}";
        }

        try {
            return OBJECT_MAPPER.writeValueAsString(sanitized);
        } catch (JsonProcessingException e) {
            log.warn("[AuditValueUtils] Lỗi khi serialize JSON: {}", e.getMessage());
            return "{}";
        }
    }

    /**
     * Dựng chuỗi JSON mảng từ một danh sách các phần tử.
     *
     * @param items danh sách các phần tử
     * @return chuỗi JSON mảng dạng {@code ["item1","item2"]}; {@code "[]"} nếu danh sách null hoặc rỗng
     */
    public static String toJsonArray(Iterable<?> items) {
        if (items == null) {
            return "[]";
        }
        try {
            return OBJECT_MAPPER.writeValueAsString(items);
        } catch (JsonProcessingException e) {
            log.warn("[AuditValueUtils] Lỗi khi serialize JSON array: {}", e.getMessage());
            return "[]";
        }
    }

    /**
     * Che số điện thoại, chỉ giữ 2 ký tự đầu và 3 ký tự cuối (ví dụ {@code 09*****123}).
     *
     * @param phone số điện thoại gốc
     * @return số điện thoại đã che; {@code null} nếu đầu vào {@code null}
     */
    public static String maskPhone(String phone) {
        if (phone == null) {
            return null;
        }
        String p = phone.trim();
        if (p.length() <= 5) {
            return "*".repeat(p.length());
        }
        return p.substring(0, 2) + "*".repeat(p.length() - 5) + p.substring(p.length() - 3);
    }

    /**
     * Che email, chỉ giữ ký tự đầu của phần tên và toàn bộ tên miền (ví dụ {@code a***@gmail.com}).
     *
     * @param email email gốc
     * @return email đã che; {@code null} nếu đầu vào {@code null}
     */
    public static String maskEmail(String email) {
        if (email == null) {
            return null;
        }
        int at = email.indexOf('@');
        if (at <= 0) {
            return "***";
        }
        return email.charAt(0) + "***" + email.substring(at);
    }

    /**
     * Cắt chuỗi về tối đa {@code max} ký tự để tránh ghi nhật ký quá dài.
     *
     * @param value chuỗi gốc
     * @param max   số ký tự tối đa
     * @return chuỗi đã cắt; {@code null} nếu đầu vào {@code null}
     */
    public static String truncate(String value, int max) {
        if (value == null || value.length() <= max) {
            return value;
        }
        return value.substring(0, Math.max(0, max - 3)) + "...";
    }

    private static boolean isForbiddenKey(String key) {
        String lower = key.toLowerCase(Locale.ROOT);
        return FORBIDDEN_KEY_FRAGMENTS.stream().anyMatch(lower::contains);
    }
}
