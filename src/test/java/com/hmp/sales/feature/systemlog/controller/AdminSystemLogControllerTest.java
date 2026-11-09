package com.hmp.sales.feature.systemlog.controller;

import com.hmp.sales.common.exception.GlobalExceptionHandler;
import com.hmp.sales.feature.systemlog.dto.SystemLogDetailResponse;
import com.hmp.sales.feature.systemlog.dto.SystemLogSearchRequest;
import com.hmp.sales.feature.systemlog.dto.SystemLogSummaryResponse;
import com.hmp.sales.feature.systemlog.service.SystemLogService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.Instant;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
@DisplayName("AdminSystemLogController Unit Tests")
class AdminSystemLogControllerTest {

    private MockMvc mockMvc;

    @Mock
    private SystemLogService systemLogService;

    @InjectMocks
    private AdminSystemLogController adminSystemLogController;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(adminSystemLogController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    @DisplayName("GET /api/v1/admin/system-logs - Danh sách nhật ký có envelope phân trang chuẩn")
    void searchLogs_success() throws Exception {
        SystemLogSummaryResponse item = new SystemLogSummaryResponse(
                9001L,
                2001L,
                "PRODUCT_UPDATE",
                "Updated product 100",
                Instant.parse("2026-10-01T09:40:00Z")
        );
        var page = new PageImpl<>(List.of(item));
        when(systemLogService.search(any(SystemLogSearchRequest.class), anyInt(), anyInt()))
                .thenReturn(page);

        mockMvc.perform(get("/api/v1/admin/system-logs")
                        .param("activity", "PRODUCT_UPDATE")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.statusCode").value(200))
                .andExpect(jsonPath("$.data.result[0].logId").value(9001))
                .andExpect(jsonPath("$.data.result[0].activity").value("PRODUCT_UPDATE"))
                .andExpect(jsonPath("$.data.meta.page").value(0))
                .andExpect(jsonPath("$.data.meta.totalElements").value(1));
    }

    @Test
    @DisplayName("GET /api/v1/admin/system-logs/{logId} - Trả về chi tiết bản ghi")
    void getLog_success() throws Exception {
        SystemLogDetailResponse detail = new SystemLogDetailResponse(
                9001L,
                2001L,
                "PRODUCT_UPDATE",
                "PRODUCT",
                100L,
                "Updated product 100",
                "{\"name\":\"Old\"}",
                "{\"name\":\"New\"}",
                Instant.parse("2026-10-01T09:40:00Z")
        );
        when(systemLogService.getById(eq(9001L))).thenReturn(detail);

        mockMvc.perform(get("/api/v1/admin/system-logs/9001"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.statusCode").value(200))
                .andExpect(jsonPath("$.data.logId").value(9001))
                .andExpect(jsonPath("$.data.entityType").value("PRODUCT"))
                .andExpect(jsonPath("$.data.oldValue").value("{\"name\":\"Old\"}"))
                .andExpect(jsonPath("$.data.newValue").value("{\"name\":\"New\"}"));
    }
}

