package com.hmp.sales.feature.file.service;

import com.hmp.sales.common.exception.BusinessRuleException;
import com.hmp.sales.config.AppProperties;
import com.hmp.sales.feature.file.dto.FileUploadResponse;
import com.hmp.sales.feature.file.service.impl.R2FileStorageServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectResponse;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("R2FileStorageServiceImpl Unit Tests")
class R2FileStorageServiceImplTest {

    @Mock
    private S3Client s3Client;

    private AppProperties.R2Properties enabledR2Props;
    private AppProperties.R2Properties disabledR2Props;

    @BeforeEach
    void setUp() {
        enabledR2Props = new AppProperties.R2Properties(
                true,
                "test-account-id",
                "test-access-key",
                "test-secret-key",
                "test-bucket",
                "https://pub-test.r2.dev"
        );

        disabledR2Props = new AppProperties.R2Properties(
                false,
                "",
                "",
                "",
                "",
                ""
        );
    }

    private AppProperties createAppProperties(AppProperties.R2Properties r2Props) {
        return new AppProperties(
                null, null, null, null, null, null, null,
                new AppProperties.StorageProperties(r2Props)
        );
    }

    @Test
    @DisplayName("Ném lỗi khi dịch vụ R2 chưa được bật (enabled = false)")
    void uploadImage_r2Disabled_throwsException() {
        R2FileStorageServiceImpl service = new R2FileStorageServiceImpl(s3Client, createAppProperties(disabledR2Props));
        MockMultipartFile file = new MockMultipartFile("file", "test.png", "image/png", new byte[]{1, 2, 3});

        assertThatThrownBy(() -> service.uploadImage(file, "colors"))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("chưa được kích hoạt");
    }

    @Test
    @DisplayName("Ném lỗi khi tệp tải lên rỗng")
    void uploadImage_emptyFile_throwsException() {
        R2FileStorageServiceImpl service = new R2FileStorageServiceImpl(s3Client, createAppProperties(enabledR2Props));
        MockMultipartFile file = new MockMultipartFile("file", "test.png", "image/png", new byte[0]);

        assertThatThrownBy(() -> service.uploadImage(file, "colors"))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("không được để trống");
    }

    @Test
    @DisplayName("Ném lỗi khi kích thước tệp vượt quá 5MB")
    void uploadImage_fileTooLarge_throwsException() {
        R2FileStorageServiceImpl service = new R2FileStorageServiceImpl(s3Client, createAppProperties(enabledR2Props));
        byte[] largeBytes = new byte[5 * 1024 * 1024 + 1];
        MockMultipartFile file = new MockMultipartFile("file", "large.png", "image/png", largeBytes);

        assertThatThrownBy(() -> service.uploadImage(file, "products"))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("vượt quá giới hạn tối đa cho phép (5MB)");
    }

    @Test
    @DisplayName("Ném lỗi khi định dạng tệp không được hỗ trợ (ví dụ PDF)")
    void uploadImage_unsupportedContentType_throwsException() {
        R2FileStorageServiceImpl service = new R2FileStorageServiceImpl(s3Client, createAppProperties(enabledR2Props));
        MockMultipartFile file = new MockMultipartFile("file", "doc.pdf", "application/pdf", new byte[]{1, 2, 3});

        assertThatThrownBy(() -> service.uploadImage(file, "colors"))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("Định dạng tệp không được hỗ trợ");
    }

    @Test
    @DisplayName("Tải tệp ảnh PNG thành công lên Cloudflare R2")
    void uploadImage_pngSuccess() {
        R2FileStorageServiceImpl service = new R2FileStorageServiceImpl(s3Client, createAppProperties(enabledR2Props));
        MockMultipartFile file = new MockMultipartFile("file", "mau-ton-xanh.png", "image/png", "sample content".getBytes());

        when(s3Client.putObject(any(PutObjectRequest.class), any(RequestBody.class)))
                .thenReturn(PutObjectResponse.builder().build());

        FileUploadResponse response = service.uploadImage(file, "colors");

        assertThat(response).isNotNull();
        assertThat(response.originalName()).isEqualTo("mau-ton-xanh.png");
        assertThat(response.contentType()).isEqualTo("image/png");
        assertThat(response.size()).isEqualTo("sample content".getBytes().length);
        assertThat(response.fileUrl()).startsWith("https://pub-test.r2.dev/colors/");
        assertThat(response.fileUrl()).endsWith(".png");

        verify(s3Client).putObject(any(PutObjectRequest.class), any(RequestBody.class));
    }

    @Test
    @DisplayName("Tải tệp ảnh JPEG thành công với thư mục mặc định 'general'")
    void uploadImage_jpegDefaultFolderSuccess() {
        R2FileStorageServiceImpl service = new R2FileStorageServiceImpl(s3Client, createAppProperties(enabledR2Props));
        MockMultipartFile file = new MockMultipartFile("file", "ton-do.jpeg", "image/jpeg", "sample jpeg".getBytes());

        when(s3Client.putObject(any(PutObjectRequest.class), any(RequestBody.class)))
                .thenReturn(PutObjectResponse.builder().build());

        FileUploadResponse response = service.uploadImage(file, null);

        assertThat(response).isNotNull();
        assertThat(response.originalName()).isEqualTo("ton-do.jpeg");
        assertThat(response.fileUrl()).startsWith("https://pub-test.r2.dev/general/");
        assertThat(response.fileUrl()).endsWith(".jpeg");
    }
}

