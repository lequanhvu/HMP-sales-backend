package com.hmp.sales.feature.user.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

/**
 * DTO yêu cầu thêm mới hoặc cập nhật địa chỉ giao nhận hàng của khách hàng (AddressRequest).
 *
 * @param address   địa chỉ chi tiết nhận hàng (bắt buộc, tối đa 500 ký tự)
 * @param isDefault cờ thiết lập làm địa chỉ mặc định (tùy chọn)
 * @param latitude  vĩ độ ghim trên bản đồ (tùy chọn, [-90, 90])
 * @param longitude kinh độ ghim trên bản đồ (tùy chọn, [-180, 180])
 */
public record AddressRequest(
        @NotBlank(message = "Địa chỉ nhận hàng không được để trống")
        @Size(max = 500, message = "Địa chỉ nhận hàng tối đa 500 ký tự")
        String address,

        @Size(max = 100, message = "Tỉnh/Thành phố tối đa 100 ký tự")
        String province,

        @Size(max = 100, message = "Quận/Huyện tối đa 100 ký tự")
        String district,

        @Size(max = 100, message = "Phường/Xã tối đa 100 ký tự")
        String ward,

        Boolean isDefault,

        @DecimalMin(value = "-90.0", message = "Vĩ độ không được nhỏ hơn -90")
        @DecimalMax(value = "90.0", message = "Vĩ độ không được lớn hơn 90")
        BigDecimal latitude,

        @DecimalMin(value = "-180.0", message = "Kinh độ không được nhỏ hơn -180")
        @DecimalMax(value = "180.0", message = "Kinh độ không được lớn hơn 180")
        BigDecimal longitude
) {
    /**
     * Constructor tương thích ngược cho các bài kiểm thử hoặc client cũ chỉ truyền address và isDefault.
     */
    public AddressRequest(String address, Boolean isDefault) {
        this(address, null, null, null, isDefault, null, null);
    }

    /**
     * Constructor tương thích ngược có tọa độ.
     */
    public AddressRequest(String address, Boolean isDefault, BigDecimal latitude, BigDecimal longitude) {
        this(address, null, null, null, isDefault, latitude, longitude);
    }
}

