package com.hmp.sales.feature.setting.service;

import com.hmp.sales.feature.setting.dto.SettingResponse;

import java.util.List;
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
     * Tên khóa cấu hình thời gian tối đa khách hàng được phép hoàn tác hủy đơn hàng (tính bằng phút).
     */
    String KEY_UNDO_CANCEL_WINDOW_MINUTES = "ORDER_UNDO_CANCEL_WINDOW_MINUTES";

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
     * Lấy thời gian tối đa khách hàng được phép hoàn tác hủy đơn hàng (tính bằng phút).
     * <p>
     * Ưu tiên tra cứu từ cơ sở dữ liệu (PostgreSQL); nếu chưa có hoặc không hợp lệ,
     * tự động fallback về giá trị cấu hình mặc định trong file cấu hình ứng dụng.
     * </p>
     *
     * @return số phút cho phép hoàn tác hủy đơn hàng
     */
    int getUndoCancelWindowMinutes();

    /**
     * Lấy giá trị chuỗi của một tham số cấu hình theo khóa.
     *
     * @param settingKey khóa tham số
     * @return Optional chứa giá trị cấu hình nếu tìm thấy
     */
    Optional<String> getSettingValue(String settingKey);

    /**
     * Liệt kê toàn bộ tham số cấu hình chưa bị xóa mềm, sắp xếp theo khóa.
     *
     * @return danh sách tham số cấu hình
     */
    List<SettingResponse> listSettings();

    /**
     * Cập nhật giá trị một tham số cấu hình đã tồn tại và ghi nhật ký hệ thống
     * ({@code SYSTEM_SETTING_UPDATED}) cùng giao dịch.
     * <p>
     * Không tạo khóa mới qua API này. Giá trị được kiểm tra theo {@code dataType} của tham số;
     * các khóa cửa sổ thời gian (hủy / hoàn tác hủy) bắt buộc là số nguyên dương.
     * </p>
     *
     * @param actorId    ID quản trị viên thực hiện
     * @param settingKey khóa tham số cần cập nhật
     * @param value      giá trị mới dạng chuỗi
     * @return tham số sau khi cập nhật
     * @throws com.hmp.sales.common.exception.ResourceNotFoundException nếu khóa không tồn tại
     * @throws com.hmp.sales.common.exception.BusinessRuleException     nếu giá trị không hợp lệ với kiểu dữ liệu
     */
    SettingResponse updateSetting(Long actorId, String settingKey, String value);
}

