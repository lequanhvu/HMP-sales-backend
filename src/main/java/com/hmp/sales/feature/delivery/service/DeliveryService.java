package com.hmp.sales.feature.delivery.service;

import com.hmp.sales.feature.delivery.dto.DeliveryDistanceResponse;
import com.hmp.sales.feature.delivery.dto.StoreInfoResponse;

import java.math.BigDecimal;

/**
 * Hợp đồng nghiệp vụ quản lý khoảng cách và kiểm tra phạm vi giao hàng của xưởng.
 */
public interface DeliveryService {

    /**
     * Lấy thông tin vị trí cơ sở và bán kính phục vụ tối đa của xưởng.
     *
     * @return đối tượng {@link StoreInfoResponse}
     */
    StoreInfoResponse getStoreInfo();

    /**
     * Kiểm tra khoảng cách từ xưởng đến tọa độ người nhận và đánh giá khả năng phục vụ.
     *
     * @param latitude  vĩ độ người nhận
     * @param longitude kinh độ người nhận
     * @return đối tượng {@link DeliveryDistanceResponse} chứa khoảng cách và trạng thái
     */
    DeliveryDistanceResponse checkDistance(BigDecimal latitude, BigDecimal longitude);

    /**
     * Xác thực vị trí giao hàng khi khách đặt đơn hàng giao tận nơi (SHIPPING).
     * Ném {@link com.hmp.sales.common.exception.BusinessRuleException} nếu vượt quá bán kính.
     *
     * @param latitude  vĩ độ nhận hàng
     * @param longitude kinh độ nhận hàng
     * @return khoảng cách tính toán thực tế (km)
     */
    BigDecimal validateDeliveryLocation(BigDecimal latitude, BigDecimal longitude);
}
