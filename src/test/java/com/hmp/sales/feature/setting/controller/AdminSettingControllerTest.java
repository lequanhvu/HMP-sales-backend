package com.hmp.sales.feature.setting.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hmp.sales.common.exception.GlobalExceptionHandler;
import com.hmp.sales.feature.setting.dto.SettingResponse;
import com.hmp.sales.feature.setting.dto.UpdateSettingRequest;
import com.hmp.sales.feature.setting.service.SystemSettingService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.Instant;
import java.util.List;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
@DisplayName("AdminSettingController Unit Tests")
class AdminSettingControllerTest {

    private MockMvc mockMvc;
    private ObjectMapper objectMapper;

    @Mock
    private SystemSettingService systemSettingService;

    @InjectMocks
    private AdminSettingController adminSettingController;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        mockMvc = MockMvcBuilders.standaloneSetup(adminSettingController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    @DisplayName("GET /api/v1/admin/settings - Trả về danh sách cấu hình thành công")
    void listSettings_success() throws Exception {
        SettingResponse item = new SettingResponse(
                "ORDER_CUSTOMER_CANCELLATION_WINDOW_MINUTES",
                "30",
                "INT",
                "Cửa sổ hủy",
                Instant.now()
        );
        when(systemSettingService.listSettings()).thenReturn(List.of(item));

        mockMvc.perform(get("/api/v1/admin/settings"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.statusCode").value(200))
                .andExpect(jsonPath("$.data[0].key").value("ORDER_CUSTOMER_CANCELLATION_WINDOW_MINUTES"))
                .andExpect(jsonPath("$.data[0].value").value("30"))
                .andExpect(jsonPath("$.data[0].dataType").value("INT"));
    }

    @Test
    @DisplayName("PUT /api/v1/admin/settings/{key} - Cập nhật thành công")
    void updateSetting_success() throws Exception {
        SettingResponse updated = new SettingResponse(
                "ORDER_CUSTOMER_CANCELLATION_WINDOW_MINUTES",
                "60",
                "INT",
                "Cửa sổ hủy",
                Instant.now()
        );
        when(systemSettingService.updateSetting(eq(1L), eq("ORDER_CUSTOMER_CANCELLATION_WINDOW_MINUTES"), eq("60")))
                .thenReturn(updated);

        var auth = new UsernamePasswordAuthenticationToken("1", null,
                List.of(new SimpleGrantedAuthority("ROLE_ADMIN")));

        UpdateSettingRequest req = new UpdateSettingRequest("60");

        mockMvc.perform(put("/api/v1/admin/settings/ORDER_CUSTOMER_CANCELLATION_WINDOW_MINUTES")
                        .principal(auth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.statusCode").value(200))
                .andExpect(jsonPath("$.data.value").value("60"));
    }

    @Test
    @DisplayName("PUT /api/v1/admin/settings/{key} - 400 khi body thiếu value")
    void updateSetting_blankValue_badRequest() throws Exception {
        mockMvc.perform(put("/api/v1/admin/settings/ORDER_CUSTOMER_CANCELLATION_WINDOW_MINUTES")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"value\":\"\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.statusCode").value(400));
    }
}

