package com.hmp.sales.feature.search.dto;

/**
 * Bản ghi chỉ mục gợi ý tìm kiếm lưu trữ siêu nhẹ trong bộ nhớ RAM của máy chủ.
 * <p>
 * Tuyệt đối không giữ tham chiếu đến JPA Entity để tránh rò rỉ bộ nhớ (Heap Overflow).
 *
 * @param id           khóa chính định danh tài nguyên
 * @param name         tên hiển thị gốc có dấu
 * @param unaccentName tên đã được chuẩn hóa chữ thường không dấu
 * @param type         loại tài nguyên (PRODUCT, CATEGORY, BRAND)
 * @param imageUrl     đường dẫn hình ảnh đại diện (nếu có)
 * @param extraInfo    thông tin phụ trợ (ví dụ tên danh mục hoặc tên thương hiệu)
 */
public record SuggestionIndexItem(
        Long id,
        String name,
        String unaccentName,
        SearchResourceType type,
        String imageUrl,
        String extraInfo
) {
}

