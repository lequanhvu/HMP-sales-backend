package com.hmp.sales.feature.systemlog.service;

import com.hmp.sales.feature.systemlog.dto.SystemLogDetailResponse;
import com.hmp.sales.feature.systemlog.dto.SystemLogSearchRequest;
import com.hmp.sales.feature.systemlog.dto.SystemLogSummaryResponse;
import org.springframework.data.domain.Page;

/**
 * Service ghi và tra cứu nhật ký nghiệp vụ quan trọng trong bảng {@code system_logs}.
 * <p>
 * Được sử dụng để audit trail các hành động quản trị nhạy cảm như:
 * thay đổi quyền hạn vai trò, khóa/mở khóa tài khoản, tạo tài khoản Manager,
 * thay đổi cài đặt hệ thống, thay đổi danh mục/sản phẩm và xử lý đơn hàng của nhân viên.
 *
 * <p><strong>Giao dịch</strong>: {@link #log} và {@link #logWithChange} tham gia cùng giao dịch
 * với thao tác nghiệp vụ đang chạy (commit/rollback cùng nhau), nên chỉ gọi sau khi thao tác
 * nghiệp vụ đã hợp lệ và đã lưu. {@link #logSecurityEvent} dùng giao dịch riêng cho các sự kiện
 * không nằm trong giao dịch nghiệp vụ.
 */
public interface SystemLogService {

    /**
     * Ghi nhật ký một hành động quản trị đơn giản, cùng giao dịch với thao tác nghiệp vụ.
     *
     * @param actorId   ID người dùng thực hiện hành động (null nếu là hệ thống)
     * @param action    mã hành động (ví dụ: "USER_LOCKED", "ROLE_PERMISSION_UPDATED")
     * @param entityType loại thực thể bị ảnh hưởng (ví dụ: "USER", "ROLE")
     * @param entityId  ID thực thể bị ảnh hưởng
     * @param description mô tả bằng tiếng Việt về hành động
     */
    void log(Long actorId, String action, String entityType, Long entityId, String description);

    /**
     * Ghi nhật ký hành động có trước/sau state để audit trail đầy đủ, cùng giao dịch với thao tác nghiệp vụ.
     *
     * @param actorId   ID người dùng thực hiện hành động
     * @param action    mã hành động
     * @param entityType loại thực thể bị ảnh hưởng
     * @param entityId  ID thực thể bị ảnh hưởng
     * @param description mô tả hành động
     * @param oldValue  giá trị trước khi thay đổi (JSON text hoặc mô tả ngắn)
     * @param newValue  giá trị sau khi thay đổi (JSON text hoặc mô tả ngắn)
     */
    void logWithChange(Long actorId, String action, String entityType, Long entityId,
                       String description, String oldValue, String newValue);

    /**
     * Ghi nhật ký sự kiện bảo mật trong một giao dịch riêng ({@code REQUIRES_NEW}).
     * <p>
     * Dùng cho các sự kiện không nằm trong giao dịch nghiệp vụ hoặc giao dịch gọi có thể rollback
     * (đăng nhập bị chặn, phát hiện tái sử dụng refresh token). Lỗi ghi nhật ký chỉ được cảnh báo
     * trong log kỹ thuật và không làm ảnh hưởng luồng gọi.
     *
     * @param actorId     ID người dùng liên quan (null nếu không xác định)
     * @param action      mã hành động
     * @param entityType  loại thực thể liên quan
     * @param entityId    ID thực thể liên quan
     * @param description mô tả bằng tiếng Việt
     */
    void logSecurityEvent(Long actorId, String action, String entityType, Long entityId, String description);

    /**
     * Tìm kiếm nhật ký hệ thống theo bộ lọc, sắp xếp mới nhất lên đầu.
     *
     * @param filter bộ lọc tìm kiếm (có thể null)
     * @param page   chỉ số trang (0-indexed)
     * @param size   số bản ghi mỗi trang
     * @return trang kết quả dạng rút gọn
     * @throws com.hmp.sales.common.exception.BusinessRuleException nếu khoảng ngày hoặc vai trò không hợp lệ
     */
    Page<SystemLogSummaryResponse> search(SystemLogSearchRequest filter, int page, int size);

    /**
     * Lấy chi tiết một bản ghi nhật ký.
     *
     * @param logId ID bản ghi nhật ký
     * @return chi tiết bản ghi
     * @throws com.hmp.sales.common.exception.ResourceNotFoundException nếu không tồn tại
     */
    SystemLogDetailResponse getById(Long logId);
}
