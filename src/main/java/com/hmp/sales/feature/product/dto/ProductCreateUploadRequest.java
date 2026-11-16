package com.hmp.sales.feature.product.dto;

import com.hmp.sales.common.exception.BusinessRuleException;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Size;
import org.springframework.web.multipart.MultipartFile;

import java.util.ArrayList;
import java.util.List;

/**
 * Request DTO phục vụ cho form-data khi tạo mới sản phẩm trong danh mục kèm tệp ảnh đại diện và ảnh phụ (1 bước duy nhất).
 *
 * @param product         thông tin chi tiết sản phẩm (dạng nested DTO nếu gửi qua multipart)
 * @param productName     tên sản phẩm (tùy chọn nếu đã có trong product, tối đa 250 ký tự)
 * @param description     mô tả chi tiết sản phẩm (tùy chọn)
 * @param brandId         ID thương hiệu (tùy chọn)
 * @param primaryImage    danh sách tệp ảnh đại diện (alias primaryImage)
 * @param primaryImages   danh sách tệp ảnh đại diện (alias primaryImages)
 * @param secondaryImages danh sách tệp ảnh phụ tải lên trực tiếp (tùy chọn)
 */
public record ProductCreateUploadRequest(
        @Valid
        ProductCreateRequest product,

        @Size(max = 250, message = "Tên sản phẩm không được vượt quá 250 ký tự.")
        String productName,

        String description,

        Long brandId,

        List<MultipartFile> primaryImage,

        List<MultipartFile> primaryImages,

        List<MultipartFile> secondaryImages
) {
    /**
     * Constructor tương thích khi truyền 1 ảnh đại diện đơn lẻ.
     */
    public ProductCreateUploadRequest(ProductCreateRequest product, String productName, String description, Long brandId,
                                      MultipartFile primaryImage, List<MultipartFile> secondaryImages) {
        this(product, productName, description, brandId,
                primaryImage != null ? List.of(primaryImage) : null,
                null,
                secondaryImages);
    }

    /**
     * Constructor thuận tiện khi tạo sản phẩm bằng form fields và file ảnh đơn lẻ.
     *
     * @param productName     tên sản phẩm
     * @param description     mô tả chi tiết
     * @param brandId         ID thương hiệu
     * @param primaryImage    tệp ảnh đại diện
     * @param secondaryImages danh sách tệp ảnh phụ
     */
    public ProductCreateUploadRequest(String productName, String description, Long brandId,
                                      MultipartFile primaryImage, List<MultipartFile> secondaryImages) {
        this(null, productName, description, brandId, primaryImage, secondaryImages);
    }

    /**
     * Thu thập danh sách các tệp ảnh đại diện hợp lệ không rỗng.
     *
     * @return danh sách MultipartFile ảnh đại diện
     */
    public List<MultipartFile> allPrimaryImages() {
        List<MultipartFile> result = new ArrayList<>();
        collectNonEmpty(result, primaryImage);
        collectNonEmpty(result, primaryImages);
        return result;
    }

    /**
     * Lấy tệp ảnh đại diện mục tiêu (ưu tiên tệp đầu tiên).
     *
     * @return MultipartFile hoặc null nếu không có
     */
    public MultipartFile resolvePrimaryImage() {
        List<MultipartFile> all = allPrimaryImages();
        return all.isEmpty() ? null : all.get(0);
    }

    /**
     * Thu thập danh sách các tệp ảnh phụ hợp lệ không rỗng.
     *
     * @return danh sách MultipartFile ảnh phụ
     */
    public List<MultipartFile> allSecondaryImages() {
        List<MultipartFile> result = new ArrayList<>();
        collectNonEmpty(result, secondaryImages);
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
     * Trích xuất thông tin sản phẩm cơ bản dưới dạng {@link ProductCreateRequest}.
     *
     * @return ProductCreateRequest hợp lệ
     * @throws BusinessRuleException nếu không có thông tin sản phẩm hoặc tên sản phẩm rỗng
     */
    public ProductCreateRequest toProductCreateRequest() {
        if (product != null) {
            return product;
        }
        if (productName == null || productName.isBlank()) {
            throw new BusinessRuleException("Tên sản phẩm không được để trống.");
        }
        return new ProductCreateRequest(productName.trim(), description, brandId, List.of(), List.of());
    }
}
