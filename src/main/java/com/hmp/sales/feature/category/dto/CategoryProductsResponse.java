package com.hmp.sales.feature.category.dto;

import com.hmp.sales.common.response.PageResponse;
import com.hmp.sales.feature.product.dto.ProductSummaryResponse;

/**
 * Response DTO cho danh sách sản phẩm thuộc một danh mục cụ thể có cấu trúc phân trang.
 *
 * @param categoryId   ID danh mục
 * @param categoryName tên danh mục
 * @param products     danh sách sản phẩm tóm tắt trực thuộc danh mục dạng phân trang (chứa result và meta)
 */
public record CategoryProductsResponse(
        Long categoryId,
        String categoryName,
        PageResponse<ProductSummaryResponse> products
) {
}
