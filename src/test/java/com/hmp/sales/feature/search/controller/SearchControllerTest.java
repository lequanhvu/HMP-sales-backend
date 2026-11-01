package com.hmp.sales.feature.search.controller;

import com.hmp.sales.common.exception.GlobalExceptionHandler;
import com.hmp.sales.feature.search.dto.SearchResourceType;
import com.hmp.sales.feature.search.dto.SearchSuggestionResponse;
import com.hmp.sales.feature.search.service.SearchSuggestionService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;

import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("SearchController Unit Tests")
class SearchControllerTest {

    private MockMvc mockMvc;

    @Mock
    private SearchSuggestionService searchSuggestionService;

    @InjectMocks
    private SearchController searchController;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(searchController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    @DisplayName("GET /api/v1/search/suggestions - Thành công trả về gợi ý")
    void getSuggestions_success() throws Exception {
        SearchSuggestionResponse item = new SearchSuggestionResponse(
                1L, "Tôn sóng lợp mái Hoa Sen", SearchResourceType.PRODUCT, "http://image.jpg", "Phụ kiện lợp mái");

        when(searchSuggestionService.getSuggestions(eq("ton"), eq(SearchResourceType.PRODUCT), anyInt()))
                .thenReturn(List.of(item));

        mockMvc.perform(get("/api/v1/search/suggestions")
                        .param("keyword", "ton")
                        .param("type", "PRODUCT")
                        .param("limit", "10")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.statusCode").value(200))
                .andExpect(jsonPath("$.data[0].id").value(1))
                .andExpect(jsonPath("$.data[0].name").value("Tôn sóng lợp mái Hoa Sen"))
                .andExpect(jsonPath("$.data[0].type").value("PRODUCT"));
    }

    @Test
    @DisplayName("GET /api/v1/search/suggestions - Keyword vượt quá 50 ký tự -> 400 Bad Request")
    void getSuggestions_keywordExceedsMaxLength_returns400() throws Exception {
        String longKeyword = "a".repeat(51);

        mockMvc.perform(get("/api/v1/search/suggestions")
                        .param("keyword", longKeyword)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.statusCode").value(400));
    }

    @Test
    @DisplayName("GET /api/v1/search/suggestions - Không truyền keyword -> trả về danh sách rỗng 200 OK")
    void getSuggestions_emptyKeyword_returns200() throws Exception {
        when(searchSuggestionService.getSuggestions(null, null, 10))
                .thenReturn(List.of());

        mockMvc.perform(get("/api/v1/search/suggestions")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.statusCode").value(200))
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data").isEmpty());
    }
}
