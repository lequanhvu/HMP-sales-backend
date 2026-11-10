package com.hmp.sales.feature.file.service;

import com.hmp.sales.common.exception.BusinessRuleException;
import com.hmp.sales.feature.file.dto.FileUploadResponse;
import org.springframework.web.multipart.MultipartFile;

/**
 * Interface định nghĩa các phương thức lưu trữ và quản lý tệp tin.
 */
public interface FileStorageService {

    /**
     * Tải tệp hình ảnh lên bộ lưu trữ đám mây Cloudflare R2.
     *
     * @param file   tệp dữ liệu nhị phân MultipartFile từ client
     * @param folder thư mục con phân loại trên storage (ví dụ: "colors", "products", "general")
     * @return {@link FileUploadResponse} chứa thông tin tệp tải lên bao gồm URL công khai
     * @throws BusinessRuleException nếu tệp không hợp lệ hoặc cấu hình R2 chưa bật
     */
    FileUploadResponse uploadImage(MultipartFile file, String folder);
}

