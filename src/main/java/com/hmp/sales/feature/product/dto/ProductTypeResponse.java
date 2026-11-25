package com.hmp.sales.feature.product.dto;

import com.hmp.sales.feature.product.entity.ProductType;

/**
 * DTO trả về thông tin loại sản phẩm kỹ thuật.
 *
 * @param id   ID loại sản phẩm
 * @param code mã định danh loại sản phẩm (ví dụ: ROOFING_SHEET, GUTTER, RETAIL)
 * @param name tên hiển thị tiếng Việt
 * @param note hướng dẫn cấu trúc thông số kỹ thuật
 */
public record ProductTypeResponse(
        Long id,
        String code,
        String name,
        String note
) {
    /**
     * Chuyển đổi từ entity {@link ProductType} sang DTO.
     *
     * @param type entity loại sản phẩm
     * @return ProductTypeResponse hoặc null nếu entity null
     */
    public static ProductTypeResponse fromEntity(ProductType type) {
        if (type == null) {
            return null;
        }
        return new ProductTypeResponse(
                type.getId(),
                type.getCode(),
                type.getName(),
                type.getNote()
        );
    }
}
