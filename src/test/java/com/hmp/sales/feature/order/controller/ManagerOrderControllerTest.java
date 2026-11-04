package com.hmp.sales.feature.order.controller;

import com.hmp.sales.common.exception.BusinessRuleException;
import com.hmp.sales.common.exception.GlobalExceptionHandler;
import com.hmp.sales.common.exception.InvalidOrderStateException;
import com.hmp.sales.common.exception.ResourceNotFoundException;
import com.hmp.sales.common.response.PageResponse;
import com.hmp.sales.config.AppProperties;
import com.hmp.sales.feature.order.dto.CalculateOrderRequest;
import com.hmp.sales.feature.order.dto.CustomerSummaryDto;
import com.hmp.sales.feature.order.dto.ManagerOrderFilterRequest;
import com.hmp.sales.feature.order.dto.ManagerOrderSummaryResponse;
import com.hmp.sales.feature.order.dto.OrderDetailResponse;
import com.hmp.sales.feature.order.dto.OrderItemResponse;
import com.hmp.sales.feature.order.dto.OrderStatusHistoryResponse;
import com.hmp.sales.feature.order.dto.UpdateOrderStatusRequest;
import com.hmp.sales.feature.order.entity.OrderStatus;
import com.hmp.sales.feature.order.service.ManagerOrderService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Collections;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
@DisplayName("ManagerOrderController Tests")
class ManagerOrderControllerTest {

    private MockMvc mockMvc;

    @Mock
    private ManagerOrderService managerOrderService;

    @Mock
    private AppProperties appProperties;

    @InjectMocks
    private ManagerOrderController managerOrderController;

    private TestingAuthenticationToken authentication;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(managerOrderController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();

        authentication = new TestingAuthenticationToken("2", null);
    }

    @Test
    @DisplayName("GET /api/v1/manager/orders - Lấy danh sách đơn hàng phân trang trả về 200")
    void getAllOrders_success_returns200() throws Exception {
        ManagerOrderSummaryResponse summary = new ManagerOrderSummaryResponse(
                100L,
                new CustomerSummaryDto(1L, "Nguyễn Văn A", "0987654321", "customer@example.com"),
                "Nguyễn Văn A",
                "0987654321",
                OrderStatus.PENDING,
                null,
                0L,
                null,
                Instant.now(),
                null
        );

        PageResponse<ManagerOrderSummaryResponse> pageResp = PageResponse.from(
                new PageImpl<>(List.of(summary), PageRequest.of(0, 20), 1)
        );

        when(managerOrderService.getAllOrders(any(ManagerOrderFilterRequest.class), any()))
                .thenReturn(pageResp);

        mockMvc.perform(get("/api/v1/manager/orders")
                        .principal(authentication)
                        .param("page", "0")
                        .param("size", "20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.statusCode").value(200))
                .andExpect(jsonPath("$.data.result[0].orderId").value(100))
                .andExpect(jsonPath("$.data.result[0].customer.fullName").value("Nguyễn Văn A"))
                .andExpect(jsonPath("$.data.meta.totalElements").value(1));
    }

    @Test
    @DisplayName("GET /api/v1/manager/orders/{orderId} - Lấy chi tiết đơn hàng thành công trả về 200")
    void getOrderDetail_success_returns200() throws Exception {
        OrderItemResponse itemResp = new OrderItemResponse(
                1L, 100L, "Tôn sóng", 1001L, "Biến thể A", "Đỏ", "m",
                new BigDecimal("10"), new BigDecimal("5"), 100_000, 0L, 5_000_000L
        );
        OrderStatusHistoryResponse historyResp = new OrderStatusHistoryResponse(
                1L, null, OrderStatus.PENDING, 1L, "Khách đặt", Instant.now()
        );
        OrderDetailResponse detailResp = new OrderDetailResponse(
                100L, OrderStatus.PENDING, 5_000_000L, 0L, 5_000_000L,
                "Nguyễn Văn A", "0987654321", "123 Đường X", "Ghi chú", null,
                Instant.now(), null, List.of(itemResp), List.of(historyResp)
        );

        when(managerOrderService.getOrderDetail(100L)).thenReturn(detailResp);

        mockMvc.perform(get("/api/v1/manager/orders/100")
                        .principal(authentication))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.statusCode").value(200))
                .andExpect(jsonPath("$.data.orderId").value(100))
                .andExpect(jsonPath("$.data.totalAmount").value(5_000_000));
    }

    @Test
    @DisplayName("GET /api/v1/manager/orders/{orderId} - Không tìm thấy đơn hàng trả về 404")
    void getOrderDetail_notFound_returns404() throws Exception {
        when(managerOrderService.getOrderDetail(999L))
                .thenThrow(new ResourceNotFoundException("Không tìm thấy đơn hàng ID: 999"));

        mockMvc.perform(get("/api/v1/manager/orders/999")
                        .principal(authentication))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.statusCode").value(404));
    }

