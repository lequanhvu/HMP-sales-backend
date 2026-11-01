package com.hmp.sales.feature.search.service;

import com.hmp.sales.feature.search.dto.SearchResourceType;
import com.hmp.sales.feature.search.dto.SearchSuggestionResponse;
import com.hmp.sales.feature.search.event.SearchIndexInvalidationEvent;

import java.util.List;

/**
 * Service định nghĩa các thao tác gợi ý tìm kiếm tức thời và đồng bộ chỉ mục trong RAM.
 */
public interface SearchSuggestionService {

    /**
     * Lấy danh sách gợi ý tìm kiếm theo từ khóa người dùng đang gõ.
     * <p>
     * Hỗ trợ tìm kiếm không dấu, tiền tố, chuỗi con và sửa lỗi gõ trượt phím (Fuzzy/Typo-tolerance).
     *
     * @param keyword từ khóa tìm kiếm (tối đa 50 ký tự)
     * @param type    loại tài nguyên cần lọc (PRODUCT, CATEGORY, BRAND, hoặc null để tìm tổng hợp)
     * @param limit   số lượng kết quả tối đa cần trả về (mặc định 10, tối đa 20)
     * @return danh sách các gợi ý khớp nhất
     */
    List<SearchSuggestionResponse> getSuggestions(String keyword, SearchResourceType type, int limit);

    /**
     * Nạp lại toàn bộ chỉ mục tìm kiếm từ cơ sở dữ liệu vào bộ nhớ RAM.
     */
    void reloadAll();

    /**
     * Xử lý sự kiện đồng bộ khi có thực thể bị thêm, sửa hoặc xóa.
     *
     * @param event sự kiện thay đổi dữ liệu
     */
    void handleInvalidation(SearchIndexInvalidationEvent event);
}

