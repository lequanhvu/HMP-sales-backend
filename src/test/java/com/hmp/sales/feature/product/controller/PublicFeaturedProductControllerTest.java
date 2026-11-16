package com.hmp.sales.feature.product.controller;

import com.hmp.sales.common.exception.GlobalExceptionHandler;
import com.hmp.sales.feature.product.dto.FeaturedProductResponse;
import com.hmp.sales.feature.product.entity.FeaturedProductStatus;
import com.hmp.sales.feature.product.entity.ProductStatus;
import com.hmp.sales.feature.product.service.FeaturedProductService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.Instant;
import java.util.List;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Unit tests cho {@link PublicFeaturedProductController}.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("PublicFeaturedProductController Unit Tests")
class PublicFeaturedProductControllerTest {

    private MockMvc mockMvc;

    @Mock
    private FeaturedProductService featuredProductService;

    @InjectMocks
    private PublicFeaturedProductController controller;

    private FeaturedProductResponse sampleResponse;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();

        sampleResponse = new FeaturedProductResponse(
                1L, 100L, "Tôn Hoa Sen 11 sóng", ProductStatus.ACTIVE,
                10L, "Tôn sóng vuông", 5L, "Tôn Hoa Sen", "https://r2.dev/sample.jpg",
                1, FeaturedProductStatus.ACTIVE,
                Instant.now(), Instant.now(), true
        );
    }

    @Test
    @DisplayName("GET /api/v1/featured-products - Lấy danh sách sản phẩm nổi bật công khai 200 OK")
    void getFeaturedProducts_returns200() throws Exception {
        when(featuredProductService.getPublicFeaturedProducts()).thenReturn(List.of(sampleResponse));

        mockMvc.perform(get("/api/v1/featured-products"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.statusCode").value(200))
                .andExpect(jsonPath("$.data[0].id").value(1))
                .andExpect(jsonPath("$.data[0].productName").value("Tôn Hoa Sen 11 sóng"))
                .andExpect(jsonPath("$.data[0].priceVisible").value(true));
    }
}
