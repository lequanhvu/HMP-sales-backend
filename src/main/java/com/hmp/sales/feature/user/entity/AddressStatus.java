package com.hmp.sales.feature.user.entity;

/**
 * Trạng thái hoạt động của địa chỉ nhận hàng khách hàng (Customer Address).
 */
public enum AddressStatus {

    /**
     * Địa chỉ đang hoạt động, có thể lựa chọn khi đặt hàng.
     */
    ACTIVE,

    /**
     * Địa chỉ tạm ngừng hoạt động hoặc đã bị vô hiệu hóa.
     */
    INACTIVE
}

