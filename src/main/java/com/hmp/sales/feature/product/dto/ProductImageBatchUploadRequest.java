package com.hmp.sales.feature.product.dto;

import org.springframework.web.multipart.MultipartFile;

import java.util.ArrayList;
import java.util.List;

/**
 * Request DTO phục vụ cho form-data khi tải lên hàng loạt ảnh sản phẩm trực tiếp.
 *
 * @param image             danh sách tệp ảnh tải lên (alias image)
 * @param images            danh sách tệp ảnh tải lên (alias images)
 * @param file              danh sách tệp ảnh tải lên (alias file)
 * @param files             danh sách tệp ảnh tải lên (alias files)
 * @param isPrimary         true nếu các ảnh này là ảnh đại diện (mặc định false)
 * @param startDisplayOrder thứ tự hiển thị bắt đầu (tùy chọn)
 */
public record ProductImageBatchUploadRequest(
        List<MultipartFile> image,

        List<MultipartFile> images,

        List<MultipartFile> file,

        List<MultipartFile> files,

        Boolean isPrimary,

        Integer startDisplayOrder
) {
    /**
     * Constructor thuận tiện với giá trị mặc định cho isPrimary.
     */
    public ProductImageBatchUploadRequest(List<MultipartFile> image, List<MultipartFile> images,
                                          List<MultipartFile> file, List<MultipartFile> files,
                                          Boolean isPrimary, Integer startDisplayOrder) {
        this.image = image;
        this.images = images;
        this.file = file;
        this.files = files;
        this.isPrimary = isPrimary != null ? isPrimary : false;
        this.startDisplayOrder = startDisplayOrder;
    }

    /**
     * Constructor tương thích ngược với 2 alias images và files.
     */
    public ProductImageBatchUploadRequest(List<MultipartFile> images, List<MultipartFile> files,
                                          Boolean isPrimary, Integer startDisplayOrder) {
        this(null, images, null, files, isPrimary, startDisplayOrder);
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
     * Lấy danh sách toàn bộ các tệp ảnh mục tiêu từ tất cả alias không rỗng.
     *
     * @return danh sách MultipartFile hoặc rỗng nếu không có
     */
    public List<MultipartFile> resolveFiles() {
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
}
