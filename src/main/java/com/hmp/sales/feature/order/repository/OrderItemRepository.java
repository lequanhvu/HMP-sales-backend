package com.hmp.sales.feature.order.repository;

import com.hmp.sales.feature.order.entity.OrderItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Repository truy xuất dữ liệu Chi tiết dòng sản phẩm đơn hàng (OrderItem).
 */
@Repository
public interface OrderItemRepository extends JpaRepository<OrderItem, Long> {

    /**
     * Lấy danh sách các dòng sản phẩm của một đơn hàng chưa bị xóa mềm.
     *
     * @param orderId ID đơn hàng
     * @return danh sách các dòng sản phẩm
     */
    List<OrderItem> findByOrderIdAndIsDeletedFalse(Long orderId);
}

