package com.hmp.sales.feature.order.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.util.List;

/**
 * Record chứa thông tin yêu cầu tạo đơn hàng mới từ khách hàng.
 *
 * @param receiverName    họ và tên người nhận hàng
 * @param receiverPhone   số điện thoại người nhận hàng (đã xác thực OTP)
 * @param shippingAddress địa chỉ giao nhận hàng chi tiết
 * @param note            ghi chú đơn hàng của khách hàng
 * @param items           danh sách các sản phẩm đặt mua
 */
public record CreateOrderRequest(
        @NotBlank(message = "Tên người nhận không được để trống")
        @Size(max = 150, message = "Tên người nhận không được vượt quá 150 ký tự")
        String receiverName,

        @NotBlank(message = "Số điện thoại người nhận không được để trống")
        @Pattern(regexp = "^(0|\\+84)[0-9]{9,10}$", message = "Số điện thoại không đúng định dạng")
        String receiverPhone,

        @NotBlank(message = "Địa chỉ nhận hàng không được để trống")
        @Size(max = 1000, message = "Địa chỉ nhận hàng không được vượt quá 1000 ký tự")
        String shippingAddress,

        @Size(max = 1000, message = "Ghi chú không được vượt quá 1000 ký tự")
        String note,

        @NotEmpty(message = "Đơn hàng phải chứa ít nhất một sản phẩm")
        List<@Valid OrderItemRequest> items
) {}

