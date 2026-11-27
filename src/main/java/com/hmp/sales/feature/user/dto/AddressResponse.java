package com.hmp.sales.feature.user.dto;

import com.hmp.sales.feature.user.entity.AddressStatus;
import com.hmp.sales.feature.user.entity.CustomerAddress;

import java.math.BigDecimal;

/**
 * DTO phản hồi thông tin địa chỉ giao nhận hàng của khách hàng (AddressResponse).
 *
 * @param addressId ID địa chỉ
 * @param address   địa chỉ chi tiết
 * @param isDefault có phải là địa chỉ mặc định không
 * @param status    trạng thái của địa chỉ (ACTIVE, INACTIVE)
 * @param latitude  vĩ độ ghim trên bản đồ
 * @param longitude kinh độ ghim trên bản đồ
 */
public record AddressResponse(
        Long addressId,
        String address,
        String province,
        String district,
        String ward,
        Boolean isDefault,
        String status,
        BigDecimal latitude,
        BigDecimal longitude
) {
    /**
     * Constructor tương thích ngược.
     */
    public AddressResponse(Long addressId, String address, Boolean isDefault, String status) {
        this(addressId, address, null, null, null, isDefault, status, null, null);
    }

    /**
     * Constructor tương thích ngược có tọa độ.
     */
    public AddressResponse(Long addressId, String address, Boolean isDefault, String status, BigDecimal latitude, BigDecimal longitude) {
        this(addressId, address, null, null, null, isDefault, status, latitude, longitude);
    }

    /**
     * Chuyển đổi từ thực thể {@link CustomerAddress} sang DTO {@link AddressResponse}.
     *
     * @param entity thực thể địa chỉ nhận hàng
     * @return đối tượng phản hồi địa chỉ
     */
    public static AddressResponse fromEntity(CustomerAddress entity) {
        if (entity == null) {
            return null;
        }
        return new AddressResponse(
                entity.getId(),
                entity.getAddressLine(),
                entity.getProvince(),
                entity.getDistrict(),
                entity.getWard(),
                Boolean.TRUE.equals(entity.getIsDefault()),
                entity.getStatus() != null ? entity.getStatus().name() : AddressStatus.ACTIVE.name(),
                entity.getLatitude(),
                entity.getLongitude()
        );
    }
}

