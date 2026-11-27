package com.hmp.sales.feature.delivery.dto;

import java.math.BigDecimal;

/**
 * DTO phản hồi thông tin vị trí và bán kính phục vụ của xưởng cho bản đồ Frontend.
 *
 * @param storeName      tên xưởng sản xuất
 * @param storeAddress   địa chỉ xưởng sản xuất
 * @param storeLatitude  vĩ độ tâm xưởng
 * @param storeLongitude kinh độ tâm xưởng
 * @param maxRadiusKm    bán kính giao hàng tối đa cho phép (km)
 * @param storeHotline   số điện thoại hotline liên hệ
 */
public record StoreInfoResponse(
        String storeName,
        String storeAddress,
        BigDecimal storeLatitude,
        BigDecimal storeLongitude,
        BigDecimal maxRadiusKm,
        String storeHotline
) {}
