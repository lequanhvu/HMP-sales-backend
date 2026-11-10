package com.hmp.sales.feature.user.controller;

import com.hmp.sales.common.exception.GlobalExceptionHandler;
import com.hmp.sales.feature.user.dto.CreateManagerRequest;
import com.hmp.sales.feature.user.dto.UserSearchRequest;
import com.hmp.sales.feature.user.dto.UserSummaryResponse;
import com.hmp.sales.feature.user.entity.AuthProvider;
import com.hmp.sales.feature.user.entity.UserStatus;
import com.hmp.sales.feature.user.service.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.web.method.annotation.AuthenticationPrincipalArgumentResolver;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.Instant;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
@DisplayName("UserController Unit Tests")
class UserControllerTest {

    private MockMvc mockMvc;

    @Mock
    private UserService userService;

    @InjectMocks
    private UserController userController;

    private TestingAuthenticationToken authentication;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(userController)
                .setCustomArgumentResolvers(new AuthenticationPrincipalArgumentResolver())
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();

        Jwt jwt = Jwt.withTokenValue("mock-token")
                .header("alg", "none")
                .claim("sub", "999")
                .build();
        authentication = new TestingAuthenticationToken(jwt, null);
    }

    @Test
    @DisplayName("GET /api/v1/admin/users - Danh sách người dùng trả về format phân trang chuẩn")
    void listUsers_returnsPagedResponse() throws Exception {
        UserSummaryResponse userSummary = new UserSummaryResponse(
                1L,
                "Nguyễn Văn A",
                "a@example.com",
                "0987654321",
                "ROLE_CUSTOMER",
                UserStatus.ACTIVE
        );

        Page<UserSummaryResponse> page = new PageImpl<>(
                List.of(userSummary),
                PageRequest.of(0, 20),
                1
        );

        when(userService.searchUsers(any(UserSearchRequest.class))).thenReturn(page);

        mockMvc.perform(get("/api/v1/admin/users")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.statusCode").value(200))
                .andExpect(jsonPath("$.message").value("Thành công"))
                .andExpect(jsonPath("$.data.result[0].userId").value(1))
                .andExpect(jsonPath("$.data.result[0].fullName").value("Nguyễn Văn A"))
                .andExpect(jsonPath("$.data.meta.page").value(0))
                .andExpect(jsonPath("$.data.meta.size").value(20))
                .andExpect(jsonPath("$.data.meta.totalElements").value(1))
                .andExpect(jsonPath("$.data.meta.totalPages").value(1));
    }

    @Test
    @DisplayName("POST /api/v1/admin/managers - Tạo Manager thành công với email hợp lệ đầy đủ TLD")
    void createManager_validRequest_returns201() throws Exception {
        UserSummaryResponse response = new UserSummaryResponse(
                28L,
                "managerName",
                "managertest@gmail.com",
                null,
                "MANAGER",
                UserStatus.ACTIVE
        );

        when(userService.createManager(any(CreateManagerRequest.class), any())).thenReturn(response);

        mockMvc.perform(post("/api/v1/admin/managers")
                        .principal(authentication)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "email": "managertest@gmail.com",
                                  "fullName": "managerName"
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.statusCode").value(201))
                .andExpect(jsonPath("$.message").value("Tạo tài khoản Manager thành công."))
                .andExpect(jsonPath("$.data.userId").value(28))
                .andExpect(jsonPath("$.data.email").value("managertest@gmail.com"));
    }

    @Test
    @DisplayName("POST /api/v1/admin/managers - Báo lỗi 400 Bad Request khi email thiếu đuôi tên miền TLD (ví dụ managertest@gmai)")
    void createManager_invalidEmailWithoutTld_returns400() throws Exception {
        mockMvc.perform(post("/api/v1/admin/managers")
                        .principal(authentication)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "email": "managertest@gmai",
                                  "fullName": "managerName"
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.statusCode").value(400))
                .andExpect(jsonPath("$.message").value("Dữ liệu yêu cầu không hợp lệ."))
                .andExpect(jsonPath("$.errors[0].field").value("email"))
                .andExpect(jsonPath("$.errors[0].message").value("Định dạng email không hợp lệ"));
    }
}
