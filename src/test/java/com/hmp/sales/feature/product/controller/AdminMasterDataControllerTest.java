package com.hmp.sales.feature.product.controller;

import com.hmp.sales.common.exception.BusinessRuleException;
import com.hmp.sales.common.exception.GlobalExceptionHandler;
import com.hmp.sales.feature.product.dto.BrandRequest;
import com.hmp.sales.feature.product.dto.BrandResponse;
import com.hmp.sales.feature.product.dto.UnitRequest;
import com.hmp.sales.feature.product.dto.UnitResponse;
import com.hmp.sales.feature.product.dto.ColorRequest;
import com.hmp.sales.feature.product.dto.ColorResponse;
import com.hmp.sales.feature.product.entity.BrandStatus;
import com.hmp.sales.feature.product.entity.UnitStatus;
import com.hmp.sales.feature.product.entity.ColorStatus;
import com.hmp.sales.feature.product.service.BrandService;
import com.hmp.sales.feature.product.service.ColorService;
import com.hmp.sales.feature.product.service.UnitService;
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
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Unit tests cho các Controller quản lý Master Data của Admin:
 * {@link AdminBrandController}, {@link AdminUnitController}, {@link AdminColorController}.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("Admin Master Data Controllers Unit Tests")
class AdminMasterDataControllerTest {

    private MockMvc mockMvcBrand;
    private MockMvc mockMvcUnit;
    private MockMvc mockMvcColor;

    @Mock
    private BrandService brandService;

    @Mock
    private UnitService unitService;

    @Mock
    private ColorService colorService;

    @InjectMocks
    private AdminBrandController brandController;

    @InjectMocks
    private AdminUnitController unitController;

    @InjectMocks
    private AdminColorController colorController;

