package com.hmp.sales.feature.delivery.service;

import com.hmp.sales.common.exception.BusinessRuleException;
import com.hmp.sales.feature.delivery.dto.DeliveryDistanceResponse;
import com.hmp.sales.feature.delivery.dto.StoreInfoResponse;
import com.hmp.sales.feature.delivery.service.impl.DeliveryServiceImpl;
import com.hmp.sales.feature.setting.service.SystemSettingService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.math.BigDecimal;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("DeliveryServiceImpl Unit Tests")
class DeliveryServiceImplTest {

    @Mock
    private SystemSettingService systemSettingService;

    private DeliveryServiceImpl deliveryService;

    // Tọa độ cấu hình xưởng: 21.352410, 105.541280, bán kính 25km
    private static final String STORE_NAME = "Xưởng Tôn Thép HMP";
    private static final String STORE_ADDRESS = "Huyện Tam Dương, Vĩnh Phúc";
    private static final String STORE_LAT = "21.352410";
    private static final String STORE_LNG = "105.541280";
    private static final String MAX_RADIUS = "25.0";
    private static final String HOTLINE = "0987654321";

    @BeforeEach
    void setUp() {
        when(systemSettingService.getSettingValue("STORE_NAME")).thenReturn(Optional.of(STORE_NAME));
        when(systemSettingService.getSettingValue("STORE_ADDRESS")).thenReturn(Optional.of(STORE_ADDRESS));
        when(systemSettingService.getSettingValue("STORE_LATITUDE")).thenReturn(Optional.of(STORE_LAT));
        when(systemSettingService.getSettingValue("STORE_LONGITUDE")).thenReturn(Optional.of(STORE_LNG));
        when(systemSettingService.getSettingValue("STORE_MAX_DELIVERY_RADIUS_KM")).thenReturn(Optional.of(MAX_RADIUS));
        when(systemSettingService.getSettingValue("STORE_HOTLINE")).thenReturn(Optional.of(HOTLINE));

        deliveryService = new DeliveryServiceImpl(systemSettingService);
    }

    @Test
    @DisplayName("getStoreInfo - Lấy thông tin xưởng và bán kính phục vụ thành công")
    void getStoreInfo_success() {
        StoreInfoResponse info = deliveryService.getStoreInfo();
        assertThat(info.storeName()).isEqualTo(STORE_NAME);
        assertThat(info.storeAddress()).isEqualTo(STORE_ADDRESS);
        assertThat(info.storeLatitude()).isEqualByComparingTo(new BigDecimal(STORE_LAT));
        assertThat(info.storeLongitude()).isEqualByComparingTo(new BigDecimal(STORE_LNG));
        assertThat(info.maxRadiusKm()).isEqualByComparingTo(new BigDecimal(MAX_RADIUS));
        assertThat(info.storeHotline()).isEqualTo(HOTLINE);
    }

    @Test
    @DisplayName("checkDistance - Điểm trong bán kính 25km → allowed = true")
    void checkDistance_withinRadius_allowedTrue() {
        BigDecimal nearLat = new BigDecimal("21.380000");
        BigDecimal nearLng = new BigDecimal("105.580000");

        DeliveryDistanceResponse response = deliveryService.checkDistance(nearLat, nearLng);
        assertThat(response.allowed()).isTrue();
        assertThat(response.distanceKm()).isLessThan(new BigDecimal("25.0"));
        assertThat(response.message()).contains("nằm trong phạm vi giao hàng");
    }

    @Test
    @DisplayName("checkDistance - Điểm ngoài bán kính 25km → allowed = false kèm số hotline")
    void checkDistance_outsideRadius_allowedFalseWithHotline() {
        BigDecimal farLat = new BigDecimal("21.028511"); // Hà Nội
        BigDecimal farLng = new BigDecimal("105.804817");

        DeliveryDistanceResponse response = deliveryService.checkDistance(farLat, farLng);
        assertThat(response.allowed()).isFalse();
        assertThat(response.distanceKm()).isGreaterThan(new BigDecimal("25.0"));
        assertThat(response.message()).contains("Rất tiếc").contains(HOTLINE);
    }

    @Test
    @DisplayName("checkDistance - Tọa độ null → ném BusinessRuleException")
    void checkDistance_nullCoordinates_throwsException() {
        assertThatThrownBy(() -> deliveryService.checkDistance(null, new BigDecimal("105.5")))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("Vui lòng cung cấp đầy đủ tọa độ");
    }

    @Test
    @DisplayName("validateDeliveryLocation - Trong bán kính → trả về khoảng cách")
    void validateDeliveryLocation_withinRadius_returnsDistance() {
        BigDecimal nearLat = new BigDecimal("21.380000");
        BigDecimal nearLng = new BigDecimal("105.580000");

        BigDecimal distance = deliveryService.validateDeliveryLocation(nearLat, nearLng);
        assertThat(distance).isNotNull();
        assertThat(distance).isLessThan(new BigDecimal("25.0"));
    }

    @Test
    @DisplayName("validateDeliveryLocation - Ngoài bán kính → ném BusinessRuleException chặn đơn")
    void validateDeliveryLocation_outsideRadius_throwsBusinessRuleException() {
        BigDecimal farLat = new BigDecimal("21.028511");
        BigDecimal farLng = new BigDecimal("105.804817");

        assertThatThrownBy(() -> deliveryService.validateDeliveryLocation(farLat, farLng))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("Rất tiếc! Xưởng hiện chỉ hỗ trợ vận chuyển tôn lợp mái trong bán kính");
    }
}
