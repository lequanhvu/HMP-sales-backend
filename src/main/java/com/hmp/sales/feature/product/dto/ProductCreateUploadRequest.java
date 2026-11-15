package com.hmp.sales.feature.product.dto;

import com.hmp.sales.common.exception.BusinessRuleException;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Size;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/**
 * Request DTO phục vụ cho form-data khi tạo mới sản phẩm trong danh mục kèm tệp ảnh đại diện và ảnh phụ (1 bước duy nhất).
 *
 * @param product         thông tin chi tiết sản phẩm (dạng nested DTO nếu gửi qua multipart)
 * @param productName     tên sản phẩm (tùy chọn nếu đã có trong product, tối đa 250 ký tự)
 * @param description     mô tả chi tiết sản phẩm (tùy chọn)
 * @param brandId         ID thương hiệu (tùy chọn)
 * @param primaryImage    tệp ảnh đại diện tải lên trực tiếp (tùy chọn)
 * @param secondaryImages danh sách tệp ảnh phụ tải lên trực tiếp (tùy chọn)
 */
public record ProductCreateUploadRequest(
        @Valid
        ProductCreateRequest product,

        @Size(max = 250, message = "Tên sản phẩm không được vượt quá 250 ký tự.")
        String productName,

        String description,

        Long brandId,

        MultipartFile primaryImage,

        List<MultipartFile> secondaryImages
) {
    /**
     * Constructor thuận tiện khi tạo sản phẩm bằng form fields và file ảnh.
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

