package com.hmp.sales.feature.auth.util;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.util.Arrays;
import java.util.Optional;

/**
 * Tiện ích tạo và xóa HttpOnly Cookie dùng để vận chuyển Refresh Token.
 * <p>
 * Refresh Token được gửi về client qua cookie {@code HttpOnly; Secure; SameSite=Lax}
 * để ngăn chặn truy cập từ JavaScript (chống XSS) và chỉ gửi khi cùng site hoặc
 * điều hướng an toàn (giảm nguy cơ CSRF).
 */
public final class CookieUtils {

    /** Tên cookie chứa Refresh Token. */
    public static final String REFRESH_TOKEN_COOKIE = "refresh_token";

    /** Tên cookie chứa Access Token (Dành cho Cookie-Based Authentication). */
    public static final String ACCESS_TOKEN_COOKIE = "access_token";

    /** Đường dẫn cookie chỉ gửi đến endpoint xác thực. */
    public static final String AUTH_COOKIE_PATH = "/api/v1/auth";

    /** Đường dẫn cookie Access Token áp dụng cho toàn bộ ứng dụng. */
    public static final String ROOT_COOKIE_PATH = "/";

    private CookieUtils() {
        // Utility class — không khởi tạo
    }

    /**
     * Tạo và gắn cookie {@code access_token} HttpOnly vào response.
     * Cookie áp dụng cho path {@code /} để gửi kèm trong mọi request gọi API.
     *
     * @param response      HttpServletResponse cần gắn cookie vào
     * @param rawToken      giá trị JWT của access token
     * @param maxAgeSeconds thời hạn sống của cookie (giây); thường bằng thời hạn Access Token (ví dụ: 15 phút)
     */
    public static void setAccessTokenCookie(HttpServletResponse response, String rawToken, int maxAgeSeconds) {
        Cookie cookie = new Cookie(ACCESS_TOKEN_COOKIE, rawToken);
        cookie.setHttpOnly(true);
        cookie.setSecure(false); // Đặt true khi triển khai HTTPS trong production
        cookie.setPath(ROOT_COOKIE_PATH);
        cookie.setMaxAge(maxAgeSeconds);
        response.addCookie(cookie);

        // Thêm SameSite=Lax thủ công vào Set-Cookie header
        String cookieHeader = ACCESS_TOKEN_COOKIE + "=" + rawToken
                + "; Path=" + ROOT_COOKIE_PATH
                + "; Max-Age=" + maxAgeSeconds
                + "; HttpOnly"
                + "; SameSite=Lax";
        response.addHeader("Set-Cookie", cookieHeader);
    }

    /**
     * Xóa cookie {@code access_token} bằng cách set Max-Age=0.
     *
     * @param response HttpServletResponse cần xóa cookie
     */
    public static void clearAccessTokenCookie(HttpServletResponse response) {
        Cookie cookie = new Cookie(ACCESS_TOKEN_COOKIE, "");
        cookie.setHttpOnly(true);
        cookie.setSecure(false);
        cookie.setPath(ROOT_COOKIE_PATH);
        cookie.setMaxAge(0);
        response.addCookie(cookie);

        String clearHeader = ACCESS_TOKEN_COOKIE + "="
                + "; Path=" + ROOT_COOKIE_PATH
                + "; Max-Age=0"
                + "; HttpOnly"
                + "; SameSite=Lax";
        response.addHeader("Set-Cookie", clearHeader);
    }

    /**
     * Đọc giá trị của cookie {@code access_token} từ request.
     *
     * @param request HttpServletRequest chứa cookie cần đọc
     * @return {@link Optional} chứa giá trị raw access token nếu cookie tồn tại
     */
    public static Optional<String> getAccessTokenFromCookie(HttpServletRequest request) {
        if (request.getCookies() == null) return Optional.empty();
        return Arrays.stream(request.getCookies())
                .filter(c -> ACCESS_TOKEN_COOKIE.equals(c.getName()))
                .map(Cookie::getValue)
                .filter(v -> v != null && !v.isBlank())
                .findFirst();
    }

    /**
     * Tạo và gắn cookie {@code refresh_token} HttpOnly vào response.
     * Cookie chỉ áp dụng cho path {@code /api/v1/auth} để giới hạn phạm vi gửi.
     *
     * @param response      HttpServletResponse cần gắn cookie vào
     * @param rawToken      giá trị plain text của refresh token
     * @param maxAgeSeconds thời hạn sống của cookie (giây); thường bằng thời hạn của refresh token
     */
    public static void setRefreshTokenCookie(HttpServletResponse response, String rawToken, int maxAgeSeconds) {
        Cookie cookie = new Cookie(REFRESH_TOKEN_COOKIE, rawToken);
        cookie.setHttpOnly(true);
        cookie.setSecure(false); // Đặt true khi triển khai HTTPS trong production
        cookie.setPath(AUTH_COOKIE_PATH);
        cookie.setMaxAge(maxAgeSeconds);
        // SameSite=Lax: gửi cookie khi điều hướng cross-site an toàn (GET top-level), không gửi khi POST cross-site
        // Servlet API chưa hỗ trợ trực tiếp, cần thêm qua header
        response.addCookie(cookie);
        // Thêm SameSite=Lax thủ công vào Set-Cookie header
        String cookieHeader = REFRESH_TOKEN_COOKIE + "=" + rawToken
                + "; Path=" + AUTH_COOKIE_PATH
                + "; Max-Age=" + maxAgeSeconds
                + "; HttpOnly"
                + "; SameSite=Lax";
        response.addHeader("Set-Cookie", cookieHeader);
    }

    /**
     * Xóa cookie {@code refresh_token} bằng cách set Max-Age=0.
     * Gọi khi người dùng đăng xuất hoặc phát hiện replay attack.
     *
     * @param response HttpServletResponse cần xóa cookie
     */
    public static void clearRefreshTokenCookie(HttpServletResponse response) {
        Cookie cookie = new Cookie(REFRESH_TOKEN_COOKIE, "");
        cookie.setHttpOnly(true);
        cookie.setSecure(false);
        cookie.setPath(AUTH_COOKIE_PATH);
        cookie.setMaxAge(0);
        response.addCookie(cookie);
        String clearHeader = REFRESH_TOKEN_COOKIE + "="
                + "; Path=" + AUTH_COOKIE_PATH
                + "; Max-Age=0"
                + "; HttpOnly"
                + "; SameSite=Lax";
        response.addHeader("Set-Cookie", clearHeader);
    }

    /**
     * Đọc giá trị của cookie {@code refresh_token} từ request.
     *
     * @param request HttpServletRequest chứa cookie cần đọc
     * @return {@link Optional} chứa giá trị raw refresh token nếu cookie tồn tại
     */
    public static Optional<String> getRefreshTokenFromCookie(HttpServletRequest request) {
        if (request.getCookies() == null) return Optional.empty();
        return Arrays.stream(request.getCookies())
                .filter(c -> REFRESH_TOKEN_COOKIE.equals(c.getName()))
                .map(Cookie::getValue)
                .filter(v -> v != null && !v.isBlank())
                .findFirst();
    }
}
