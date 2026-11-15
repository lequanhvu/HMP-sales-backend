package com.hmp.sales.feature.product.dto;

import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/**
 * Request DTO phục vụ cho form-data khi tải lên hàng loạt ảnh sản phẩm trực tiếp.
 *
 * @param images            danh sách tệp ảnh tải lên (alias images)
 * @param files             danh sách tệp ảnh tải lên (alias files)
 * @param isPrimary         true nếu các ảnh này là ảnh đại diện (mặc định false)
 * @param startDisplayOrder thứ tự hiển thị bắt đầu (tùy chọn)
 */
public record ProductImageBatchUploadRequest(
        List<MultipartFile> images,

        List<MultipartFile> files,

        Boolean isPrimary,

        Integer startDisplayOrder
) {
    /**
     * Constructor thuận tiện với giá trị mặc định cho isPrimary.
     */
    public ProductImageBatchUploadRequest(List<MultipartFile> images, List<MultipartFile> files, Boolean isPrimary, Integer startDisplayOrder) {
        this.images = images;
        this.files = files;
        this.isPrimary = isPrimary != null ? isPrimary : false;
        this.startDisplayOrder = startDisplayOrder;
    }

    /**
     * Constructor thuận tiện với danh sách tệp images.
     *
     * @param images            danh sách tệp ảnh
     * @param isPrimary         true nếu là ảnh đại diện
     * @param startDisplayOrder thứ tự hiển thị bắt đầu
     */
    public ProductImageBatchUploadRequest(List<MultipartFile> images, Boolean isPrimary, Integer startDisplayOrder) {
        this(images, null, isPrimary, startDisplayOrder);
    }

    /**
     * Lấy danh sách tệp ảnh mục tiêu từ alias images hoặc files.
     *
     * @return danh sách MultipartFile hoặc rỗng nếu không có
     */
    public List<MultipartFile> resolveFiles() {
        if (images != null && !images.isEmpty()) {
            return images;
        }
        return files != null ? files : List.of();
    }
}

