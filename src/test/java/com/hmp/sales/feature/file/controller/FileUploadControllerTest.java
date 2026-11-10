package com.hmp.sales.feature.file.controller;

import com.hmp.sales.common.exception.GlobalExceptionHandler;
import com.hmp.sales.feature.file.dto.FileUploadResponse;
import com.hmp.sales.feature.file.service.FileStorageService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
@DisplayName("FileUploadController Unit Tests")
class FileUploadControllerTest {

    private MockMvc mockMvc;

    @Mock
    private FileStorageService fileStorageService;

    @InjectMocks
    private FileUploadController fileUploadController;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(fileUploadController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    @DisplayName("POST /api/v1/files/upload - Tải ảnh lên thành công trả về 200 OK")
    void uploadFile_success_returns200() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "mau-xanh.png",
                "image/png",
                new byte[]{1, 2, 3}
        );

        FileUploadResponse response = new FileUploadResponse(
                "https://pub-test.r2.dev/colors/uuid-123.png",
                "mau-xanh.png",
                "uuid-123.png",
                "image/png",
                3L
        );

        when(fileStorageService.uploadImage(any(), eq("colors"))).thenReturn(response);

        mockMvc.perform(multipart("/api/v1/files/upload")
                        .file(file)
                        .param("folder", "colors"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.statusCode").value(200))
                .andExpect(jsonPath("$.message").value("Tải tệp lên thành công."))
                .andExpect(jsonPath("$.data.fileUrl").value("https://pub-test.r2.dev/colors/uuid-123.png"))
                .andExpect(jsonPath("$.data.originalName").value("mau-xanh.png"))
                .andExpect(jsonPath("$.data.contentType").value("image/png"))
                .andExpect(jsonPath("$.data.size").value(3));
    }
}

