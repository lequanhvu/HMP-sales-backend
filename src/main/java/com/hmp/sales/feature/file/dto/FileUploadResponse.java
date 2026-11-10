package com.hmp.sales.feature.file.dto;

/**
 * Response DTO trả về sau khi tải tệp lên hệ thống lưu trữ thành công.
 *
 * @param fileUrl      đường dẫn URL công khai của tệp để nhúng vào sản phẩm/màu sắc
 * @param originalName tên gốc ban đầu của tệp phía máy khách
 * @param storedName   tên định danh ngẫu nhiên (UUID) của tệp trên bộ lưu trữ
 * @param contentType  loại MIME của tệp (ví dụ: image/jpeg, image/png, image/webp)
 * @param size         kích thước tệp tính theo đơn vị byte
 */
public record FileUploadResponse(
        String fileUrl,
        String originalName,
        String storedName,
        String contentType,
        long size
) {}

