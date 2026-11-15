package com.hmp.sales.feature.product.dto;

import java.util.List;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.web.multipart.MultipartFile;

/**
 * Request DTO phục vụ cho form-data khi tạo mới hoặc cập nhật màu sắc tôn sản xuất kèm tệp ảnh mẫu.
 *
 * @param colorName tên màu sắc, bắt buộc, tối đa 100 ký tự (vd: Xanh ngọc, Đỏ đậm)
 * @param image     tệp ảnh tải lên trực tiếp (tùy chọn, tối đa 5MB)
 * @param images    danh sách tệp ảnh tải lên (tùy chọn; hệ thống chỉ chấp nhận tối đa 1 ảnh)
 * @param imageUrl  đường dẫn URL ảnh có sẵn (tùy chọn khi không tải tệp)
 */
public record ColorUploadRequest(

        @NotBlank(message = "Tên màu sắc không được để trống.")
        @Size(max = 100, message = "Tên màu sắc không được vượt quá 100 ký tự.")
        String colorName,

        MultipartFile image,

        List<MultipartFile> images,

        @Size(max = 500, message = "Đường dẫn ảnh màu sắc không được vượt quá 500 ký tự.")
        String imageUrl
) {
    /**
     * Constructor thuận tiện khi tạo request với tên màu và 1 tệp ảnh đơn.
     *
     * @param colorName tên màu sắc
     * @param image     tệp ảnh tải lên
     */
    public ColorUploadRequest(String colorName, MultipartFile image) {
        this(colorName, image, null, null);
    }

    /**
     * Constructor thuận tiện khi truyền đầy đủ tệp đơn hoặc URL.
     *
     * @param colorName tên màu sắc
     * @param image     tệp ảnh tải lên
     * @param imageUrl  đường dẫn URL ảnh có sẵn
     */
    public ColorUploadRequest(String colorName, MultipartFile image, String imageUrl) {
        this(colorName, image, null, imageUrl);
    }
}

