package com.hmp.sales.feature.setting.service;

import java.util.Optional;

/**
 * Interface định nghĩa dịch vụ quản lý và tra cứu tham số cấu hình hệ thống.
 */
public interface SystemSettingService {

    /**
     * Tên khóa cấu hình thời gian khách hàng được phép hủy đơn hàng (tính bằng phút).
     */
    String KEY_CANCELLATION_WINDOW_MINUTES = "ORDER_CUSTOMER_CANCELLATION_WINDOW_MINUTES";

    /**
     * Lấy thời gian tối đa khách hàng được phép hủy đơn hàng (tính bằng phút).
     * <p>
     * Ưu tiên tra cứu từ cơ sở dữ liệu (PostgreSQL); nếu chưa có hoặc không hợp lệ,
     * tự động fallback về giá trị cấu hình mặc định trong file cấu hình ứng dụng.
     * </p>
     *
     * @return số phút cho phép hủy đơn hàng
     */
    int getCancellationWindowMinutes();

    /**
     * Lấy giá trị chuỗi của một tham số cấu hình theo khóa.
     *
     * @param settingKey khóa tham số
     * @return Optional chứa giá trị cấu hình nếu tìm thấy
     */
    Optional<String> getSettingValue(String settingKey);
}

