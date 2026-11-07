package com.hmp.sales.feature.notification.service.impl;

import com.hmp.sales.common.exception.ResourceNotFoundException;
import com.hmp.sales.common.response.PageResponse;
import com.hmp.sales.feature.notification.dto.MarkAllReadResponse;
import com.hmp.sales.feature.notification.dto.NotificationResponse;
import com.hmp.sales.feature.notification.dto.UnreadCountResponse;
import com.hmp.sales.feature.notification.entity.Notification;
import com.hmp.sales.feature.notification.entity.NotificationType;
import com.hmp.sales.feature.notification.event.OrderStatusChangedEvent;
import com.hmp.sales.feature.notification.external.ExternalNotificationProvider;
import com.hmp.sales.feature.notification.repository.NotificationRepository;
import com.hmp.sales.feature.notification.service.NotificationService;
import com.hmp.sales.feature.order.entity.OrderStatus;
import com.hmp.sales.feature.user.entity.User;
import com.hmp.sales.feature.user.repository.UserRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.time.Instant;
import java.util.List;

/**
 * Triển khai nghiệp vụ của {@link NotificationService}.
 * Quản lý tra cứu, cập nhật trạng thái đọc của thông báo và lắng nghe sự kiện đơn hàng để phân phát thông báo.
 */
@Service
@Slf4j
public class NotificationServiceImpl implements NotificationService {

    private final NotificationRepository notificationRepository;
    private final UserRepository userRepository;
    private final List<ExternalNotificationProvider> externalProviders;

