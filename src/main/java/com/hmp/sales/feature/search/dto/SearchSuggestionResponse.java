package com.hmp.sales.feature.search.dto;

/**
 * DTO phản hồi gợi ý tìm kiếm trả về cho Client.
 *
 * @param id        ID tài nguyên
 * @param name      tên hiển thị gốc
 * @param type      loại tài nguyên
 * @param imageUrl  đường dẫn ảnh đại diện (nếu có)
 * @param extraInfo thông tin phụ trợ (ví dụ tên danh mục hoặc tên thương hiệu)
 */
public record SearchSuggestionResponse(
        Long id,
        String name,
        SearchResourceType type,
        String imageUrl,
        String extraInfo
) {
    /**
     * Chuyển đổi từ {@link SuggestionIndexItem} sang response DTO.
     *
     * @param item bản ghi chỉ mục trong RAM
     * @return response DTO
     */
    public static SearchSuggestionResponse fromIndexItem(SuggestionIndexItem item) {
        return new SearchSuggestionResponse(
                item.id(),
                item.name(),
                item.type(),
                item.imageUrl(),
                item.extraInfo()
        );
    }
}

