package com.hmp.sales.feature.product.dto;

import jakarta.validation.constraints.Size;
import org.springframework.web.multipart.MultipartFile;

/**
 * Request DTO phục vụ cho form-data khi thêm ảnh đơn cho sản phẩm bằng tệp tải lên trực tiếp.
 *
 * @param image        tệp ảnh tải lên (alias image)
 * @param file         tệp ảnh tải lên (alias file)
 * @param imageUrl     đường dẫn URL ảnh trực tiếp (tùy chọn)
 * @param isPrimary    true nếu đây là ảnh đại diện (mặc định false)
 * @param displayOrder thứ tự hiển thị (tùy chọn, mặc định 0)
 */
public record ProductImageUploadMultipartRequest(
        MultipartFile image,

        MultipartFile file,

        @Size(max = 1000, message = "Đường dẫn URL ảnh không được vượt quá 1000 ký tự.")
        String imageUrl,

        Boolean isPrimary,

        Integer displayOrder
) {
    /**
     * Constructor thuận tiện với giá trị mặc định cho isPrimary và displayOrder.
     */
    public ProductImageUploadMultipartRequest(MultipartFile image, MultipartFile file, String imageUrl, Boolean isPrimary, Integer displayOrder) {
        this.image = image;
        this.file = file;
        this.imageUrl = imageUrl;
        this.isPrimary = isPrimary != null ? isPrimary : false;
        this.displayOrder = displayOrder != null ? displayOrder : 0;
    }

    /**
     * Constructor thuận tiện khi truyền 1 tệp ảnh đơn và các cấu hình hiển thị.
     *
     * @param image        tệp ảnh tải lên
     * @param isPrimary    true nếu là ảnh đại diện
     * @param displayOrder thứ tự hiển thị
     */
    public ProductImageUploadMultipartRequest(MultipartFile image, Boolean isPrimary, Integer displayOrder) {
        this(image, null, null, isPrimary, displayOrder);
    }

    /**
     * Lấy tệp ảnh mục tiêu từ alias image hoặc file.
     *
     * @return tệp MultipartFile hoặc null nếu không có
     */
    public MultipartFile resolveFile() {
        return image != null ? image : file;
    }
}

