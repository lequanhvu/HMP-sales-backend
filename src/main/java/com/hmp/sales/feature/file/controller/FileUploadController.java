package com.hmp.sales.feature.file.controller;

import com.hmp.sales.common.exception.BusinessRuleException;
import com.hmp.sales.common.response.ApiSuccessResponse;
import com.hmp.sales.feature.file.dto.FileUploadResponse;
import com.hmp.sales.feature.file.service.FileStorageService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.multipart.MultipartHttpServletRequest;

import java.util.List;

/**
 * REST Controller tiếp nhận và xử lý tải lên tệp tin lên hệ thống lưu trữ đám mây.
 * <p>
 * Phục vụ nhu cầu tải ảnh sản phẩm, ảnh mẫu màu sắc, logo thương hiệu.
 * Yêu cầu quyền {@code FILE_UPLOAD}.
 */
@RestController
@RequestMapping("/api/v1/files")
public class FileUploadController {

    private final FileStorageService fileStorageService;

    /**
     * Khởi tạo FileUploadController với FileStorageService.
     *
     * @param fileStorageService dịch vụ lưu trữ tệp tin
     */
    @Autowired
    public FileUploadController(FileStorageService fileStorageService) {
        this.fileStorageService = fileStorageService;
    }

    /**
     * Tải tệp hình ảnh lên bộ lưu trữ đám mây Cloudflare R2.
     *
     * @param file           tệp dữ liệu nhị phân (ảnh) tải lên qua form-data
     * @param folder         thư mục phân loại tệp (tùy chọn: "colors", "products", "brands", mặc định "general")
     * @param servletRequest đối tượng HttpServletRequest để kiểm tra số lượng tệp tải lên
     * @return đối tượng phản hồi chuẩn chứa thông tin tệp và URL công khai
     */
    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiSuccessResponse<FileUploadResponse>> uploadFile(
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "folder", required = false, defaultValue = "general") String folder,
            HttpServletRequest servletRequest) {
        if (servletRequest instanceof MultipartHttpServletRequest multipartRequest) {
            int count = 0;
            for (List<MultipartFile> files : multipartRequest.getMultiFileMap().values()) {
                for (MultipartFile f : files) {
                    if (f != null && !f.isEmpty()) {
                        count++;
                    }
                }
            }
            if (count > 1) {
                throw new BusinessRuleException("Mỗi lần tải lên chỉ được phép chọn tối đa 1 tệp tin.");
            }
        }

        FileUploadResponse response = fileStorageService.uploadImage(file, folder);
        return ResponseEntity.ok(ApiSuccessResponse.success("Tải tệp lên thành công.", response));
    }
}
