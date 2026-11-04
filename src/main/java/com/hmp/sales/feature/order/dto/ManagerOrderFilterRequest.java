package com.hmp.sales.feature.order.dto;

import com.hmp.sales.feature.order.entity.OrderStatus;

import java.time.LocalDate;

/**
 * Request DTO chứa các tiêu chí lọc danh sách đơn hàng dành cho Quản lý.
 *
 * @param dateFrom      lọc đơn hàng tạo từ ngày (tùy chọn)
 * @param dateTo        lọc đơn hàng tạo đến ngày (tùy chọn)
 * @param customerName  tìm kiếm theo tên khách hàng (hỗ trợ có dấu và không dấu)
 * @param customerPhone tìm kiếm theo số điện thoại khách hàng hoặc người nhận
 * @param customerEmail tìm kiếm theo email khách hàng
 * @param status        lọc theo trạng thái đơn hàng (tùy chọn)
 */
public record ManagerOrderFilterRequest(
        LocalDate dateFrom,
        LocalDate dateTo,
        String customerName,
        String customerPhone,
        String customerEmail,
        OrderStatus status
) {
}

