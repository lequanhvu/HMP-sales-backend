package com.hmp.sales.feature.search.controller;

import com.hmp.sales.common.exception.BusinessRuleException;
import com.hmp.sales.common.response.ApiSuccessResponse;
import com.hmp.sales.feature.search.dto.SearchResourceType;
import com.hmp.sales.feature.search.dto.SearchSuggestionResponse;
import com.hmp.sales.feature.search.service.SearchSuggestionService;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * REST Controller công khai phục vụ gợi ý tìm kiếm tức thời (Search Autocomplete / Type-ahead).
 * <p>
 * Phản hồi tức thời từ bộ nhớ RAM của Server ($&lt; 2\text{ms}$), hỗ trợ tiếng Việt không dấu,
 * tìm kiếm tiền tố và tự động sửa lỗi gõ nhầm bàn phím (Fuzzy Typo Tolerance).
 */
@Validated
@RestController
@RequestMapping("/api/v1/search")
public class SearchController {

    private final SearchSuggestionService searchSuggestionService;

    /**
     * Khởi tạo SearchController với SearchSuggestionService.
     *
     * @param searchSuggestionService service gợi ý tìm kiếm
     */
    public SearchController(SearchSuggestionService searchSuggestionService) {
        this.searchSuggestionService = searchSuggestionService;
    }

    /**
     * Lấy danh sách gợi ý tìm kiếm theo từ khóa người dùng đang gõ.
     *
     * @param keyword từ khóa tìm kiếm (tối đa 50 ký tự)
     * @param type    loại tài nguyên cần gợi ý (PRODUCT, CATEGORY, BRAND, hoặc null để tìm tổng hợp)
     * @param limit   số lượng kết quả tối đa (mặc định 10, tối đa 20)
     * @return danh sách các gợi ý phù hợp
     */
    @GetMapping("/suggestions")
    public ResponseEntity<ApiSuccessResponse<List<SearchSuggestionResponse>>> getSuggestions(
            @RequestParam(required = false)
            @Size(max = 50, message = "Từ khóa tìm kiếm không được vượt quá 50 ký tự")
            String keyword,

            @RequestParam(required = false)
            SearchResourceType type,

            @RequestParam(defaultValue = "10")
            @Min(value = 1, message = "Số lượng gợi ý tối thiểu là 1")
            @Max(value = 20, message = "Số lượng gợi ý tối đa là 20")
            int limit) {

        if (keyword != null && keyword.length() > 50) {
            throw new BusinessRuleException("Từ khóa tìm kiếm không được vượt quá 50 ký tự.");
        }

        List<SearchSuggestionResponse> suggestions = searchSuggestionService.getSuggestions(keyword, type, limit);
        return ResponseEntity.ok(ApiSuccessResponse.success(suggestions));
    }
}
