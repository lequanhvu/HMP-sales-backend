package com.hmp.sales.feature.order.repository;

import com.hmp.sales.feature.order.entity.Order;
import com.hmp.sales.feature.order.entity.OrderStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

/**
 * Repository truy xuất dữ liệu Đơn hàng (Order).
 */
@Repository
public interface OrderRepository extends JpaRepository<Order, Long>, JpaSpecificationExecutor<Order> {

    /**
     * Tìm đơn hàng theo ID và chưa xóa mềm.
     *
     * @param id ID đơn hàng
     * @return Optional chứa đơn hàng
     */
    Optional<Order> findByIdAndIsDeletedFalse(Long id);

    /**
     * Tìm đơn hàng theo ID, thuộc về một khách hàng cụ thể và chưa xóa mềm.
     *
     * @param id         ID đơn hàng
     * @param customerId ID khách hàng
     * @return Optional chứa đơn hàng
     */
    Optional<Order> findByIdAndCustomerIdAndIsDeletedFalse(Long id, Long customerId);

    /**
     * Tìm các đơn hàng có trạng thái cụ thể và thời điểm xác nhận giá trước một mốc thời gian.
     * <p>
     * Dùng cho tác vụ nền (Scheduler) tự động chuyển đơn hàng từ {@code CONFIRMED}
     * sang {@code PROCESSING} khi hết thời hạn hủy.
     * </p>
     *
     * @param status     trạng thái đơn hàng (ví dụ: CONFIRMED)
     * @param cutoffTime mốc thời gian hết hạn (thời điểm hiện tại trừ đi cancellationWindowMinutes)
     * @return danh sách các đơn hàng thỏa mãn điều kiện
     */
    List<Order> findByStatusAndConfirmedAtBeforeAndIsDeletedFalse(OrderStatus status, Instant cutoffTime);
}

