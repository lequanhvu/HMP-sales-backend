package com.hmp.sales.feature.user.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * DTO yêu cầu thêm mới hoặc cập nhật địa chỉ giao nhận hàng của khách hàng (AddressRequest).
 *
 * @param address   địa chỉ chi tiết nhận hàng (bắt buộc, tối đa 500 ký tự)
 * @param isDefault cờ thiết lập làm địa chỉ mặc định (tùy chọn)
 */
public record AddressRequest(
        @NotBlank(message = "Địa chỉ nhận hàng không được để trống")
        @Size(max = 500, message = "Địa chỉ nhận hàng tối đa 500 ký tự")
        String address,

        Boolean isDefault
) {
}
