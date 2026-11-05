package com.hmp.sales.feature.order.scheduler;

import com.hmp.sales.feature.order.entity.Order;
import com.hmp.sales.feature.order.entity.OrderStatus;
import com.hmp.sales.feature.order.entity.OrderStatusHistory;
import com.hmp.sales.feature.order.repository.OrderRepository;
import com.hmp.sales.feature.order.repository.OrderStatusHistoryRepository;
import com.hmp.sales.feature.setting.service.SystemSettingService;
import com.hmp.sales.feature.user.entity.User;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * Unit test cho {@link OrderAutoTransitionScheduler}.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("OrderAutoTransitionScheduler Unit Tests")
class OrderAutoTransitionSchedulerTest {

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private OrderStatusHistoryRepository orderStatusHistoryRepository;

    @Mock
    private SystemSettingService systemSettingService;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    @InjectMocks
    private OrderAutoTransitionScheduler scheduler;

    @Test
    @DisplayName("Tự động chuyển các đơn CONFIRMED hết hạn hủy sang PROCESSING và ghi vết lịch sử")
    void autoTransitionConfirmedOrders_expiredOrders_transitionedToProcessing() {
        when(systemSettingService.getCancellationWindowMinutes()).thenReturn(30);

        User customer = User.builder().id(1L).fullName("Khách hàng").build();

        Order order = Order.builder()
                .id(100L)
                .customer(customer)
                .status(OrderStatus.CONFIRMED)
                .confirmedAt(Instant.now().minus(35, ChronoUnit.MINUTES))
                .isDeleted(false)
                .build();

        when(orderRepository.findByStatusAndConfirmedAtBeforeAndIsDeletedFalse(eq(OrderStatus.CONFIRMED), any(Instant.class)))
                .thenReturn(List.of(order));

        scheduler.autoTransitionConfirmedOrders();

        assertThat(order.getStatus()).isEqualTo(OrderStatus.PROCESSING);
        verify(orderRepository).save(order);
        verify(orderStatusHistoryRepository).save(any(OrderStatusHistory.class));
    }

    @Test
    @DisplayName("Không thực hiện cập nhật gì khi không có đơn hàng CONFIRMED nào hết hạn")
    void autoTransitionConfirmedOrders_noExpiredOrders_nothingUpdated() {
        when(systemSettingService.getCancellationWindowMinutes()).thenReturn(30);
        when(orderRepository.findByStatusAndConfirmedAtBeforeAndIsDeletedFalse(eq(OrderStatus.CONFIRMED), any(Instant.class)))
                .thenReturn(Collections.emptyList());

        scheduler.autoTransitionConfirmedOrders();

        verify(orderRepository, never()).save(any(Order.class));
        verify(orderStatusHistoryRepository, never()).save(any(OrderStatusHistory.class));
    }
}

