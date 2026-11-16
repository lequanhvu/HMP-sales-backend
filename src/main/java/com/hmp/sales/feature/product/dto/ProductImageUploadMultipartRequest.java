package com.hmp.sales.feature.product.dto;

import jakarta.validation.constraints.Size;
import org.springframework.web.multipart.MultipartFile;

import java.util.ArrayList;
import java.util.List;

/**
 * Request DTO phục vụ cho form-data khi thêm ảnh đơn cho sản phẩm bằng tệp tải lên trực tiếp.
 *
 * @param image        danh sách tệp ảnh tải lên (alias image)
 * @param images       danh sách tệp ảnh tải lên (alias images)
 * @param file         danh sách tệp ảnh tải lên (alias file)
 * @param files        danh sách tệp ảnh tải lên (alias files)
 * @param imageUrl     đường dẫn URL ảnh trực tiếp (tùy chọn)
 * @param isPrimary    true nếu đây là ảnh đại diện (mặc định false)
 * @param displayOrder thứ tự hiển thị (tùy chọn, mặc định 0)
 */
public record ProductImageUploadMultipartRequest(
        List<MultipartFile> image,

        List<MultipartFile> images,

        List<MultipartFile> file,

        List<MultipartFile> files,

        @Size(max = 1000, message = "Đường dẫn URL ảnh không được vượt quá 1000 ký tự.")
        String imageUrl,

        Boolean isPrimary,

        Integer displayOrder
) {
    /**
     * Constructor thuận tiện với giá trị mặc định cho isPrimary và displayOrder.
     */
    public ProductImageUploadMultipartRequest(List<MultipartFile> image, List<MultipartFile> images,
                                              List<MultipartFile> file, List<MultipartFile> files,
                                              String imageUrl, Boolean isPrimary, Integer displayOrder) {
        this.image = image;
        this.images = images;
        this.file = file;
        this.files = files;
        this.imageUrl = imageUrl;
        this.isPrimary = isPrimary != null ? isPrimary : false;
        this.displayOrder = displayOrder != null ? displayOrder : 0;
    }

    /**
     * Constructor tương thích ngược cho việc khởi tạo với MultipartFile đơn lẻ.
     */
    public ProductImageUploadMultipartRequest(MultipartFile image, MultipartFile file, String imageUrl,
                                              Boolean isPrimary, Integer displayOrder) {
        this(image != null ? List.of(image) : null,
                null,
                file != null ? List.of(file) : null,
                null,
                imageUrl, isPrimary, displayOrder);
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
     * Thu thập toàn bộ các tệp ảnh hợp lệ (không rỗng) từ các trường alias.
     *
     * @return danh sách các tệp tải lên hợp lệ
     */
    public List<MultipartFile> allUploadedFiles() {
        List<MultipartFile> result = new ArrayList<>();
        collectNonEmpty(result, image);
        collectNonEmpty(result, images);
        collectNonEmpty(result, file);
        collectNonEmpty(result, files);
        return result;
    }

    private static void collectNonEmpty(List<MultipartFile> target, List<MultipartFile> source) {
        if (source != null) {
            for (MultipartFile f : source) {
                if (f != null && !f.isEmpty()) {
                    target.add(f);
                }
            }
        }
    }

    /**
     * Lấy tệp ảnh mục tiêu từ alias image hoặc file (ưu tiên tệp đầu tiên).
     *
     * @return tệp MultipartFile hoặc null nếu không có
     */
    public MultipartFile resolveFile() {
        List<MultipartFile> all = allUploadedFiles();
        return all.isEmpty() ? null : all.get(0);
    }
}
