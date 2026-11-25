package com.hmp.sales.feature.cart.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hmp.sales.common.exception.GlobalExceptionHandler;
import com.hmp.sales.feature.cart.dto.CartCountResponse;
import com.hmp.sales.feature.cart.dto.CartItemAddRequest;
import com.hmp.sales.feature.cart.dto.CartItemResponse;
import com.hmp.sales.feature.cart.dto.CartItemUpdateRequest;
import com.hmp.sales.feature.cart.dto.CartResponse;
import com.hmp.sales.feature.cart.service.CartService;
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

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
@DisplayName("CustomerCartController Tests")
class CustomerCartControllerTest {

    private MockMvc mockMvc;

    @Mock
    private CartService cartService;

    @InjectMocks
    private CustomerCartController customerCartController;

    private final ObjectMapper objectMapper = new ObjectMapper();
    private TestingAuthenticationToken authentication;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(customerCartController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
        authentication = new TestingAuthenticationToken(10L, null);
    }

    @Test
    @DisplayName("GET /api/v1/cart - Trả về 200 OK và danh sách món")
    void getCart_returns200() throws Exception {
        CartItemResponse item = CartItemResponse.of(
                11L, 100L, "Tôn sóng", null, "Thành 15cm x Đáy 20cm", new BigDecimal("5"), 100000, 500000L
        );
        CartResponse cartResponse = new CartResponse(1L, 1, List.of(item));

        when(cartService.getCart(10L)).thenReturn(cartResponse);

        mockMvc.perform(get("/api/v1/cart")
                        .principal(authentication))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.statusCode").value(200))
                .andExpect(jsonPath("$.data.cartId").value(1))
                .andExpect(jsonPath("$.data.totalItems").value(1))
                .andExpect(jsonPath("$.data.items[0].productName").value("Tôn sóng"))
                .andExpect(jsonPath("$.data.items[0].dimensions").value("Thành 15cm x Đáy 20cm"));
    }

    @Test
    @DisplayName("GET /api/v1/cart/count - Trả về 200 OK và số lượng món")
    void getCartCount_returns200() throws Exception {
        when(cartService.getCartCount(10L)).thenReturn(new CartCountResponse(3L));

        mockMvc.perform(get("/api/v1/cart/count")
                        .principal(authentication))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.statusCode").value(200))
                .andExpect(jsonPath("$.data.totalCount").value(3));
    }

    @Test
    @DisplayName("POST /api/v1/cart/items - Trả về 201 Created khi thêm món hợp lệ")
    void addItem_returns201() throws Exception {
        CartItemAddRequest request = new CartItemAddRequest(
                100L, 1001L, 1L, 2L, new BigDecimal("5.5"), "Mép 1cm", new BigDecimal("10")
        );
        CartItemResponse response = CartItemResponse.of(
                11L, 100L, "Tôn sóng", new BigDecimal("5.5"), "Mép 1cm", new BigDecimal("10"), null, null
        );

        when(cartService.addItem(eq(10L), any(CartItemAddRequest.class))).thenReturn(response);

        mockMvc.perform(post("/api/v1/cart/items")
                        .principal(authentication)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.statusCode").value(201))
                .andExpect(jsonPath("$.data.cartItemId").value(11));
    }

    @Test
    @DisplayName("PATCH /api/v1/cart/items/{id} - Trả về 200 OK khi cập nhật thành công")
    void updateItem_returns200() throws Exception {
        CartItemUpdateRequest request = new CartItemUpdateRequest(new BigDecimal("15"), new BigDecimal("6.0"), "Mép 2cm");
        CartItemResponse response = CartItemResponse.of(
                11L, 100L, "Tôn sóng", new BigDecimal("6.0"), "Mép 2cm", new BigDecimal("15"), null, null
        );

        when(cartService.updateItem(eq(10L), eq(11L), any(CartItemUpdateRequest.class))).thenReturn(response);

        mockMvc.perform(patch("/api/v1/cart/items/11")
                        .principal(authentication)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.statusCode").value(200))
                .andExpect(jsonPath("$.data.quantity").value(15));
    }

    @Test
    @DisplayName("DELETE /api/v1/cart/items/{id} - Trả về 200 OK khi xóa thành công")
    void removeItem_returns200() throws Exception {
        mockMvc.perform(delete("/api/v1/cart/items/11")
                        .principal(authentication))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.statusCode").value(200))
                .andExpect(jsonPath("$.message").value("Xóa sản phẩm khỏi giỏ hàng thành công"));
    }

    @Test
    @DisplayName("DELETE /api/v1/cart/clear - Trả về 200 OK khi làm trống giỏ hàng")
    void clearCart_returns200() throws Exception {
        mockMvc.perform(delete("/api/v1/cart/clear")
                        .principal(authentication))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.statusCode").value(200))
                .andExpect(jsonPath("$.message").value("Làm trống giỏ hàng thành công"));
    }
}
