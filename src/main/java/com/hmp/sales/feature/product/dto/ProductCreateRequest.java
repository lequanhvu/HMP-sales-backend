package com.hmp.sales.feature.product.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.util.List;

/**
 * Request DTO để tạo mới sản phẩm trong một danh mục.
 * Dùng cho endpoint {@code POST /api/v1/manager/categories/{categoryId}/products}.
 *
 * @param productName tên sản phẩm, bắt buộc, tối đa 250 ký tự
 * @param description mô tả chi tiết (tùy chọn)
 * @param brandId     ID thương hiệu (tùy chọn)
 * @param variants    danh sách biến thể khởi tạo ban đầu (tùy chọn, có thể rỗng)
 * @param images      danh sách ảnh ban đầu dạng URL (tùy chọn, có thể rỗng)
 */
public record ProductCreateRequest(

        @NotBlank(message = "Tên sản phẩm không được để trống.")
        @Size(max = 250, message = "Tên sản phẩm không được vượt quá 250 ký tự.")
        String productName,

        String description,

        Long brandId,

        @Valid
        List<ProductVariantRequest> variants,

        @Valid
        List<ProductImageUploadRequest> images
) {
    /**
     * Constructor tương thích ngược cho các luồng hoặc bài kiểm thử không truyền tham số images.
     *
     * @param productName tên sản phẩm
     * @param description mô tả chi tiết
     * @param brandId     ID thương hiệu
     * @param variants    danh sách biến thể khởi tạo ban đầu
     */
    public ProductCreateRequest(String productName, String description, Long brandId, List<ProductVariantRequest> variants) {
        this(productName, description, brandId, variants, null);
    }
}
