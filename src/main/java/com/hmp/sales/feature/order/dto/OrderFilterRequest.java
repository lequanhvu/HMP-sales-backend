package com.hmp.sales.feature.order.dto;

import com.hmp.sales.feature.order.entity.OrderStatus;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;

/**
 * Record chứa các tiêu chí lọc danh sách đơn hàng của khách hàng.
 *
 * @param dateFrom ngày bắt đầu (YYYY-MM-DD)
 * @param dateTo   ngày kết thúc (YYYY-MM-DD)
 * @param status   trạng thái đơn hàng cần lọc
 * @param page     chỉ số trang
 * @param size     kích thước trang
 */
public record OrderFilterRequest(
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
        LocalDate dateFrom,

        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
        LocalDate dateTo,

        OrderStatus status,

        Integer page,

        Integer size
) {}

