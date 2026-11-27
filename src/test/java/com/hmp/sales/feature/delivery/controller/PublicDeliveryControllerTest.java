package com.hmp.sales.feature.delivery.controller;

import com.hmp.sales.common.exception.GlobalExceptionHandler;
import com.hmp.sales.feature.delivery.dto.DeliveryDistanceResponse;
import com.hmp.sales.feature.delivery.dto.StoreInfoResponse;
import com.hmp.sales.feature.delivery.service.DeliveryService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.math.BigDecimal;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
@DisplayName("PublicDeliveryController MockMvc Tests")
class PublicDeliveryControllerTest {

    private MockMvc mockMvc;

    @Mock
    private DeliveryService deliveryService;

    @InjectMocks
    private PublicDeliveryController controller;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    @DisplayName("GET /api/v1/delivery/store-info - Trả về 200 OK kèm thông tin xưởng")
    void getStoreInfo_returns200() throws Exception {
        StoreInfoResponse storeInfo = new StoreInfoResponse(
                "Xưởng Tôn Thép HMP",
                "Huyện Tam Dương, Vĩnh Phúc",
                new BigDecimal("21.352410"),
                new BigDecimal("105.541280"),
                new BigDecimal("25.0"),
                "0987654321"
        );
        when(deliveryService.getStoreInfo()).thenReturn(storeInfo);

        mockMvc.perform(get("/api/v1/delivery/store-info"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.statusCode").value(200))
                .andExpect(jsonPath("$.data.storeName").value("Xưởng Tôn Thép HMP"))
                .andExpect(jsonPath("$.data.maxRadiusKm").value(25.0));
    }

    @Test
    @DisplayName("GET /api/v1/delivery/check-distance - Trả về 200 OK với kết quả khoảng cách")
    void checkDistance_returns200() throws Exception {
        BigDecimal lat = new BigDecimal("21.380000");
        BigDecimal lng = new BigDecimal("105.580000");
        DeliveryDistanceResponse distanceResponse = new DeliveryDistanceResponse(
                "Xưởng Tôn Thép HMP",
                "Huyện Tam Dương, Vĩnh Phúc",
                new BigDecimal("21.352410"),
                new BigDecimal("105.541280"),
                new BigDecimal("25.0"),
                new BigDecimal("5.50"),
                true,
                "Nằm trong phạm vi giao hàng"
        );
        when(deliveryService.checkDistance(lat, lng)).thenReturn(distanceResponse);

        mockMvc.perform(get("/api/v1/delivery/check-distance")
                        .param("lat", "21.380000")
                        .param("lng", "105.580000"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.statusCode").value(200))
                .andExpect(jsonPath("$.data.allowed").value(true))
                .andExpect(jsonPath("$.data.distanceKm").value(5.50));
    }
}
