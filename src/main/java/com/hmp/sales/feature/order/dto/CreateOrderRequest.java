package com.hmp.sales.feature.order.dto;

import com.hmp.sales.feature.order.entity.DeliveryMethod;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.util.List;

/**
 * Record chứa thông tin yêu cầu tạo đơn hàng mới từ khách hàng.
 *
 * @param receiverName   họ và tên người nhận hàng
 * @param receiverPhone  số điện thoại người nhận hàng (đã xác thực OTP)
 * @param addressId      mã ID địa chỉ nhận hàng đã lưu trong sổ địa chỉ của khách hàng
 * @param note           ghi chú đơn hàng của khách hàng
 * @param items          danh sách các sản phẩm đặt mua
 * @param deliveryMethod hình thức nhận hàng (SHIPPING: giao tận nơi, PICKUP: tự bốc hàng tại xưởng)
 */
public record CreateOrderRequest(
        @NotBlank(message = "Tên người nhận không được để trống")
        @Size(max = 150, message = "Tên người nhận không được vượt quá 150 ký tự")
        String receiverName,

        @NotBlank(message = "Số điện thoại người nhận không được để trống")
        @Pattern(regexp = "^(0|\\+84)[0-9]{9,10}$", message = "Số điện thoại không đúng định dạng")
        String receiverPhone,

        @NotNull(message = "Vui lòng chọn địa chỉ nhận hàng")
        @Positive(message = "Mã địa chỉ nhận hàng không hợp lệ")
        Long addressId,

        @Size(max = 1000, message = "Ghi chú không được vượt quá 1000 ký tự")
        String note,

        @NotEmpty(message = "Đơn hàng phải chứa ít nhất một sản phẩm")
        List<@Valid OrderItemRequest> items,

        DeliveryMethod deliveryMethod
) {
    public CreateOrderRequest {
        if (deliveryMethod == null) {
            deliveryMethod = DeliveryMethod.SHIPPING;
        }
    }

    /**
     * Constructor tương thích ngược không có deliveryMethod (mặc định SHIPPING).
     */
    public CreateOrderRequest(String receiverName, String receiverPhone, Long addressId, String note, List<@Valid OrderItemRequest> items) {
        this(receiverName, receiverPhone, addressId, note, items, DeliveryMethod.SHIPPING);
    }
}

