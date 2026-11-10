package com.hmp.sales.feature.product.service;

import com.hmp.sales.common.exception.BusinessRuleException;
import com.hmp.sales.feature.file.dto.FileUploadResponse;
import com.hmp.sales.feature.file.service.FileStorageService;
import com.hmp.sales.feature.product.dto.ColorRequest;
import com.hmp.sales.feature.product.dto.ColorResponse;
import com.hmp.sales.feature.product.entity.Color;
import com.hmp.sales.feature.product.entity.ColorStatus;
import com.hmp.sales.feature.product.repository.ColorRepository;
import com.hmp.sales.feature.product.service.impl.ColorServiceImpl;
import com.hmp.sales.feature.systemlog.service.SystemLogService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.mock.web.MockMultipartFile;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * Unit tests cho {@link ColorServiceImpl}.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("ColorServiceImpl Unit Tests")
class ColorServiceImplTest {

    @Mock
    private ColorRepository colorRepository;

    @Mock
    private SystemLogService systemLogService;

    @Mock
    private FileStorageService fileStorageService;

    @InjectMocks
    private ColorServiceImpl colorService;

    private Color color;

    @BeforeEach
    void setUp() {
        color = Color.builder()
                .id(1L)
                .colorName("Xanh ngọc")
                .status(ColorStatus.ACTIVE)
                .isDeleted(false)
                .build();
    }

    @Test
    @DisplayName("searchColors - Phân trang màu sắc thành công")
    void searchColors_returnsPage() {
        when(colorRepository.searchColors(eq("ngoc"), eq(ColorStatus.ACTIVE), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(color)));

        Page<ColorResponse> page = colorService.searchColors("ngọc", "ACTIVE", 0, 10);

        assertThat(page.getTotalElements()).isEqualTo(1);
        assertThat(page.getContent().get(0).colorName()).isEqualTo("Xanh ngọc");
    }

    @Test
    @DisplayName("createColor - Tạo mới màu sắc thành công")
    void createColor_success() {
        ColorRequest request = new ColorRequest("Đỏ đậm");
        when(colorRepository.existsByColorNameAndIsDeletedFalse("Đỏ đậm")).thenReturn(false);
        when(colorRepository.save(any(Color.class))).thenAnswer(inv -> {
            Color c = inv.getArgument(0);
            c.setId(2L);
            return c;
        });

        ColorResponse response = colorService.createColor(request);

        assertThat(response).isNotNull();
        assertThat(response.colorName()).isEqualTo("Đỏ đậm");
    }

    @Test
    @DisplayName("createColor - Tạo mới màu sắc kèm ảnh thành công")
    void createColor_withImage_success() {
        ColorRequest request = new ColorRequest("Đỏ đậm", "https://cdn.example.com/do-dam.png");
        when(colorRepository.existsByColorNameAndIsDeletedFalse("Đỏ đậm")).thenReturn(false);
        when(colorRepository.save(any(Color.class))).thenAnswer(inv -> {
            Color c = inv.getArgument(0);
            c.setId(2L);
            return c;
        });

        ColorResponse response = colorService.createColor(request);

        assertThat(response).isNotNull();
        assertThat(response.colorName()).isEqualTo("Đỏ đậm");
        assertThat(response.imageUrl()).isEqualTo("https://cdn.example.com/do-dam.png");
    }

    @Test
    @DisplayName("createColor - Tên màu sắc trùng ném BusinessRuleException")
    void createColor_duplicate_throwsException() {
        ColorRequest request = new ColorRequest("Xanh ngọc");
        when(colorRepository.existsByColorNameAndIsDeletedFalse("Xanh ngọc")).thenReturn(true);

        assertThatThrownBy(() -> colorService.createColor(request))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("đã tồn tại");
    }

