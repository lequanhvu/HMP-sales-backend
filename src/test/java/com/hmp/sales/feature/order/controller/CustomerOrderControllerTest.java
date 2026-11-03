package com.hmp.sales.feature.order.controller;

import com.hmp.sales.common.exception.GlobalExceptionHandler;
import com.hmp.sales.common.exception.InvalidOrderStateException;
import com.hmp.sales.common.exception.ResourceNotFoundException;
import com.hmp.sales.common.response.PageResponse;
import com.hmp.sales.config.AppProperties;
import com.hmp.sales.feature.order.dto.CreateOrderRequest;
import com.hmp.sales.feature.order.dto.OrderDetailResponse;
import com.hmp.sales.feature.order.dto.OrderItemResponse;
import com.hmp.sales.feature.order.dto.OrderStatusHistoryResponse;
import com.hmp.sales.feature.order.dto.OrderSummaryResponse;
import com.hmp.sales.feature.order.entity.OrderStatus;
import com.hmp.sales.feature.order.service.CustomerOrderService;
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
import java.util.Collections;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Controller unit/integration test cho {@link CustomerOrderController}.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("CustomerOrderController Tests")
class CustomerOrderControllerTest {

    private MockMvc mockMvc;

    @Mock
    private CustomerOrderService customerOrderService;

    @Mock
    private AppProperties appProperties;

    @InjectMocks
    private CustomerOrderController customerOrderController;

    private TestingAuthenticationToken authentication;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(customerOrderController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();

        authentication = new TestingAuthenticationToken("1", null);
    }

