package com.hmp.sales.feature.product.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/**
 * Request DTO để tạo mới hoặc cập nhật biến thể sản phẩm.
 *
 * @param variantCode mã biến thể nội bộ (tùy chọn, tối đa 80 ký tự)
 * @param variantName tên biến thể hiển thị (bắt buộc, tối đa 150 ký tự)
 * @param typeValue   loại sóng/loại tôn (tùy chọn)
 * @param thickness   độ dày tôn tính theo mm (tùy chọn)
 * @param width       khổ rộng tôn tính theo mm hoặc mét (tùy chọn)
 * @param colorIds    danh sách ID màu sắc liên kết (tùy chọn)
 * @param prices      bảng giá theo đơn vị tính: Map từ unitId → price (VND integer)
 */
public record ProductVariantRequest(

        @Size(max = 80, message = "Mã biến thể không được vượt quá 80 ký tự.")
        String variantCode,

        @NotBlank(message = "Tên biến thể không được để trống.")
        @Size(max = 150, message = "Tên biến thể không được vượt quá 150 ký tự.")
        String variantName,

        String typeValue,

        // độ dày
        BigDecimal thickness,

        // khổ rộng tôn
        BigDecimal width,

        List<Long> colorIds,

        Map<Long, Integer> prices
) {
    /**
     * Constructor tương thích ngược cho các bài kiểm thử hoặc luồng không truyền tham số width.
     *
     * @param variantCode mã biến thể nội bộ
     * @param variantName tên biến thể hiển thị
     * @param typeValue   loại sóng/loại tôn
     * @param thickness   độ dày tôn tính theo mm
     * @param colorIds    danh sách ID màu sắc liên kết
     * @param prices      bảng giá theo đơn vị tính
     */
    public ProductVariantRequest(String variantCode, String variantName, String typeValue,
                                 BigDecimal thickness, List<Long> colorIds, Map<Long, Integer> prices) {
        this(variantCode, variantName, typeValue, thickness, null, colorIds, prices);
    }
}
