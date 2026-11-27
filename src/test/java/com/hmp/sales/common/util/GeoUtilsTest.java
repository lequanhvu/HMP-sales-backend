package com.hmp.sales.common.util;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("GeoUtils Unit Tests")
class GeoUtilsTest {

    // Tọa độ mẫu: Xưởng HMP (Tam Dương, Vĩnh Phúc)
    private static final BigDecimal STORE_LAT = new BigDecimal("21.352410");
    private static final BigDecimal STORE_LNG = new BigDecimal("105.541280");

    // Điểm gần: Chân công trình trong huyện (~5.5 km)
    private static final BigDecimal NEAR_LAT = new BigDecimal("21.380000");
    private static final BigDecimal NEAR_LNG = new BigDecimal("105.580000");

    // Điểm xa: Trung tâm Hà Nội (~46 km)
    private static final BigDecimal FAR_LAT = new BigDecimal("21.028511");
    private static final BigDecimal FAR_LNG = new BigDecimal("105.804817");

    @Test
    @DisplayName("calculateDistanceKm - Khoảng cách giữa 2 điểm trùng nhau bằng 0")
    void calculateDistanceKm_samePoint_returnsZero() {
        BigDecimal distance = GeoUtils.calculateDistanceKm(STORE_LAT, STORE_LNG, STORE_LAT, STORE_LNG);
        assertThat(distance).isEqualByComparingTo(BigDecimal.ZERO);
    }

    @Test
    @DisplayName("calculateDistanceKm - Tính khoảng cách giữa 2 điểm hợp lệ")
    void calculateDistanceKm_validPoints_returnsCorrectDistance() {
        BigDecimal distance = GeoUtils.calculateDistanceKm(STORE_LAT, STORE_LNG, NEAR_LAT, NEAR_LNG);
        // Khoảng cách xấp xỉ 5 - 6 km
        assertThat(distance).isGreaterThan(new BigDecimal("4.0"));
        assertThat(distance).isLessThan(new BigDecimal("7.0"));
    }

    @Test
    @DisplayName("calculateDistanceKm - Tọa độ null → ném IllegalArgumentException")
    void calculateDistanceKm_nullCoordinate_throwsException() {
        assertThatThrownBy(() -> GeoUtils.calculateDistanceKm(null, STORE_LNG, NEAR_LAT, NEAR_LNG))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Tọa độ không được để trống");
    }

    @Test
    @DisplayName("calculateDistanceKm - Vĩ độ vượt khoảng [-90, 90] → ném IllegalArgumentException")
    void calculateDistanceKm_invalidLatitude_throwsException() {
        BigDecimal invalidLat = new BigDecimal("95.0");
        assertThatThrownBy(() -> GeoUtils.calculateDistanceKm(invalidLat, STORE_LNG, NEAR_LAT, NEAR_LNG))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Vĩ độ (latitude) phải nằm trong khoảng");
    }

    @Test
    @DisplayName("calculateDistanceKm - Kinh độ vượt khoảng [-180, 180] → ném IllegalArgumentException")
    void calculateDistanceKm_invalidLongitude_throwsException() {
        BigDecimal invalidLng = new BigDecimal("190.0");
        assertThatThrownBy(() -> GeoUtils.calculateDistanceKm(STORE_LAT, invalidLng, NEAR_LAT, NEAR_LNG))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Kinh độ (longitude) phải nằm trong khoảng");
    }

    @Test
    @DisplayName("isWithinRadius - Điểm nằm trong bán kính 25km → trả về true")
    void isWithinRadius_insideRadius_returnsTrue() {
        boolean within = GeoUtils.isWithinRadius(STORE_LAT, STORE_LNG, NEAR_LAT, NEAR_LNG, new BigDecimal("25.0"));
        assertThat(within).isTrue();
    }

    @Test
    @DisplayName("isWithinRadius - Điểm nằm ngoài bán kính 25km (Hà Nội ~46km) → trả về false")
    void isWithinRadius_outsideRadius_returnsFalse() {
        boolean within = GeoUtils.isWithinRadius(STORE_LAT, STORE_LNG, FAR_LAT, FAR_LNG, new BigDecimal("25.0"));
        assertThat(within).isFalse();
    }
}