    @Test
    @DisplayName("deleteColor - Còn biến thể liên kết ném BusinessRuleException")
    void deleteColor_hasVariants_throwsException() {
        when(colorRepository.findByIdAndIsDeletedFalse(1L)).thenReturn(Optional.of(color));
        when(colorRepository.hasActiveVariants(1L)).thenReturn(true);

        assertThatThrownBy(() -> colorService.deleteColor(1L))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("biến thể sản phẩm đang liên kết");
    }

    @Test
    @DisplayName("deleteColor - Không có biến thể liên kết xóa mềm thành công")
    void deleteColor_noVariants_softDeletes() {
        when(colorRepository.findByIdAndIsDeletedFalse(1L)).thenReturn(Optional.of(color));
        when(colorRepository.hasActiveVariants(1L)).thenReturn(false);

        colorService.deleteColor(1L);

        assertThat(color.getIsDeleted()).isTrue();
        verify(colorRepository).save(color);
    }

    @Test
    @DisplayName("createColor (Multipart) - Tải tệp ảnh trực tiếp và tạo màu sắc thành công")
    void createColor_withMultipartFile_success() {
        MockMultipartFile file = new MockMultipartFile("image", "color-sample.jpg", "image/jpeg", "fake-image".getBytes());
        FileUploadResponse uploadResponse = new FileUploadResponse(
                "https://pub-r2.test/colors/sample.jpg",
                "color-sample.jpg",
                "colors/sample.jpg",
                "image/jpeg",
                10L
        );
        when(fileStorageService.uploadImage(file, "colors")).thenReturn(uploadResponse);
        when(colorRepository.existsByColorNameAndIsDeletedFalse("Xanh rêu")).thenReturn(false);
        when(colorRepository.save(any(Color.class))).thenAnswer(inv -> {
            Color c = inv.getArgument(0);
            c.setId(10L);
            return c;
        });

        ColorResponse response = colorService.createColor("Xanh rêu", file, null);

        assertThat(response).isNotNull();
        assertThat(response.colorName()).isEqualTo("Xanh rêu");
        assertThat(response.imageUrl()).isEqualTo("https://pub-r2.test/colors/sample.jpg");
        verify(fileStorageService).uploadImage(file, "colors");
    }

    @Test
    @DisplayName("createColor (Multipart) - Khi FileStorageService null ném BusinessRuleException")
    void createColor_withMultipartFile_whenFileStorageNull_throwsException() {
        ColorServiceImpl serviceWithoutStorage = new ColorServiceImpl(colorRepository, systemLogService);
        MockMultipartFile file = new MockMultipartFile("image", "color.jpg", "image/jpeg", "fake".getBytes());

        assertThatThrownBy(() -> serviceWithoutStorage.createColor("Xanh rêu", file, null))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("Dịch vụ lưu trữ tệp chưa sẵn sàng");
    }

    @Test
    @DisplayName("updateColor (Multipart) - Tải tệp ảnh mới và cập nhật màu sắc thành công")
    void updateColor_withMultipartFile_success() {
        MockMultipartFile file = new MockMultipartFile("image", "new-color.jpg", "image/jpeg", "fake".getBytes());
        FileUploadResponse uploadResponse = new FileUploadResponse(
                "https://pub-r2.test/colors/new-color.jpg",
                "new-color.jpg",
                "colors/new-color.jpg",
                "image/jpeg",
                4L
        );
        when(colorRepository.findByIdAndIsDeletedFalse(1L)).thenReturn(Optional.of(color));
        when(colorRepository.existsByColorNameAndIsDeletedFalseAndIdNot("Xanh lá", 1L)).thenReturn(false);
        when(fileStorageService.uploadImage(file, "colors")).thenReturn(uploadResponse);
        when(colorRepository.save(any(Color.class))).thenAnswer(inv -> inv.getArgument(0));

        ColorResponse response = colorService.updateColor(1L, "Xanh lá", file, null);

        assertThat(response).isNotNull();
        assertThat(response.colorName()).isEqualTo("Xanh lá");
        assertThat(response.imageUrl()).isEqualTo("https://pub-r2.test/colors/new-color.jpg");
    }
}
