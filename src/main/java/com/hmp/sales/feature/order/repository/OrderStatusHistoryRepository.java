package com.hmp.sales.feature.order.repository;

import com.hmp.sales.feature.order.entity.OrderStatusHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Repository truy xuất dữ liệu Lịch sử trạng thái đơn hàng (OrderStatusHistory).
 */
@Repository
public interface OrderStatusHistoryRepository extends JpaRepository<OrderStatusHistory, Long> {

    /**
     * Lấy toàn bộ lịch sử chuyển trạng thái của một đơn hàng, sắp xếp theo thời gian tăng dần.
     *
     * @param orderId ID đơn hàng
     * @return danh sách lịch sử chuyển trạng thái
     */
    List<OrderStatusHistory> findByOrderIdOrderByCreatedAtAsc(Long orderId);
}

