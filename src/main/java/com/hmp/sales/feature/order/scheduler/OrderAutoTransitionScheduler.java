package com.hmp.sales.feature.order.scheduler;

import com.hmp.sales.feature.order.entity.Order;
import com.hmp.sales.feature.order.entity.OrderStatus;
import com.hmp.sales.feature.order.entity.OrderStatusHistory;
import com.hmp.sales.feature.order.repository.OrderRepository;
import com.hmp.sales.feature.order.repository.OrderStatusHistoryRepository;
import com.hmp.sales.feature.setting.service.SystemSettingService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

/**
 * Tác vụ nền định kỳ tự động chuyển trạng thái đơn hàng (OrderAutoTransitionScheduler).
 * <p>
 * Định kỳ quét các đơn hàng đang ở trạng thái {@link OrderStatus#CONFIRMED}
 * đã vượt quá thời hạn hủy đơn (cancellationWindowMinutes) được cấu hình động trong DB,
 * tự động chuyển sang trạng thái {@link OrderStatus#PROCESSING} và ghi vết lịch sử.
 * </p>
 */
@Slf4j
@Component
public class OrderAutoTransitionScheduler {

    private final OrderRepository orderRepository;
    private final OrderStatusHistoryRepository orderStatusHistoryRepository;
    private final SystemSettingService systemSettingService;

    /**
     * Khởi tạo scheduler với các repositories và services liên quan.
     */
    public OrderAutoTransitionScheduler(
            OrderRepository orderRepository,
            OrderStatusHistoryRepository orderStatusHistoryRepository,
            SystemSettingService systemSettingService) {
        this.orderRepository = orderRepository;
        this.orderStatusHistoryRepository = orderStatusHistoryRepository;
        this.systemSettingService = systemSettingService;
    }

    /**
     * Quét và tự động chuyển các đơn hàng CONFIRMED hết hạn hủy sang PROCESSING.
     * Mặc định chạy mỗi 60 giây.
     */
    @Scheduled(fixedDelay = 60000)
    @Transactional
    public void autoTransitionConfirmedOrders() {
        int windowMinutes = systemSettingService.getCancellationWindowMinutes();
        Instant cutoffTime = Instant.now().minus(windowMinutes, ChronoUnit.MINUTES);

        List<Order> expiredOrders = orderRepository.findByStatusAndConfirmedAtBeforeAndIsDeletedFalse(
                OrderStatus.CONFIRMED, cutoffTime);

        if (expiredOrders.isEmpty()) {
            return;
        }

        log.info("[OrderAutoTransition] Tìm thấy {} đơn hàng CONFIRMED đã hết hạn hủy ({} phút), bắt đầu chuyển sang PROCESSING.",
                expiredOrders.size(), windowMinutes);

        for (Order order : expiredOrders) {
            order.setStatus(OrderStatus.PROCESSING);
            orderRepository.save(order);

            OrderStatusHistory history = OrderStatusHistory.builder()
                    .order(order)
                    .fromStatus(OrderStatus.CONFIRMED)
                    .toStatus(OrderStatus.PROCESSING)
                    .changedBy(null) // null biểu thị tác vụ tự động của hệ thống
                    .reason("Tự động chuyển sang PROCESSING sau khi hết thời hạn hủy đơn (" + windowMinutes + " phút)")
                    .createdAt(Instant.now())
                    .build();
            orderStatusHistoryRepository.save(history);

            log.info("[OrderAutoTransition] Đơn hàng ID={} đã tự động chuyển sang PROCESSING.", order.getId());
        }
    }
}

