package com.hmp.sales.feature.product.controller;

import com.hmp.sales.common.exception.GlobalExceptionHandler;
import com.hmp.sales.feature.product.dto.FeaturedProductCreateRequest;
import com.hmp.sales.feature.product.dto.FeaturedProductResponse;
import com.hmp.sales.feature.product.dto.FeaturedProductUpdateRequest;
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
import org.springframework.core.MethodParameter;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;

import java.time.Instant;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Unit tests cho {@link ManagerFeaturedProductController}.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("ManagerFeaturedProductController Unit Tests")
class ManagerFeaturedProductControllerTest {

    private MockMvc mockMvc;

    @Mock
    private FeaturedProductService featuredProductService;

    @InjectMocks
    private ManagerFeaturedProductController controller;

    private FeaturedProductResponse sampleResponse;

    @BeforeEach
    void setUp() {
        HandlerMethodArgumentResolver jwtResolver = new HandlerMethodArgumentResolver() {
            @Override
            public boolean supportsParameter(MethodParameter parameter) {
                return parameter.getParameterType().isAssignableFrom(Jwt.class);
            }

            @Override
            public Object resolveArgument(MethodParameter parameter, ModelAndViewContainer mavContainer,
                                          NativeWebRequest webRequest, WebDataBinderFactory binderFactory) {
                return Jwt.withTokenValue("mock-jwt-token")
                        .header("alg", "none")
                        .subject("1")
                        .claim("userId", 1L)
                        .build();
            }
        };

        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new GlobalExceptionHandler())
                .setCustomArgumentResolvers(jwtResolver)
                .build();

        sampleResponse = new FeaturedProductResponse(
                1L, 100L, "Tôn Hoa Sen 11 sóng", ProductStatus.ACTIVE,
                10L, "Tôn sóng vuông", 5L, "Tôn Hoa Sen", "https://r2.dev/sample.jpg",
                1, FeaturedProductStatus.ACTIVE,
                Instant.now(), Instant.now(), true
        );
    }

    @Test
    @DisplayName("GET /api/v1/manager/featured-products - Lấy danh sách thành công 200 OK")
    void getAllFeaturedProducts_returns200() throws Exception {
        when(featuredProductService.getAllFeaturedProducts()).thenReturn(List.of(sampleResponse));

        mockMvc.perform(get("/api/v1/manager/featured-products"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.statusCode").value(200))
                .andExpect(jsonPath("$.data[0].id").value(1))
                .andExpect(jsonPath("$.data[0].productName").value("Tôn Hoa Sen 11 sóng"));
    }

    @Test
    @DisplayName("GET /api/v1/manager/featured-products/{id} - Lấy chi tiết thành công 200 OK")
    void getFeaturedProductById_returns200() throws Exception {
        when(featuredProductService.getFeaturedProductById(1L)).thenReturn(sampleResponse);

        mockMvc.perform(get("/api/v1/manager/featured-products/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.statusCode").value(200))
                .andExpect(jsonPath("$.data.id").value(1))
                .andExpect(jsonPath("$.data.productId").value(100));
    }

    @Test
    @DisplayName("POST /api/v1/manager/featured-products - Thêm sản phẩm nổi bật 201 Created")
    void addFeaturedProduct_returns201() throws Exception {
        when(featuredProductService.addFeaturedProduct(any(FeaturedProductCreateRequest.class), any()))
                .thenReturn(sampleResponse);

        String json = """
                {
                    "productId": 100,
                    "displayOrder": 1,
                    "status": "ACTIVE"
                }
                """;

        mockMvc.perform(post("/api/v1/manager/featured-products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.statusCode").value(201))
                .andExpect(jsonPath("$.data.id").value(1));
    }

    @Test
    @DisplayName("POST /api/v1/manager/featured-products - Thiếu productId ném 400 Bad Request")
    void addFeaturedProduct_missingProductId_returns400() throws Exception {
        String json = """
                {
                    "displayOrder": 1
                }
                """;

        mockMvc.perform(post("/api/v1/manager/featured-products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.statusCode").value(400));
    }

    @Test
    @DisplayName("PUT /api/v1/manager/featured-products/{id} - Cập nhật 200 OK")
    void updateFeaturedProduct_returns200() throws Exception {
        when(featuredProductService.updateFeaturedProduct(eq(1L), any(FeaturedProductUpdateRequest.class), any()))
                .thenReturn(sampleResponse);

        String json = """
                {
                    "displayOrder": 3,
                    "status": "INACTIVE"
                }
                """;

        mockMvc.perform(put("/api/v1/manager/featured-products/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.statusCode").value(200))
                .andExpect(jsonPath("$.data.id").value(1));
    }

    @Test
    @DisplayName("DELETE /api/v1/manager/featured-products/{id} - Xóa mềm 200 OK")
    void deleteFeaturedProduct_returns200() throws Exception {
        mockMvc.perform(delete("/api/v1/manager/featured-products/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.statusCode").value(200));

        verify(featuredProductService).deleteFeaturedProduct(eq(1L), any());
    }
}