    @Test
    @DisplayName("POST /api/v1/orders - Tạo đơn hàng thành công trả về 201 Created")
    void createOrder_success_returns201() throws Exception {
        String jsonPayload = """
                {
                    "receiverName": "Nguyễn Văn A",
                    "receiverPhone": "0987654321",
                    "addressId": 1,
                    "note": "Giao giờ hành chính",
                    "items": [
                        {
                            "productId": 100,
                            "variantId": 1001,
                            "colorId": 1,
                            "unitId": 2,
                            "quantity": 10,
                            "size": 5
                        }
                    ]
                }
                """;

        OrderItemResponse itemResp = new OrderItemResponse(
                1L, 100L, "Tôn sóng Hoa Sen", 1001L, "Tôn 0.35mm - 5 sóng", "Xanh", "m",
                new BigDecimal("10"), new BigDecimal("5"), null, null);
        OrderStatusHistoryResponse historyResp = new OrderStatusHistoryResponse(
                1L, null, OrderStatus.PENDING, 1L, "Khách hàng tạo đơn hàng mới", Instant.now());
        OrderDetailResponse detailResp = new OrderDetailResponse(
                10L, OrderStatus.PENDING, null, "Nguyễn Văn A", "0987654321",
                "123 Đường ABC", "Giao giờ hành chính", null, Instant.now(), null,
                List.of(itemResp), List.of(historyResp));

        when(customerOrderService.createOrder(eq(1L), any(CreateOrderRequest.class)))
                .thenReturn(detailResp);

        mockMvc.perform(post("/api/v1/orders")
                        .principal(authentication)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonPayload))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.statusCode").value(201))
                .andExpect(jsonPath("$.data.orderId").value(10))
                .andExpect(jsonPath("$.data.status").value("PENDING"))
                .andExpect(jsonPath("$.data.receiverName").value("Nguyễn Văn A"))
                .andExpect(jsonPath("$.data.items[0].productName").value("Tôn sóng Hoa Sen"));
    }

    @Test
    @DisplayName("POST /api/v1/orders - Validate thất bại khi tên người nhận để trống trả về 400")
    void createOrder_validationError_returns400() throws Exception {
        String jsonPayload = """
                {
                    "receiverName": "",
                    "receiverPhone": "0987654321",
                    "addressId": 1,
                    "items": [
                        {
                            "productId": 100,
                            "quantity": 1
                        }
                    ]
                }
                """;

        mockMvc.perform(post("/api/v1/orders")
                        .principal(authentication)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonPayload))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.statusCode").value(400))
                .andExpect(jsonPath("$.message").value("Dữ liệu yêu cầu không hợp lệ."))
                .andExpect(jsonPath("$.errors[0].field").value("receiverName"));
    }

    @Test
    @DisplayName("POST /api/v1/orders - Validate thất bại khi colorId không phải kiểu số trả về 400")
    void createOrder_invalidColorIdFormat_returns400() throws Exception {
        String jsonPayload = """
                {
                    "receiverName": "Nguyễn Văn A",
                    "receiverPhone": "0987654321",
                    "addressId": 1,
                    "items": [
                        {
                            "productId": 100,
                            "variantId": 1001,
                            "colorId": "abc",
                            "unitId": 2,
                            "quantity": 10
                        }
                    ]
                }
                """;

        mockMvc.perform(post("/api/v1/orders")
                        .principal(authentication)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonPayload))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.statusCode").value(400))
                .andExpect(jsonPath("$.message").value("Dữ liệu yêu cầu không hợp lệ."))
                .andExpect(jsonPath("$.errors[0].field").value("items[0].colorId"))
                .andExpect(jsonPath("$.errors[0].message").value(org.hamcrest.Matchers.containsString("abc")));
    }

    @Test
    @DisplayName("GET /api/v1/orders - Lấy danh sách đơn hàng của tôi thành công trả về 200")
    void getOwnOrders_success_returns200() throws Exception {
        OrderSummaryResponse summary = new OrderSummaryResponse(
                10L, OrderStatus.PENDING, null, "Nguyễn Văn A", Instant.now(), null);
        PageResponse<OrderSummaryResponse> pageResp = new PageResponse<>(
                List.of(summary),
                new PageResponse.PageMeta(0, 20, 1L, 1)
        );

        when(customerOrderService.getOwnOrders(eq(1L), any(), any()))
                .thenReturn(pageResp);

        mockMvc.perform(get("/api/v1/orders")
                        .principal(authentication)
                        .param("page", "0")
                        .param("size", "20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.statusCode").value(200))
                .andExpect(jsonPath("$.data.result[0].orderId").value(10))
                .andExpect(jsonPath("$.data.meta.totalElements").value(1));
    }

    @Test
    @DisplayName("GET /api/v1/orders/{orderId} - Lấy chi tiết đơn hàng thành công trả về 200")
    void getOwnOrderDetail_success_returns200() throws Exception {
        OrderDetailResponse detailResp = new OrderDetailResponse(
                10L, OrderStatus.PENDING, null, "Nguyễn Văn A", "0987654321",
                "123 Đường ABC", null, null, Instant.now(), null,
                Collections.emptyList(), Collections.emptyList());

        when(customerOrderService.getOwnOrderDetail(1L, 10L)).thenReturn(detailResp);

        mockMvc.perform(get("/api/v1/orders/10")
                        .principal(authentication))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.statusCode").value(200))
                .andExpect(jsonPath("$.data.orderId").value(10))
                .andExpect(jsonPath("$.data.receiverPhone").value("0987654321"));
    }

    @Test
    @DisplayName("GET /api/v1/orders/{orderId} - Đơn hàng không tồn tại trả về 404")
    void getOwnOrderDetail_notFound_returns404() throws Exception {
        when(customerOrderService.getOwnOrderDetail(1L, 999L))
                .thenThrow(new ResourceNotFoundException("Không tìm thấy đơn hàng ID: 999"));

        mockMvc.perform(get("/api/v1/orders/999")
                        .principal(authentication))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.statusCode").value(404));
    }

    @Test
    @DisplayName("POST /api/v1/orders/{orderId}/cancel - Hủy đơn hàng thành công trả về 200")
    void cancelOwnOrder_success_returns200() throws Exception {
        String jsonPayload = """
                {
                    "reason": "Thay đổi kế hoạch"
                }
                """;

        OrderDetailResponse detailResp = new OrderDetailResponse(
                10L, OrderStatus.CANCELLED, null, "Nguyễn Văn A", "0987654321",
                "123 Đường ABC", null, "Thay đổi kế hoạch", Instant.now(), null,
                Collections.emptyList(), Collections.emptyList());

        when(customerOrderService.cancelOwnOrder(eq(1L), eq(10L), any()))
                .thenReturn(detailResp);

        mockMvc.perform(post("/api/v1/orders/10/cancel")
                        .principal(authentication)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonPayload))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.statusCode").value(200))
                .andExpect(jsonPath("$.data.status").value("CANCELLED"))
                .andExpect(jsonPath("$.data.cancellationReason").value("Thay đổi kế hoạch"));
    }

    @Test
    @DisplayName("POST /api/v1/orders/{orderId}/cancel - Trạng thái không thể hủy trả về 409 Conflict")
    void cancelOwnOrder_invalidState_returns409() throws Exception {
        when(customerOrderService.cancelOwnOrder(eq(1L), eq(10L), any()))
                .thenThrow(new InvalidOrderStateException("Không thể hủy đơn hàng đang ở trạng thái 'PROCESSING'."));

        mockMvc.perform(post("/api/v1/orders/10/cancel")
                        .principal(authentication))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.statusCode").value(409))
                .andExpect(jsonPath("$.message").value("Không thể hủy đơn hàng đang ở trạng thái 'PROCESSING'."));
    }
}
