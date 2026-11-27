package com.hmp.sales.common.util;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Lớp tiện ích tính toán khoảng cách địa lý giữa các tọa độ trên mặt cầu Trái Đất
 * sử dụng công thức Haversine (Haversine Formula).
 * <p>
 * Bán kính trung bình của Trái Đất quy ước: R = 6371.0 km.
 */
public final class GeoUtils {

    /** Bán kính trung bình của Trái Đất (kilometers). */
    public static final double EARTH_RADIUS_KM = 6371.0;

    private GeoUtils() {
        // Private constructor ngăn khởi tạo đối tượng tiện ích
    }

    /**
     * Tính khoảng cách đường chim bay (kilometers) giữa hai điểm tọa độ (lat, lon)
     * theo công thức Haversine.
     *
     * @param lat1 vĩ độ điểm thứ nhất (degrees)
     * @param lon1 kinh độ điểm thứ nhất (degrees)
     * @param lat2 vĩ độ điểm thứ hai (degrees)
     * @param lon2 kinh độ điểm thứ hai (degrees)
     * @return khoảng cách tính theo km, làm tròn 2 chữ số thập phân
     * @throws IllegalArgumentException nếu tọa độ null hoặc vượt ra ngoài khoảng hợp lệ
     */
    public static BigDecimal calculateDistanceKm(BigDecimal lat1, BigDecimal lon1, BigDecimal lat2, BigDecimal lon2) {
        if (lat1 == null || lon1 == null || lat2 == null || lon2 == null) {
            throw new IllegalArgumentException("Tọa độ không được để trống (null).");
        }

        validateCoordinate(lat1.doubleValue(), lon1.doubleValue());
        validateCoordinate(lat2.doubleValue(), lon2.doubleValue());

        double dLat = Math.toRadians(lat2.doubleValue() - lat1.doubleValue());
        double dLon = Math.toRadians(lon2.doubleValue() - lon1.doubleValue());

        double rLat1 = Math.toRadians(lat1.doubleValue());
        double rLat2 = Math.toRadians(lat2.doubleValue());

        double a = Math.sin(dLat / 2.0) * Math.sin(dLat / 2.0)
                + Math.cos(rLat1) * Math.cos(rLat2)
                * Math.sin(dLon / 2.0) * Math.sin(dLon / 2.0);

        double c = 2.0 * Math.atan2(Math.sqrt(a), Math.sqrt(1.0 - a));
        double distance = EARTH_RADIUS_KM * c;

        return BigDecimal.valueOf(distance).setScale(2, RoundingMode.HALF_UP);
    }

    /**
     * Kiểm tra xem điểm tọa độ thứ hai có nằm trong bán kính cho phép tính từ điểm thứ nhất hay không.
     *
     * @param centerLat vĩ độ tâm (tọa độ cửa hàng)
     * @param centerLon kinh độ tâm (tọa độ cửa hàng)
     * @param targetLat vĩ độ đích (tọa độ nhận hàng)
     * @param targetLon kinh độ đích (tọa độ nhận hàng)
     * @param radiusKm  bán kính tối đa cho phép (km)
     * @return {@code true} nếu khoảng cách <= bán kính; {@code false} nếu vượt quá
     */
    public static boolean isWithinRadius(BigDecimal centerLat, BigDecimal centerLon,
                                        BigDecimal targetLat, BigDecimal targetLon,
                                        BigDecimal radiusKm) {
        if (radiusKm == null || radiusKm.compareTo(BigDecimal.ZERO) <= 0) {
            return false;
        }
        BigDecimal distance = calculateDistanceKm(centerLat, centerLon, targetLat, targetLon);
        return distance.compareTo(radiusKm) <= 0;
    }

    private static void validateCoordinate(double lat, double lon) {
        if (lat < -90.0 || lat > 90.0) {
            throw new IllegalArgumentException("Vĩ độ (latitude) phải nằm trong khoảng [-90, 90]: " + lat);
        }
        if (lon < -180.0 || lon > 180.0) {
            throw new IllegalArgumentException("Kinh độ (longitude) phải nằm trong khoảng [-180, 180]: " + lon);
        }
    }
}
