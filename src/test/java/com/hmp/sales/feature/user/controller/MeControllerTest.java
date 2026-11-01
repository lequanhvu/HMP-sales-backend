package com.hmp.sales.feature.user.controller;

import com.hmp.sales.common.exception.GlobalExceptionHandler;
import com.hmp.sales.common.exception.ResourceNotFoundException;
import com.hmp.sales.feature.user.dto.AddressRequest;
import com.hmp.sales.feature.user.dto.AddressResponse;
import com.hmp.sales.feature.user.dto.UpdateProfileRequest;
import com.hmp.sales.feature.user.dto.UserProfileResponse;
import com.hmp.sales.feature.user.service.CustomerAddressService;
import com.hmp.sales.feature.user.service.UserProfileService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Controller unit/integration test cho {@link MeController}.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("MeController Tests")
class MeControllerTest {

    private MockMvc mockMvc;

    @Mock
    private UserProfileService userProfileService;

    @Mock
    private CustomerAddressService customerAddressService;

    @InjectMocks
    private MeController meController;

    private TestingAuthenticationToken authentication;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(meController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();

        // Tạo Authentication giả định với user ID = 1L
        authentication = new TestingAuthenticationToken("1", null);
    }

    @Test
    @DisplayName("GET /api/v1/me - Lấy thông tin hồ sơ thành công trả về 200")
    void getProfile_success_returns200() throws Exception {
        UserProfileResponse profile = new UserProfileResponse(
                1L, "Nguyễn Văn A", "a@example.com", "0987654321", "ACTIVE");

        when(userProfileService.getProfile(1L)).thenReturn(profile);

        mockMvc.perform(get("/api/v1/me")
                        .principal(authentication)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.statusCode").value(200))
                .andExpect(jsonPath("$.data.userId").value(1))
                .andExpect(jsonPath("$.data.fullName").value("Nguyễn Văn A"))
                .andExpect(jsonPath("$.data.email").value("a@example.com"))
                .andExpect(jsonPath("$.data.phone").value("0987654321"));
    }

    @Test
    @DisplayName("PATCH /api/v1/me - Cập nhật hồ sơ thành công trả về 200")
    void updateProfile_success_returns200() throws Exception {
        UserProfileResponse profile = new UserProfileResponse(
                1L, "Nguyễn Văn B", "a@example.com", "0911223344", "ACTIVE");

        when(userProfileService.updateProfile(eq(1L), any(UpdateProfileRequest.class))).thenReturn(profile);

        String jsonPayload = """
                {
                    "fullName": "Nguyễn Văn B",
                    "phone": "0911223344"
                }
                """;

        mockMvc.perform(patch("/api/v1/me")
                        .principal(authentication)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonPayload))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.statusCode").value(200))
                .andExpect(jsonPath("$.data.fullName").value("Nguyễn Văn B"))
                .andExpect(jsonPath("$.data.phone").value("0911223344"));
    }

    @Test
    @DisplayName("PATCH /api/v1/me - Số điện thoại không hợp lệ trả về 400 Validation Error")
    void updateProfile_invalidPhone_returns400() throws Exception {
        String jsonPayload = """
                {
                    "fullName": "Nguyễn Văn B",
                    "phone": "12345"
                }
                """;

        mockMvc.perform(patch("/api/v1/me")
                        .principal(authentication)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonPayload))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.statusCode").value(400))
                .andExpect(jsonPath("$.message").value("Dữ liệu yêu cầu không hợp lệ."));
    }

    @Test
    @DisplayName("GET /api/v1/me/addresses - Lấy danh sách địa chỉ trả về 200")
    void getAddresses_success_returns200() throws Exception {
        AddressResponse address = new AddressResponse(
                10L, "123 Đường A", true, "ACTIVE");

        when(customerAddressService.getAddresses(1L)).thenReturn(List.of(address));

        mockMvc.perform(get("/api/v1/me/addresses")
                        .principal(authentication))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.statusCode").value(200))
                .andExpect(jsonPath("$.data[0].addressId").value(10))
                .andExpect(jsonPath("$.data[0].address").value("123 Đường A"))
                .andExpect(jsonPath("$.data[0].isDefault").value(true));
    }

    @Test
    @DisplayName("POST /api/v1/me/addresses - Thêm địa chỉ mới thành công trả về 201 Created")
    void createAddress_success_returns201() throws Exception {
        AddressResponse created = new AddressResponse(
                10L, "123 Đường A", true, "ACTIVE");

        when(customerAddressService.createAddress(eq(1L), any(AddressRequest.class))).thenReturn(created);

        String jsonPayload = """
                {
                    "address": "123 Đường A",
                    "isDefault": true
                }
                """;

        mockMvc.perform(post("/api/v1/me/addresses")
                        .principal(authentication)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonPayload))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.statusCode").value(201))
                .andExpect(jsonPath("$.data.addressId").value(10))
                .andExpect(jsonPath("$.data.address").value("123 Đường A"));
    }

    @Test
    @DisplayName("POST /api/v1/me/addresses - Thiếu địa chỉ nhận hàng trả về 400 Bad Request")
    void createAddress_blankAddress_returns400() throws Exception {
        String jsonPayload = """
                {
                    "address": "",
                    "isDefault": true
                }
                """;

        mockMvc.perform(post("/api/v1/me/addresses")
                        .principal(authentication)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonPayload))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.statusCode").value(400))
                .andExpect(jsonPath("$.message").value("Dữ liệu yêu cầu không hợp lệ."));
    }

    @Test
    @DisplayName("PATCH /api/v1/me/addresses/{id} - Cập nhật địa chỉ thành công trả về 200")
    void updateAddress_success_returns200() throws Exception {
        AddressResponse updated = new AddressResponse(
                10L, "456 Đường B", false, "ACTIVE");

        when(customerAddressService.updateAddress(eq(1L), eq(10L), any(AddressRequest.class))).thenReturn(updated);

        String jsonPayload = """
                {
                    "address": "456 Đường B",
                    "isDefault": false
                }
                """;

        mockMvc.perform(patch("/api/v1/me/addresses/10")
                        .principal(authentication)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonPayload))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.statusCode").value(200))
                .andExpect(jsonPath("$.data.address").value("456 Đường B"))
                .andExpect(jsonPath("$.data.isDefault").value(false));
    }

    @Test
    @DisplayName("PATCH /api/v1/me/addresses/{id} - Không tìm thấy địa chỉ trả về 404 Not Found")
    void updateAddress_notFound_returns404() throws Exception {
        when(customerAddressService.updateAddress(eq(1L), eq(999L), any(AddressRequest.class)))
                .thenThrow(new ResourceNotFoundException("Không tìm thấy địa chỉ ID: 999"));

        String jsonPayload = """
                {
                    "address": "456 Đường B",
                    "isDefault": false
                }
                """;

        mockMvc.perform(patch("/api/v1/me/addresses/999")
                        .principal(authentication)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonPayload))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.statusCode").value(404))
                .andExpect(jsonPath("$.message").value("Không tìm thấy địa chỉ ID: 999"));
    }

    @Test
    @DisplayName("DELETE /api/v1/me/addresses/{id} - Xóa địa chỉ thành công trả về 200")
    void deleteAddress_success_returns200() throws Exception {
        doNothing().when(customerAddressService).deleteAddress(1L, 10L);

        mockMvc.perform(delete("/api/v1/me/addresses/10")
                        .principal(authentication))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.statusCode").value(200));

        verify(customerAddressService).deleteAddress(1L, 10L);
    }
}
