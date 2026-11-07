package com.hmp.sales.feature.notification.service;

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
import com.hmp.sales.feature.notification.service.impl.NotificationServiceImpl;
import com.hmp.sales.feature.order.entity.OrderStatus;
import com.hmp.sales.feature.user.entity.User;
import com.hmp.sales.feature.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * Bộ kiểm thử đơn vị cho {@link NotificationServiceImpl}.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("NotificationServiceImpl Unit Tests")
class NotificationServiceImplTest {

    @Mock
    private NotificationRepository notificationRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private ExternalNotificationProvider externalNotificationProvider;

    private NotificationServiceImpl notificationService;

    private User sampleUser;

    @BeforeEach
    void setUp() {
        notificationService = new NotificationServiceImpl(
                notificationRepository,
                userRepository,
                List.of(externalNotificationProvider)
        );

        sampleUser = User.builder()
                .id(1L)
                .fullName("Khách hàng A")
                .email("customer@example.com")
                .build();
    }

    @Test
    @DisplayName("Lấy danh sách thông báo của user thành công")
    void getOwnNotifications_returnsPaginatedList() {
        Notification notification = new Notification(sampleUser, NotificationType.ORDER_CONFIRMED,
                "Tiêu đề", "Nội dung", "ORDER", 100L);
        notification.setNotificationId(10L);
        notification.setCreatedAt(Instant.now());

        Pageable pageable = PageRequest.of(0, 10);
        Page<Notification> entityPage = new PageImpl<>(List.of(notification), pageable, 1);

        when(notificationRepository.findByUser_IdAndIsDeletedFalseOrderByCreatedAtDesc(1L, pageable))
                .thenReturn(entityPage);

        PageResponse<NotificationResponse> result = notificationService.getOwnNotifications(1L, pageable);

        assertThat(result.result()).hasSize(1);
        assertThat(result.result().getFirst().notificationId()).isEqualTo(10L);
        assertThat(result.result().getFirst().type()).isEqualTo(NotificationType.ORDER_CONFIRMED);
        assertThat(result.meta().totalElements()).isEqualTo(1L);
    }

    @Test
    @DisplayName("Đếm số lượng thông báo chưa đọc chính xác")
    void countUnread_returnsCorrectCount() {
        when(notificationRepository.countByUser_IdAndIsReadFalseAndIsDeletedFalse(1L)).thenReturn(5L);

        UnreadCountResponse response = notificationService.countUnread(1L);

        assertThat(response.unreadCount()).isEqualTo(5L);
        verify(notificationRepository).countByUser_IdAndIsReadFalseAndIsDeletedFalse(1L);
    }

    @Test
    @DisplayName("Đánh dấu một thông báo đã đọc thành công")
    void markAsRead_success() {
        Notification notification = new Notification(sampleUser, NotificationType.ORDER_SHIPPING,
                "Đang giao", "Đang giao", "ORDER", 100L);
        notification.setNotificationId(10L);
        notification.setRead(false);

        when(notificationRepository.findByNotificationIdAndUser_IdAndIsDeletedFalse(10L, 1L))
                .thenReturn(Optional.of(notification));
        when(notificationRepository.save(any(Notification.class))).thenAnswer(invocation -> invocation.getArgument(0));

        NotificationResponse response = notificationService.markAsRead(1L, 10L);

        assertThat(response.isRead()).isTrue();
        assertThat(response.readAt()).isNotNull();
        verify(notificationRepository).save(notification);
    }