    @Test
    @DisplayName("POST /api/v1/manager/orders/{orderId}/calculate - Tính giá đơn hàng thành công trả về 200")
    void calculateOrderPrice_success_returns200() throws Exception {
        String jsonPayload = """
                {
                    "discountAmount": 100000,
                    "items": [
                        {
                            "orderItemId": 1,
                            "unitPrice": 95000,
                            "discountAmount": 20000
                        }
                    ],
                    "note": "Chiết khấu đặc biệt"
                }
                """;

        OrderDetailResponse detailResp = new OrderDetailResponse(
                100L, OrderStatus.PENDING, 4_730_000L, 100_000L, 4_630_000L,
                "Nguyễn Văn A", "0987654321", "123 Đường X", null, null,
                Instant.now(), null, Collections.emptyList(), Collections.emptyList()
        );

        when(managerOrderService.calculateOrderPrice(eq(2L), eq(100L), any(CalculateOrderRequest.class)))
                .thenReturn(detailResp);

        mockMvc.perform(post("/api/v1/manager/orders/100/calculate")
                        .principal(authentication)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonPayload))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.statusCode").value(200))
                .andExpect(jsonPath("$.data.totalAmount").value(4_630_000))
                .andExpect(jsonPath("$.data.status").value("PENDING"));
    }

    @Test
    @DisplayName("POST /api/v1/manager/orders/{orderId}/confirm - Xác nhận đơn hàng thành công trả về 200")
    void confirmOrder_success_returns200() throws Exception {
        OrderDetailResponse detailResp = new OrderDetailResponse(
                100L, OrderStatus.CONFIRMED, 5_000_000L, 0L, 5_000_000L,
                "Nguyễn Văn A", "0987654321", "123 Đường X", null, null,
                Instant.now(), Instant.now(), Collections.emptyList(), Collections.emptyList()
        );

        when(managerOrderService.confirmOrder(2L, 100L)).thenReturn(detailResp);

        mockMvc.perform(post("/api/v1/manager/orders/100/confirm")
                        .principal(authentication))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.statusCode").value(200))
                .andExpect(jsonPath("$.data.status").value("CONFIRMED"));
    }

    @Test
    @DisplayName("POST /api/v1/manager/orders/{orderId}/confirm - Đơn chưa tính giá ném BusinessRuleException trả về 400")
    void confirmOrder_notCalculated_returns400() throws Exception {
        when(managerOrderService.confirmOrder(2L, 100L))
                .thenThrow(new BusinessRuleException("Đơn hàng chưa được tính giá, không thể xác nhận."));

        mockMvc.perform(post("/api/v1/manager/orders/100/confirm")
                        .principal(authentication))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.statusCode").value(400))
                .andExpect(jsonPath("$.message").value("Đơn hàng chưa được tính giá, không thể xác nhận."));
    }

    @Test
    @DisplayName("PATCH /api/v1/manager/orders/{orderId}/status - Cập nhật trạng thái thành công trả về 200")
    void updateOrderStatus_success_returns200() throws Exception {
        String jsonPayload = """
                {
                    "status": "PROCESSING",
                    "reason": "Bắt đầu gia công sản xuất"
                }
                """;

        OrderDetailResponse detailResp = new OrderDetailResponse(
                100L, OrderStatus.PROCESSING, 5_000_000L, 0L, 5_000_000L,
                "Nguyễn Văn A", "0987654321", "123 Đường X", null, null,
                Instant.now(), Instant.now(), Collections.emptyList(), Collections.emptyList()
        );

        when(managerOrderService.updateOrderStatus(eq(2L), eq(100L), any(UpdateOrderStatusRequest.class)))
                .thenReturn(detailResp);

        mockMvc.perform(patch("/api/v1/manager/orders/100/status")
                        .principal(authentication)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonPayload))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.statusCode").value(200))
                .andExpect(jsonPath("$.data.status").value("PROCESSING"));
    }

    @Test
    @DisplayName("PATCH /api/v1/manager/orders/{orderId}/status - Chuyển trạng thái không hợp lệ ném 409")
    void updateOrderStatus_invalidState_returns409() throws Exception {
        String jsonPayload = """
                {
                    "status": "COMPLETED",
                    "reason": "Nhảy cóc"
                }
                """;

        when(managerOrderService.updateOrderStatus(eq(2L), eq(100L), any(UpdateOrderStatusRequest.class)))
                .thenThrow(new InvalidOrderStateException("Không thể chuyển trạng thái đơn hàng từ 'PENDING' sang 'COMPLETED'."));

        mockMvc.perform(patch("/api/v1/manager/orders/100/status")
                        .principal(authentication)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonPayload))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.statusCode").value(409));
    }

    @Test
    @DisplayName("POST /api/v1/manager/orders/{orderId}/cancel - Hủy đơn hàng ở trạng thái SHIPPING thành công trả về 200")
    void cancelOrder_atShipping_returns200() throws Exception {
        String jsonPayload = """
                {
                    "reason": "Khách hàng từ chối nhận hàng lúc giao"
                }
                """;

        OrderDetailResponse detailResp = new OrderDetailResponse(
                100L, OrderStatus.CANCELLED, 5_000_000L, 0L, 5_000_000L,
                "Nguyễn Văn A", "0987654321", "123 Đường X", null, "Khách hàng từ chối nhận hàng lúc giao",
                Instant.now(), Instant.now(), Collections.emptyList(), Collections.emptyList()
        );

        when(managerOrderService.cancelOrder(eq(2L), eq(100L), any()))
                .thenReturn(detailResp);

        mockMvc.perform(post("/api/v1/manager/orders/100/cancel")
                        .principal(authentication)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonPayload))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.statusCode").value(200))
                .andExpect(jsonPath("$.data.status").value("CANCELLED"))
                .andExpect(jsonPath("$.data.cancellationReason").value("Khách hàng từ chối nhận hàng lúc giao"));
    }
}

