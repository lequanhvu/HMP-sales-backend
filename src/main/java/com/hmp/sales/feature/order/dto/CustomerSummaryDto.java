package com.hmp.sales.feature.order.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.hmp.sales.feature.user.entity.User;

/**
 * DTO tóm tắt thông tin tài khoản khách hàng liên kết với đơn hàng.
 *
 * @param userId   ID tài khoản khách hàng
 * @param fullName họ và tên khách hàng
 * @param phone    số điện thoại khách hàng
 * @param email    email khách hàng
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record CustomerSummaryDto(
        Long userId,
        String fullName,
        String phone,
        String email
) {

    /**
     * Chuyển đổi từ entity {@link User} sang DTO {@link CustomerSummaryDto}.
     *
     * @param user entity người dùng
     * @return DTO tóm tắt khách hàng
     */
    public static CustomerSummaryDto fromEntity(User user) {
        if (user == null) {
            return null;
        }
        return new CustomerSummaryDto(
                user.getId(),
                user.getFullName(),
                user.getPhone(),
                user.getEmail()
        );
    }
}