    @Test
    @DisplayName("Đánh dấu thông báo đã đọc throw 404 khi không tồn tại hoặc không thuộc quyền sở hữu")
    void markAsRead_notFound_throwsResourceNotFoundException() {
        when(notificationRepository.findByNotificationIdAndUser_IdAndIsDeletedFalse(999L, 1L))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> notificationService.markAsRead(1L, 999L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Không tìm thấy");
    }

    @Test
    @DisplayName("Đánh dấu toàn bộ thông báo đã đọc thành công")
    void markAllAsRead_success() {
        when(notificationRepository.markAllAsRead(eq(1L), any(Instant.class))).thenReturn(3);

        MarkAllReadResponse response = notificationService.markAllAsRead(1L);

        assertThat(response.updatedCount()).isEqualTo(3);
        verify(notificationRepository).markAllAsRead(eq(1L), any(Instant.class));
    }

    @Test
    @DisplayName("Xử lý sự kiện ORDER_CONFIRMED tạo thông báo thành công")
    void onOrderStatusChanged_confirmed_createsNotification() {
        OrderStatusChangedEvent event = new OrderStatusChangedEvent(100L, 1L, OrderStatus.PENDING, OrderStatus.CONFIRMED, 2L);
        when(userRepository.findById(1L)).thenReturn(Optional.of(sampleUser));

        notificationService.onOrderStatusChanged(event);

        ArgumentCaptor<Notification> captor = ArgumentCaptor.forClass(Notification.class);
        verify(notificationRepository).save(captor.capture());

        Notification saved = captor.getValue();
        assertThat(saved.getUser().getId()).isEqualTo(1L);
        assertThat(saved.getType()).isEqualTo(NotificationType.ORDER_CONFIRMED);
        assertThat(saved.getRelatedEntityId()).isEqualTo(100L);
        assertThat(saved.getTitle()).isEqualTo("Đơn hàng đã được xác nhận");
        assertThat(saved.getMessage()).contains("#100");

        verify(externalNotificationProvider).send(eq(1L), eq(saved.getTitle()), eq(saved.getMessage()), eq(NotificationType.ORDER_CONFIRMED));
    }

    @Test
    @DisplayName("Xử lý sự kiện ORDER_PROCESSING tạo thông báo thành công")
    void onOrderStatusChanged_processing_createsNotification() {
        OrderStatusChangedEvent event = new OrderStatusChangedEvent(101L, 1L, OrderStatus.CONFIRMED, OrderStatus.PROCESSING, null);
        when(userRepository.findById(1L)).thenReturn(Optional.of(sampleUser));

        notificationService.onOrderStatusChanged(event);

        ArgumentCaptor<Notification> captor = ArgumentCaptor.forClass(Notification.class);
        verify(notificationRepository).save(captor.capture());

        Notification saved = captor.getValue();
        assertThat(saved.getType()).isEqualTo(NotificationType.ORDER_PROCESSING);
        assertThat(saved.getTitle()).isEqualTo("Đơn hàng đang được chuẩn bị");
    }

    @Test
    @DisplayName("Xử lý sự kiện ORDER_SHIPPING tạo thông báo thành công")
    void onOrderStatusChanged_shipping_createsNotification() {
        OrderStatusChangedEvent event = new OrderStatusChangedEvent(102L, 1L, OrderStatus.PROCESSING, OrderStatus.SHIPPING, 2L);
        when(userRepository.findById(1L)).thenReturn(Optional.of(sampleUser));

        notificationService.onOrderStatusChanged(event);

        ArgumentCaptor<Notification> captor = ArgumentCaptor.forClass(Notification.class);
        verify(notificationRepository).save(captor.capture());

        Notification saved = captor.getValue();
        assertThat(saved.getType()).isEqualTo(NotificationType.ORDER_SHIPPING);
        assertThat(saved.getTitle()).isEqualTo("Đơn hàng đang được giao");
    }

    @Test
    @DisplayName("Xử lý sự kiện ORDER_COMPLETED tạo thông báo thành công")
    void onOrderStatusChanged_completed_createsNotification() {
        OrderStatusChangedEvent event = new OrderStatusChangedEvent(103L, 1L, OrderStatus.SHIPPING, OrderStatus.COMPLETED, 2L);
        when(userRepository.findById(1L)).thenReturn(Optional.of(sampleUser));

        notificationService.onOrderStatusChanged(event);

        ArgumentCaptor<Notification> captor = ArgumentCaptor.forClass(Notification.class);
        verify(notificationRepository).save(captor.capture());

        Notification saved = captor.getValue();
        assertThat(saved.getType()).isEqualTo(NotificationType.ORDER_COMPLETED);
        assertThat(saved.getTitle()).isEqualTo("Đơn hàng đã hoàn thành");
    }

    @Test
    @DisplayName("Xử lý sự kiện ORDER_CANCELLED tạo thông báo thành công")
    void onOrderStatusChanged_cancelled_createsNotification() {
        OrderStatusChangedEvent event = new OrderStatusChangedEvent(104L, 1L, OrderStatus.PENDING, OrderStatus.CANCELLED, 1L);
        when(userRepository.findById(1L)).thenReturn(Optional.of(sampleUser));

        notificationService.onOrderStatusChanged(event);

        ArgumentCaptor<Notification> captor = ArgumentCaptor.forClass(Notification.class);
        verify(notificationRepository).save(captor.capture());

        Notification saved = captor.getValue();
        assertThat(saved.getType()).isEqualTo(NotificationType.ORDER_CANCELLED);
        assertThat(saved.getTitle()).isEqualTo("Đơn hàng đã bị hủy");
    }

    @Test
    @DisplayName("Xử lý sự kiện tạo đơn mới (PENDING) tạo thông báo ORDER_CREATED thành công")
    void onOrderStatusChanged_pending_createsOrderCreatedNotification() {
        OrderStatusChangedEvent event = new OrderStatusChangedEvent(105L, 1L, null, OrderStatus.PENDING, 1L);
        when(userRepository.findById(1L)).thenReturn(Optional.of(sampleUser));

        notificationService.onOrderStatusChanged(event);

        ArgumentCaptor<Notification> captor = ArgumentCaptor.forClass(Notification.class);
        verify(notificationRepository).save(captor.capture());

        Notification saved = captor.getValue();
        assertThat(saved.getType()).isEqualTo(NotificationType.ORDER_CREATED);
        assertThat(saved.getTitle()).isEqualTo("Đặt hàng thành công");
        assertThat(saved.getMessage()).contains("#105");

        verify(externalNotificationProvider).send(eq(1L), eq(saved.getTitle()), eq(saved.getMessage()), eq(NotificationType.ORDER_CREATED));
    }

    @Test
    @DisplayName("Xử lý sự kiện với trạng thái null sẽ bỏ qua")
    void onOrderStatusChanged_nullStatus_ignored() {
        OrderStatusChangedEvent event = new OrderStatusChangedEvent(107L, 1L, null, null, 1L);

        notificationService.onOrderStatusChanged(event);

        verify(notificationRepository, never()).save(any());
        verify(externalNotificationProvider, never()).send(any(), any(), any(), any());
    }

    @Test
    @DisplayName("Khi nhà cung cấp ngoại vi gặp lỗi, hệ thống ghi log và không ném ngoại lệ làm gián đoạn")
    void onOrderStatusChanged_externalProviderFails_doesNotThrow() {
        OrderStatusChangedEvent event = new OrderStatusChangedEvent(106L, 1L, OrderStatus.PENDING, OrderStatus.CONFIRMED, 2L);
        when(userRepository.findById(1L)).thenReturn(Optional.of(sampleUser));
        doThrow(new RuntimeException("Mạng ngoài bị lỗi")).when(externalNotificationProvider)
                .send(any(), any(), any(), any());

        // Không được ném ngoại lệ ra ngoài
        notificationService.onOrderStatusChanged(event);

        verify(notificationRepository).save(any(Notification.class));
    }
}
