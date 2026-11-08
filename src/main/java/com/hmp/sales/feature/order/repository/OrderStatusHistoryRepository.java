package com.hmp.sales.feature.order.repository;

import com.hmp.sales.feature.order.entity.OrderStatus;
import com.hmp.sales.feature.order.entity.OrderStatusHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

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

    /**
     * Lấy bản ghi lịch sử chuyển trạng thái gần nhất có toStatus tương ứng của đơn hàng.
     * <p>
     * Dùng để xác định trạng thái trước khi bị hủy khi thực hiện hoàn tác hủy đơn hàng.
     * </p>
     *
     * @param orderId  ID đơn hàng
     * @param toStatus trạng thái đích cần tra cứu (ví dụ CANCELLED)
     * @return bản ghi lịch sử gần nhất nếu có
     */
    Optional<OrderStatusHistory> findTopByOrderIdAndToStatusAndIsDeletedFalseOrderByCreatedAtDesc(
            Long orderId,
            OrderStatus toStatus
    );
}