    /**
     * Khởi tạo NotificationServiceImpl với các dependencies cần thiết.
     *
     * @param notificationRepository repository quản lý Notification
     * @param userRepository         repository quản lý User
     * @param externalProviders      danh sách các nhà cung cấp thông báo ngoại vi
     */
    public NotificationServiceImpl(
            NotificationRepository notificationRepository,
            UserRepository userRepository,
            List<ExternalNotificationProvider> externalProviders
    ) {
        this.notificationRepository = notificationRepository;
        this.userRepository = userRepository;
        this.externalProviders = externalProviders;
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<NotificationResponse> getOwnNotifications(Long userId, Pageable pageable) {
        Page<Notification> page = notificationRepository
                .findByUser_IdAndIsDeletedFalseOrderByCreatedAtDesc(userId, pageable);
        Page<NotificationResponse> dtoPage = page.map(NotificationResponse::fromEntity);
        return PageResponse.from(dtoPage);
    }

    @Override
    @Transactional(readOnly = true)
    public UnreadCountResponse countUnread(Long userId) {
        long count = notificationRepository.countByUser_IdAndIsReadFalseAndIsDeletedFalse(userId);
        return new UnreadCountResponse(count);
    }

    @Override
    @Transactional
    public NotificationResponse markAsRead(Long userId, Long notificationId) {
        Notification notification = notificationRepository
                .findByNotificationIdAndUser_IdAndIsDeletedFalse(notificationId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Thông báo", notificationId));

        if (!notification.isRead()) {
            notification.setRead(true);
            notification.setReadAt(Instant.now());
            notification = notificationRepository.save(notification);
        }

        return NotificationResponse.fromEntity(notification);
    }

    @Override
    @Transactional
    public MarkAllReadResponse markAllAsRead(Long userId) {
        int updated = notificationRepository.markAllAsRead(userId, Instant.now());
        return new MarkAllReadResponse(updated);
    }

    @Override
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void onOrderStatusChanged(OrderStatusChangedEvent event) {
        try {
            NotificationType type = resolveNotificationType(event.toStatus());
            if (type == null) {
                log.debug("Bỏ qua sự kiện đổi trạng thái đơn hàng không nằm trong danh mục thông báo: {}", event);
                return;
            }

            User customer = userRepository.findById(event.customerId()).orElse(null);
            if (customer == null) {
                log.warn("Không tìm thấy khách hàng ID={} để gửi thông báo đơn hàng ID={}",
                        event.customerId(), event.orderId());
                return;
            }

            String title = resolveTitle(type);
            String message = resolveMessage(type, event.orderId());

            Notification notification = new Notification(
                    customer,
                    type,
                    title,
                    message,
                    "ORDER",
                    event.orderId()
            );

            notificationRepository.save(notification);
            log.info("Đã tạo thông báo in-app thành công cho khách hàng ID={}, orderId={}, type={}",
                    customer.getId(), event.orderId(), type);

            dispatchExternal(customer.getId(), title, message, type);
        } catch (Exception e) {
            log.error("Lỗi khi xử lý tạo thông báo cho sự kiện OrderStatusChangedEvent: {}", event, e);
        }
    }

    /**
     * Xác định loại thông báo {@link NotificationType} tương ứng với trạng thái mới của đơn hàng.
     *
     * @param status trạng thái mới của đơn hàng
     * @return NotificationType tương ứng, hoặc null nếu không cấu hình thông báo
     */
    private NotificationType resolveNotificationType(OrderStatus status) {
        return switch (status) {
            case PENDING -> NotificationType.ORDER_CREATED;
            case CONFIRMED -> NotificationType.ORDER_CONFIRMED;
            case PROCESSING -> NotificationType.ORDER_PROCESSING;
            case SHIPPING -> NotificationType.ORDER_SHIPPING;
            case COMPLETED -> NotificationType.ORDER_COMPLETED;
            case CANCELLED -> NotificationType.ORDER_CANCELLED;
            default -> null;
        };
    }

    /**
     * Xác định tiêu đề thông báo theo loại thông báo.
     */
    private String resolveTitle(NotificationType type) {
        return switch (type) {
            case ORDER_CREATED -> "Đặt hàng thành công";
            case ORDER_CONFIRMED -> "Đơn hàng đã được xác nhận";
            case ORDER_PROCESSING -> "Đơn hàng đang được chuẩn bị";
            case ORDER_SHIPPING -> "Đơn hàng đang được giao";
            case ORDER_COMPLETED -> "Đơn hàng đã hoàn thành";
            case ORDER_CANCELLED -> "Đơn hàng đã bị hủy";
        };
    }

    /**
     * Tạo nội dung thông báo chuẩn hóa theo loại thông báo và mã đơn hàng.
     */
    private String resolveMessage(NotificationType type, Long orderId) {
        return switch (type) {
            case ORDER_CREATED -> String.format(
                    "Đơn hàng #%d đã được tạo thành công và đang chờ Quản lý xác nhận giá.",
                    orderId);
            case ORDER_CONFIRMED -> String.format(
                    "Đơn hàng #%d đã được Quản lý xác nhận giá. Vào đây để xem chi tiết và hủy trong vòng 30 phút nếu không đồng ý.",
                    orderId);
            case ORDER_PROCESSING -> String.format(
                    "Đơn hàng #%d đang được chuẩn bị. Chúng tôi sẽ thông báo khi hàng sẵn sàng giao.",
                    orderId);
            case ORDER_SHIPPING -> String.format(
                    "Đơn hàng #%d đang trên đường giao đến bạn.",
                    orderId);
            case ORDER_COMPLETED -> String.format(
                    "Đơn hàng #%d đã được giao thành công. Cảm ơn bạn đã mua hàng!",
                    orderId);
            case ORDER_CANCELLED -> String.format(
                    "Đơn hàng #%d đã bị hủy.",
                    orderId);
        };
    }

    /**
     * Điều phối gửi thông báo qua các nhà cung cấp bên ngoài (Zalo, Email, SMS).
     */
    private void dispatchExternal(Long userId, String title, String message, NotificationType type) {
        if (externalProviders == null || externalProviders.isEmpty()) {
            return;
        }
        for (ExternalNotificationProvider provider : externalProviders) {
            try {
                provider.send(userId, title, message, type);
            } catch (Exception ex) {
                log.error("Nhà cung cấp thông báo ngoại vi {} thất bại cho userId={}",
                        provider.getClass().getSimpleName(), userId, ex);
            }
        }
    }
}