    @BeforeEach
    void setUp() {
        mockMvcBrand = MockMvcBuilders.standaloneSetup(brandController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
        mockMvcUnit = MockMvcBuilders.standaloneSetup(unitController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
        mockMvcColor = MockMvcBuilders.standaloneSetup(colorController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    @DisplayName("POST /api/v1/admin/brands - Tạo thương hiệu 201 Created")
    void createBrand_returns201() throws Exception {
        BrandResponse response = new BrandResponse(1L, "Tôn Hoa Sen", BrandStatus.ACTIVE, null, null);
        when(brandService.createBrand(any(BrandRequest.class))).thenReturn(response);

        mockMvcBrand.perform(post("/api/v1/admin/brands")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"brandName\": \"Tôn Hoa Sen\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.statusCode").value(201))
                .andExpect(jsonPath("$.data.brandName").value("Tôn Hoa Sen"));
    }

    @Test
    @DisplayName("DELETE /api/v1/admin/brands/{id} - Còn sản phẩm liên kết trả về 400 Bad Request")
    void deleteBrand_conflict_returns400() throws Exception {
        doThrow(new BusinessRuleException("Không thể xóa vì còn sản phẩm liên kết"))
                .when(brandService).deleteBrand(1L);

        mockMvcBrand.perform(delete("/api/v1/admin/brands/1"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.statusCode").value(400))
                .andExpect(jsonPath("$.message").value("Không thể xóa vì còn sản phẩm liên kết"));
    }

    @Test
    @DisplayName("POST /api/v1/admin/units - Tạo đơn vị tính 201 Created")
    void createUnit_returns201() throws Exception {
        UnitResponse response = new UnitResponse(1L, "mét", UnitStatus.ACTIVE, null, null);
        when(unitService.createUnit(any(UnitRequest.class))).thenReturn(response);

        mockMvcUnit.perform(post("/api/v1/admin/units")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"unitName\": \"mét\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.statusCode").value(201))
                .andExpect(jsonPath("$.data.unitName").value("mét"));
    }

    @Test
    @DisplayName("GET /api/v1/admin/brands - Danh sách thương hiệu trả về format phân trang chuẩn")
    void listBrands_returnsPagedResponse() throws Exception {
        BrandResponse response = new BrandResponse(1L, "Tôn Hoa Sen", BrandStatus.ACTIVE, null, null);
        Page<BrandResponse> paged = new PageImpl<>(
                List.of(response),
                PageRequest.of(0, 20),
                1
        );
        when(brandService.searchBrands(any(), any(), eq(0), eq(20))).thenReturn(paged);

        mockMvcBrand.perform(get("/api/v1/admin/brands"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.statusCode").value(200))
                .andExpect(jsonPath("$.message").value("Thành công"))
                .andExpect(jsonPath("$.data.result[0].brandName").value("Tôn Hoa Sen"))
                .andExpect(jsonPath("$.data.meta.page").value(0))
                .andExpect(jsonPath("$.data.meta.size").value(20))
                .andExpect(jsonPath("$.data.meta.totalElements").value(1))
                .andExpect(jsonPath("$.data.meta.totalPages").value(1));
    }

    @Test
    @DisplayName("GET /api/v1/admin/units - Danh sách đơn vị tính trả về format phân trang chuẩn")
    void listUnits_returnsPagedResponse() throws Exception {
        UnitResponse response = new UnitResponse(1L, "mét", UnitStatus.ACTIVE, null, null);
        Page<UnitResponse> paged = new PageImpl<>(
                List.of(response),
                PageRequest.of(0, 20),
                1
        );
        when(unitService.searchUnits(any(), any(), eq(0), eq(20))).thenReturn(paged);

        mockMvcUnit.perform(get("/api/v1/admin/units"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.statusCode").value(200))
                .andExpect(jsonPath("$.message").value("Thành công"))
                .andExpect(jsonPath("$.data.result[0].unitName").value("mét"))
                .andExpect(jsonPath("$.data.meta.page").value(0))
                .andExpect(jsonPath("$.data.meta.size").value(20))
                .andExpect(jsonPath("$.data.meta.totalElements").value(1))
                .andExpect(jsonPath("$.data.meta.totalPages").value(1));
    }

    @Test
    @DisplayName("GET /api/v1/admin/colors - Danh sách màu sắc trả về format phân trang chuẩn")
    void listColors_returnsPagedResponse() throws Exception {
        ColorResponse response = new ColorResponse(1L, "Xanh ngọc", ColorStatus.ACTIVE, null, null);
        Page<ColorResponse> paged = new PageImpl<>(
                List.of(response),
                PageRequest.of(0, 20),
                1
        );
        when(colorService.searchColors(any(), any(), eq(0), eq(20))).thenReturn(paged);

        mockMvcColor.perform(get("/api/v1/admin/colors"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.statusCode").value(200))
                .andExpect(jsonPath("$.message").value("Thành công"))
                .andExpect(jsonPath("$.data.result[0].colorName").value("Xanh ngọc"))
                .andExpect(jsonPath("$.data.meta.page").value(0))
                .andExpect(jsonPath("$.data.meta.size").value(20))
                .andExpect(jsonPath("$.data.meta.totalElements").value(1))
                .andExpect(jsonPath("$.data.meta.totalPages").value(1));
    }

    @Test
    @DisplayName("POST /api/v1/admin/colors - Tạo màu sắc 201 Created")
    void createColor_returns201() throws Exception {
        ColorResponse response = new ColorResponse(1L, "Xanh ngọc", ColorStatus.ACTIVE, null, null);
        when(colorService.createColor(any(ColorRequest.class))).thenReturn(response);

        mockMvcColor.perform(post("/api/v1/admin/colors")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"colorName\": \"Xanh ngọc\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.statusCode").value(201))
                .andExpect(jsonPath("$.data.colorName").value("Xanh ngọc"));
    }

    @Test
    @DisplayName("POST /api/v1/admin/colors (multipart) - Tạo màu sắc kèm tệp ảnh 201 Created")
    void createColorWithMultipart_returns201() throws Exception {
        MockMultipartFile file = new MockMultipartFile("image", "color.jpg", "image/jpeg", "image-content".getBytes());
        ColorResponse response = new ColorResponse(1L, "Xanh rêu", "https://r2.cdn/color.jpg", ColorStatus.ACTIVE, null, null);
        when(colorService.createColor(eq("Xanh rêu"), any(), any())).thenReturn(response);

        mockMvcColor.perform(multipart("/api/v1/admin/colors")
                        .file(file)
                        .param("colorName", "Xanh rêu"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.statusCode").value(201))
                .andExpect(jsonPath("$.data.colorName").value("Xanh rêu"))
                .andExpect(jsonPath("$.data.imageUrl").value("https://r2.cdn/color.jpg"));
    }

    @Test
    @DisplayName("PATCH /api/v1/admin/colors/{id} (multipart) - Cập nhật màu sắc kèm tệp ảnh 200 OK")
    void updateColorWithMultipart_returns200() throws Exception {
        MockMultipartFile file = new MockMultipartFile("image", "new-color.jpg", "image/jpeg", "new-image-content".getBytes());
        ColorResponse response = new ColorResponse(1L, "Xanh lá", "https://r2.cdn/new-color.jpg", ColorStatus.ACTIVE, null, null);
        when(colorService.updateColor(eq(1L), eq("Xanh lá"), any(), any())).thenReturn(response);

        mockMvcColor.perform(multipart("/api/v1/admin/colors/1")
                        .file(file)
                        .param("colorName", "Xanh lá")
                        .with(request -> {
                            request.setMethod("PATCH");
                            return request;
                        }))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.statusCode").value(200))
                .andExpect(jsonPath("$.data.colorName").value("Xanh lá"))
                .andExpect(jsonPath("$.data.imageUrl").value("https://r2.cdn/new-color.jpg"));
    }
}
