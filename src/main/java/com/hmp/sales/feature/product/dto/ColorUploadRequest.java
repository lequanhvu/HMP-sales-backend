package com.hmp.sales.feature.product.dto;

import java.util.ArrayList;
import java.util.List;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.web.multipart.MultipartFile;

/**
 * Request DTO phục vụ cho form-data khi tạo mới hoặc cập nhật màu sắc tôn sản xuất kèm tệp ảnh mẫu.
 * <p>
 * Hỗ trợ tiếp nhận cả tệp đơn lẫn danh sách tệp qua các alias phổ biến (image, images, file, files)
 * nhằm bảo đảm phát hiện chính xác khi client gửi nhiều hơn 1 tệp ảnh.
 *
 * @param colorName tên màu sắc, bắt buộc, tối đa 100 ký tự (vd: Xanh ngọc, Đỏ đậm)
 * @param image     danh sách tệp ảnh từ trường 'image'
 * @param images    danh sách tệp ảnh từ trường 'images'
 * @param file      danh sách tệp ảnh từ trường 'file'
 * @param files     danh sách tệp ảnh từ trường 'files'
 * @param imageUrl  đường dẫn URL ảnh có sẵn (tùy chọn khi không tải tệp)
 */
public record ColorUploadRequest(

        @NotBlank(message = "Tên màu sắc không được để trống.")
        @Size(max = 100, message = "Tên màu sắc không được vượt quá 100 ký tự.")
        String colorName,

        List<MultipartFile> image,

        List<MultipartFile> images,

        List<MultipartFile> file,

        List<MultipartFile> files,

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
        this(colorName, image != null ? List.of(image) : null, null, null, null, null);
    }

    /**
     * Constructor thuận tiện khi truyền đầy đủ tệp đơn hoặc URL.
     *
     * @param colorName tên màu sắc
     * @param image     tệp ảnh tải lên
     * @param imageUrl  đường dẫn URL ảnh có sẵn
     */
    public ColorUploadRequest(String colorName, MultipartFile image, String imageUrl) {
        this(colorName, image != null ? List.of(image) : null, null, null, null, imageUrl);
    }

    /**
     * Constructor thuận tiện khi truyền tệp đơn, danh sách tệp hoặc URL.
     *
     * @param colorName tên màu sắc
     * @param image     tệp ảnh đơn
     * @param images    danh sách tệp ảnh
     * @param imageUrl  đường dẫn URL ảnh
     */
    public ColorUploadRequest(String colorName, MultipartFile image, List<MultipartFile> images, String imageUrl) {
        this(colorName, image != null ? List.of(image) : null, images, null, null, imageUrl);
    }

    /**
     * Thu thập toàn bộ các tệp ảnh hợp lệ (không rỗng) được gửi lên qua mọi trường.
     *
     * @return danh sách tệp ảnh không rỗng
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
     * Lấy tệp ảnh duy nhất nếu có.
     *
     * @return tệp ảnh hoặc null nếu không gửi tệp
     */
    public MultipartFile resolveImage() {
        List<MultipartFile> all = allUploadedFiles();
        return all.isEmpty() ? null : all.get(0);
    }
}
