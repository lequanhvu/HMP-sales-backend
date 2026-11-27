package com.hmp.sales.feature.delivery.service.impl;

import com.hmp.sales.common.exception.BusinessRuleException;
import com.hmp.sales.common.util.GeoUtils;
import com.hmp.sales.feature.delivery.dto.DeliveryDistanceResponse;
import com.hmp.sales.feature.delivery.dto.StoreInfoResponse;
import com.hmp.sales.feature.delivery.service.DeliveryService;
import com.hmp.sales.feature.setting.service.SystemSettingService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

/**
 * Triển khai nghiệp vụ kiểm tra khoảng cách và tính toán phạm vi giao hàng của xưởng.
 */
@Service
@Transactional(readOnly = true)
public class DeliveryServiceImpl implements DeliveryService {

    private static final Logger log = LoggerFactory.getLogger(DeliveryServiceImpl.class);

    private static final String DEFAULT_STORE_NAME = "Xưởng Cơ Khí & Tôn Thép HMP";
    private static final String DEFAULT_STORE_ADDRESS = "Khu Công Nghiệp Huyện, Tỉnh Vĩnh Phúc";
    private static final BigDecimal DEFAULT_STORE_LAT = new BigDecimal("21.352410");
    private static final BigDecimal DEFAULT_STORE_LNG = new BigDecimal("105.541280");
    private static final BigDecimal DEFAULT_MAX_RADIUS_KM = new BigDecimal("25.0");
    private static final String DEFAULT_HOTLINE = "0987654321";

    private final SystemSettingService systemSettingService;

    public DeliveryServiceImpl(SystemSettingService systemSettingService) {
        this.systemSettingService = systemSettingService;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public StoreInfoResponse getStoreInfo() {
        return new StoreInfoResponse(
                getStoreName(),
                getStoreAddress(),
                getStoreLatitude(),
                getStoreLongitude(),
                getMaxRadiusKm(),
                getStoreHotline()
        );
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public DeliveryDistanceResponse checkDistance(BigDecimal latitude, BigDecimal longitude) {
        if (latitude == null || longitude == null) {
            throw new BusinessRuleException("Vui lòng cung cấp đầy đủ tọa độ vĩ độ (latitude) và kinh độ (longitude).");
        }

        BigDecimal storeLat = getStoreLatitude();
        BigDecimal storeLng = getStoreLongitude();
        BigDecimal maxRadius = getMaxRadiusKm();
        String hotline = getStoreHotline();

        BigDecimal distance;
        try {
            distance = GeoUtils.calculateDistanceKm(storeLat, storeLng, latitude, longitude);
        } catch (IllegalArgumentException e) {
            throw new BusinessRuleException("Tọa độ vị trí không hợp lệ: " + e.getMessage());
        }

        boolean allowed = distance.compareTo(maxRadius) <= 0;
        String message;
        if (allowed) {
            message = "Vị trí nhận hàng cách xưởng " + distance + " km (nằm trong phạm vi giao hàng tối đa "
                    + maxRadius + " km).";
        } else {
            message = "Rất tiếc! Xưởng hiện chỉ hỗ trợ vận chuyển tôn lợp mái trong bán kính tối đa " + maxRadius
                    + " km. Vị trí của quý khách cách xưởng " + distance
                    + " km nên xưởng chưa thể hỗ trợ giao tận nơi. Quý khách vui lòng chọn hình thức 'Tự bốc hàng tại xưởng' hoặc liên hệ hotline "
                    + hotline + " để được hỗ trợ.";
        }

        return new DeliveryDistanceResponse(
                getStoreName(),
                getStoreAddress(),
                storeLat,
                storeLng,
                maxRadius,
                distance,
                allowed,
                message
        );
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public BigDecimal validateDeliveryLocation(BigDecimal latitude, BigDecimal longitude) {
        DeliveryDistanceResponse response = checkDistance(latitude, longitude);
        if (!response.allowed()) {
            log.warn("[DeliveryService] Từ chối đơn hàng do ngoài bán kính giao hàng: distance={} km, maxRadius={} km",
                    response.distanceKm(), response.maxRadiusKm());
            throw new BusinessRuleException(response.message());
        }
        return response.distanceKm();
    }

    private String getStoreName() {
        return systemSettingService.getSettingValue("STORE_NAME").orElse(DEFAULT_STORE_NAME);
    }

    private String getStoreAddress() {
        return systemSettingService.getSettingValue("STORE_ADDRESS").orElse(DEFAULT_STORE_ADDRESS);
    }

    private BigDecimal getStoreLatitude() {
        return systemSettingService.getSettingValue("STORE_LATITUDE")
                .map(this::parseBigDecimalOrNull)
                .orElse(DEFAULT_STORE_LAT);
    }

    private BigDecimal getStoreLongitude() {
        return systemSettingService.getSettingValue("STORE_LONGITUDE")
                .map(this::parseBigDecimalOrNull)
                .orElse(DEFAULT_STORE_LNG);
    }

    private BigDecimal getMaxRadiusKm() {
        return systemSettingService.getSettingValue("STORE_MAX_DELIVERY_RADIUS_KM")
                .map(this::parseBigDecimalOrNull)
                .orElse(DEFAULT_MAX_RADIUS_KM);
    }

    private String getStoreHotline() {
        return systemSettingService.getSettingValue("STORE_HOTLINE").orElse(DEFAULT_HOTLINE);
    }

    private BigDecimal parseBigDecimalOrNull(String str) {
        try {
            return new BigDecimal(str.trim());
        } catch (Exception e) {
            return null;
        }
    }
}
