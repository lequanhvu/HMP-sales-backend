package com.hmp.sales.feature.product.entity;

/**
 * Trạng thái hiển thị của sản phẩm nổi bật (Featured Product).
 * <ul>
 *   <li>{@link #ACTIVE}: Sản phẩm nổi bật đang kích hoạt, hiển thị trên trang chủ và danh sách nổi bật.</li>
 *   <li>{@link #INACTIVE}: Tạm thời ẩn khỏi danh sách nổi bật nhưng vẫn lưu dữ liệu trong hệ thống.</li>
 * </ul>
 */
public enum FeaturedProductStatus {

    /**
     * Đang kích hoạt và hiển thị nổi bật.
     */
    ACTIVE,

    /**
     * Tạm ẩn khỏi danh sách nổi bật.
     */
    INACTIVE
}
