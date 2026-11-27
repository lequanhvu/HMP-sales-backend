package com.hmp.sales.feature.delivery.dto;

import java.math.BigDecimal;

/**
 * DTO phản hồi kết quả kiểm tra khoảng cách và tính khả thi giao hàng từ xưởng.
 *
 * @param storeName      tên đại lý / xưởng sản xuất
 * @param storeAddress   địa chỉ cơ sở của xưởng
 * @param storeLatitude  vĩ độ tâm xưởng
 * @param storeLongitude kinh độ tâm xưởng
 * @param maxRadiusKm    bán kính giao hàng tối đa cho phép (km)
 * @param distanceKm     khoảng cách thực tế tính toán từ xưởng đến vị trí yêu cầu (km)
 * @param allowed        cờ biểu thị vị trí có nằm trong vùng phục vụ hay không
 * @param message        thông điệp chi tiết thông báo kết quả hoặc từ chối lịch sự kèm hotline
 */
public record DeliveryDistanceResponse(
        String storeName,
        String storeAddress,
        BigDecimal storeLatitude,
        BigDecimal storeLongitude,
        BigDecimal maxRadiusKm,
        BigDecimal distanceKm,
        boolean allowed,
        String message
) {}
