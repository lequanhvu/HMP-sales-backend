package com.hmp.sales.feature.product.dto;

import com.hmp.sales.feature.product.entity.FeaturedProduct;
import com.hmp.sales.feature.product.entity.FeaturedProductStatus;
import com.hmp.sales.feature.product.entity.Product;
import com.hmp.sales.feature.product.entity.ProductImage;
import com.hmp.sales.feature.product.entity.ProductStatus;

import java.time.Instant;

/**
 * Response DTO chi tiết của sản phẩm nổi bật.
 *
 * @param id              ID bản ghi sản phẩm nổi bật (featured_id)
 * @param productId       ID sản phẩm
 * @param productName     tên sản phẩm
 * @param productStatus   trạng thái hoạt động của sản phẩm (ACTIVE, INACTIVE)
 * @param categoryId      ID danh mục chứa sản phẩm
 * @param categoryName    tên danh mục chứa sản phẩm
 * @param brandId         ID thương hiệu
 * @param brandName       tên thương hiệu
 * @param primaryImageUrl URL ảnh đại diện sản phẩm (nếu có)
 * @param displayOrder    thứ tự ưu tiên hiển thị
 * @param status          trạng thái hiển thị nổi bật (ACTIVE, INACTIVE)
 * @param createdAt       thời điểm thêm vào danh sách nổi bật
 * @param updatedAt       thời điểm cập nhật gần nhất
 * @param priceVisible    cờ cho biết người dùng có quyền xem giá hay không
 */
public record FeaturedProductResponse(
        Long id,
        Long productId,
        String productName,
        ProductStatus productStatus,
        Long categoryId,
        String categoryName,
        Long brandId,
        String brandName,
        String primaryImageUrl,
        Integer displayOrder,
        FeaturedProductStatus status,
        Instant createdAt,
        Instant updatedAt,
        boolean priceVisible
) {

    /**
     * Chuyển đổi từ entity {@link FeaturedProduct} sang DTO phản hồi.
     *
     * @param featuredProduct entity sản phẩm nổi bật
     * @param priceVisible    {@code true} nếu người dùng được phép xem giá sản phẩm
     * @return {@link FeaturedProductResponse} đã được ánh xạ đầy đủ
     */
    public static FeaturedProductResponse fromEntity(FeaturedProduct featuredProduct, boolean priceVisible) {
        Product product = featuredProduct.getProduct();
        String primaryImageUrl = null;
        if (product != null && product.getImages() != null) {
            primaryImageUrl = product.getImages().stream()
                    .filter(img -> !Boolean.TRUE.equals(img.getIsDeleted()) && Boolean.TRUE.equals(img.getIsPrimary()))
                    .findFirst()
                    .map(ProductImage::getImageUrl)
                    .orElse(null);
        }

        return new FeaturedProductResponse(
                featuredProduct.getId(),
                product != null ? product.getId() : null,
                product != null ? product.getProductName() : null,
                product != null ? product.getStatus() : null,
                (product != null && product.getCategory() != null) ? product.getCategory().getId() : null,
                (product != null && product.getCategory() != null) ? product.getCategory().getCategoryName() : null,
                (product != null && product.getBrand() != null) ? product.getBrand().getId() : null,
                (product != null && product.getBrand() != null) ? product.getBrand().getBrandName() : null,
                primaryImageUrl,
                featuredProduct.getDisplayOrder(),
                featuredProduct.getStatus(),
                featuredProduct.getCreatedAt(),
                featuredProduct.getUpdatedAt(),
                priceVisible
        );
    }
}
