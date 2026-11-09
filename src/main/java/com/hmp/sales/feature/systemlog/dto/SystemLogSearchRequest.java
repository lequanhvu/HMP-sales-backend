package com.hmp.sales.feature.systemlog.dto;

import java.time.LocalDate;

/**
 * Bộ lọc tìm kiếm nhật ký hệ thống. Mọi trường đều tùy chọn; các trường {@code role},
 * {@code name}, {@code phone}, {@code email} áp dụng lên người thực hiện hành động.
 *
 * @param role     mã vai trò của người thực hiện ({@code ADMIN}, {@code MANAGER}, {@code CUSTOMER})
 * @param activity mã hành động chính xác (ví dụ {@code PRODUCT_UPDATED})
 * @param fromDate ngày bắt đầu (tính từ 00:00 UTC)
 * @param toDate   ngày kết thúc (tính đến hết ngày, UTC)
 * @param name     tên người thực hiện, tìm tiếng Việt không dấu
 * @param phone    số điện thoại người thực hiện (khớp một phần)
 * @param email    email người thực hiện (khớp một phần, không phân biệt hoa thường)
 */
public record SystemLogSearchRequest(
        String role,
        String activity,
        LocalDate fromDate,
        LocalDate toDate,
        String name,
        String phone,
        String email
) {
}

