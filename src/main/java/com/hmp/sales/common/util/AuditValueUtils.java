package com.hmp.sales.common.util;

import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * Tiện ích chuẩn hóa dữ liệu trước khi ghi vào nhật ký hệ thống ({@code system_logs}).
 * <p>
 * Đảm bảo {@code oldValue}/{@code newValue} chỉ gồm chuỗi JSON ngắn, loại bỏ các khóa nhạy cảm
 * (mật khẩu, token, OTP, secret) và che thông tin liên hệ cá nhân.
 */
public final class AuditValueUtils {

    /** Các từ khóa (không phân biệt hoa thường) khiến một trường bị loại khỏi nhật ký. */
    private static final Set<String> FORBIDDEN_KEY_FRAGMENTS =
            Set.of("password", "token", "otp", "secret", "authorization", "credential");

    private AuditValueUtils() {
        // Lớp tiện ích
    }

    /**
     * Dựng chuỗi JSON phẳng từ các cặp khóa-giá trị, giữ nguyên thứ tự chèn.
     * Bỏ qua giá trị {@code null} và mọi khóa chứa từ khóa nhạy cảm.
     *
     * @param fields các trường cần ghi (khóa → giá trị)
     * @return chuỗi JSON dạng {@code {"k":"v"}}; {@code "{}"} nếu không còn trường hợp lệ
     */
    public static String toJson(Map<String, ?> fields) {
        if (fields == null || fields.isEmpty()) {
            return "{}";
        }
        StringBuilder sb = new StringBuilder("{");
        boolean first = true;
        for (Map.Entry<String, ?> e : fields.entrySet()) {
            if (e.getKey() == null || e.getValue() == null || isForbiddenKey(e.getKey())) {
                continue;
            }
            if (!first) {
                sb.append(',');
            }
            first = false;
            sb.append('"').append(escape(e.getKey())).append("\":");
            Object v = e.getValue();
            if (v instanceof Number || v instanceof Boolean) {
                sb.append(v);
            } else {
                sb.append('"').append(escape(String.valueOf(v))).append('"');
            }
        }
        return sb.append('}').toString();
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

    private static String escape(String s) {
        return s.replace("\\", "\\\\").replace("\"", "\\\"")
                .replace("\n", "\\n").replace("\r", "\\r").replace("\t", "\\t");
    }
}

