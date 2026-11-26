package com.hmp.sales.feature.otp.sender;

/**
 * Hợp đồng trừu tượng (abstraction) cho bộ gửi mã xác thực OTP qua viễn thông (SMS / Zalo ZNS).
 */
public interface OtpSender {

    /**
     * Gửi mã OTP tới số điện thoại người nhận.
     *
     * @param phone   số điện thoại người nhận (định dạng 10 chữ số Việt Nam)
     * @param otpCode mã xác thực 6 chữ số
     */
    void sendOtp(String phone, String otpCode);
}
