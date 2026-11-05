package com.hmp.sales.feature.notification.repository;

import com.hmp.sales.feature.notification.entity.Notification;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.Optional;

/**
 * Repository thao tác dữ liệu với thực thể {@link Notification} trong cơ sở dữ liệu.
 */
public interface NotificationRepository extends JpaRepository<Notification, Long> {

    /**
     * Tìm danh sách thông báo phân trang của một người dùng cụ thể chưa bị xóa mềm,
     * sắp xếp theo thời gian tạo giảm dần (mới nhất lên đầu).
     *
     * @param userId   ID người dùng
     * @param pageable thông tin phân trang
     * @return trang kết quả thông báo
     */
    Page<Notification> findByUser_IdAndIsDeletedFalseOrderByCreatedAtDesc(Long userId, Pageable pageable);

    /**
     * Tìm thông báo theo ID và ID người dùng để bảo đảm quyền sở hữu.
     *
     * @param notificationId ID thông báo
     * @param userId         ID người dùng sở hữu
     * @return Optional chứa thông báo nếu tìm thấy
     */
    Optional<Notification> findByNotificationIdAndUser_IdAndIsDeletedFalse(Long notificationId, Long userId);

    /**
     * Đếm tổng số thông báo chưa đọc của một người dùng.
     *
     * @param userId ID người dùng
     * @return số lượng thông báo chưa đọc
     */
    long countByUser_IdAndIsReadFalseAndIsDeletedFalse(Long userId);

    /**
     * Đánh dấu toàn bộ thông báo chưa đọc của một người dùng thành đã đọc.
     *
     * @param userId ID người dùng
     * @param readAt thời điểm đọc
     * @return số lượng bản ghi đã được cập nhật
     */
    @Modifying
    @Query("UPDATE Notification n SET n.isRead = true, n.readAt = :readAt " +
            "WHERE n.user.id = :userId AND n.isRead = false AND n.isDeleted = false")
    int markAllAsRead(@Param("userId") Long userId, @Param("readAt") Instant readAt);
}

