package com.hmp.sales.feature.file.service.impl;

import com.hmp.sales.common.exception.BusinessRuleException;
import com.hmp.sales.config.AppProperties;
import com.hmp.sales.feature.file.dto.FileUploadResponse;
import com.hmp.sales.feature.file.service.FileStorageService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

import java.io.IOException;
import java.util.Set;
import java.util.UUID;

/**
 * Triển khai dịch vụ lưu trữ tệp tin sử dụng Cloudflare R2 Object Storage.
 * <p>
 * Thực hiện xác thực kích thước, định dạng tệp ảnh, tạo định danh ngẫu nhiên (UUID),
 * đẩy luồng tệp lên Cloudflare R2 bucket và tạo đường dẫn URL truy cập công khai.
 */
@Slf4j
@Service
public class R2FileStorageServiceImpl implements FileStorageService {

    /**
     * Giới hạn kích thước tệp tối đa: 5MB (5 * 1024 * 1024 bytes).
     */
    private static final long MAX_FILE_SIZE = 5 * 1024 * 1024L;

    /**
     * Danh sách các loại MIME ảnh được phép tải lên.
     */
    private static final Set<String> ALLOWED_CONTENT_TYPES = Set.of(
            "image/jpeg",
            "image/png",
            "image/webp",
            "image/gif"
    );

    private final S3Client s3Client;
    private final AppProperties appProperties;

    /**
     * Khởi tạo dịch vụ R2FileStorageServiceImpl.
     *
     * @param s3Client      client kết nối AWS S3 trỏ tới Cloudflare R2 (có thể null nếu chưa bật)
     * @param appProperties cấu hình ứng dụng chứa các thuộc tính lưu trữ
     */
    @Autowired
    public R2FileStorageServiceImpl(
            @Autowired(required = false) S3Client s3Client,
            AppProperties appProperties) {
        this.s3Client = s3Client;
        this.appProperties = appProperties;
    }

    /**
     * {@inheritDoc}
     *
     * @param file   tệp dữ liệu nhị phân MultipartFile từ client
     * @param folder thư mục con phân loại trên storage (ví dụ: "colors", "products", "general")
     * @return {@link FileUploadResponse} chứa thông tin tệp tải lên bao gồm URL công khai
     * @throws BusinessRuleException nếu tệp không hợp lệ hoặc dịch vụ R2 chưa được kích hoạt
     */
    @Override
    public FileUploadResponse uploadImage(MultipartFile file, String folder) {
        AppProperties.R2Properties r2 = appProperties.storage().r2();
        if (!r2.enabled() || s3Client == null) {
            log.warn("[R2FileStorage] Yêu cầu tải tệp thất bại do Cloudflare R2 chưa được kích hoạt (app.storage.r2.enabled = false).");
            throw new BusinessRuleException("Dịch vụ lưu trữ đám mây Cloudflare R2 chưa được kích hoạt. Vui lòng kiểm tra cấu hình hệ thống.");
        }

        validateFile(file);

        String originalFilename = file.getOriginalFilename() != null ? file.getOriginalFilename().trim() : "file";
        String extension = extractExtension(originalFilename);
        String uniqueFileName = UUID.randomUUID().toString() + extension;

        String sanitizedFolder = sanitizeFolder(folder);
        String objectKey = sanitizedFolder.isEmpty() ? uniqueFileName : sanitizedFolder + "/" + uniqueFileName;

        try {
            PutObjectRequest putRequest = PutObjectRequest.builder()
                    .bucket(r2.bucketName())
                    .key(objectKey)
                    .contentType(file.getContentType())
                    .contentLength(file.getSize())
                    .build();

            s3Client.putObject(putRequest, RequestBody.fromInputStream(file.getInputStream(), file.getSize()));
            log.info("[R2FileStorage] Tải tệp thành công lên R2: bucket={}, key={}", r2.bucketName(), objectKey);

            String publicDomain = r2.publicDomain() != null ? r2.publicDomain().replaceAll("/+$", "") : "";
            String fileUrl = publicDomain + "/" + objectKey;

            return new FileUploadResponse(
                    fileUrl,
                    originalFilename,
                    uniqueFileName,
                    file.getContentType(),
                    file.getSize()
            );
        } catch (IOException e) {
            log.error("[R2FileStorage] Lỗi khi đọc dữ liệu tệp '{}': {}", originalFilename, e.getMessage(), e);
            throw new BusinessRuleException("Không thể đọc dữ liệu tệp tải lên: " + e.getMessage());
        } catch (Exception e) {
            log.error("[R2FileStorage] Lỗi khi tải tệp lên Cloudflare R2: {}", e.getMessage(), e);
            throw new BusinessRuleException("Lỗi máy chủ khi tải tệp lên Cloudflare R2: " + e.getMessage());
        }
    }

    /**
     * Xác thực tính hợp lệ của tệp tải lên (rỗng, kích thước, định dạng MIME).
     *
     * @param file tệp cần kiểm tra
     * @throws BusinessRuleException nếu tệp vi phạm quy tắc
     */
    private void validateFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BusinessRuleException("Tệp tải lên không được để trống.");
        }

        if (file.getSize() > MAX_FILE_SIZE) {
            throw new BusinessRuleException("Kích thước tệp vượt quá giới hạn tối đa cho phép (5MB).");
        }

        String contentType = file.getContentType();
        if (contentType == null || !ALLOWED_CONTENT_TYPES.contains(contentType.toLowerCase())) {
            throw new BusinessRuleException("Định dạng tệp không được hỗ trợ. Chỉ chấp nhận các định dạng ảnh: JPEG, PNG, WEBP, GIF.");
        }
    }

    /**
     * Trích xuất phần mở rộng của tệp (.jpg, .png,...).
     *
     * @param filename tên tệp
     * @return phần mở rộng bao gồm dấu chấm, hoặc chuỗi rỗng nếu không có
     */
    private String extractExtension(String filename) {
        int dotIndex = filename.lastIndexOf('.');
        if (dotIndex >= 0 && dotIndex < filename.length() - 1) {
            return filename.substring(dotIndex).toLowerCase();
        }
        return "";
    }

    /**
     * Làm sạch tên thư mục để tránh tấn công Path Traversal.
     *
     * @param folder tên thư mục do người dùng truyền vào
     * @return tên thư mục đã chuẩn hóa chỉ chứa ký tự an toàn
     */
    private String sanitizeFolder(String folder) {
        if (folder == null || folder.isBlank()) {
            return "general";
        }
        return folder.trim().replaceAll("[^a-zA-Z0-9_-]", "");
    }
}

