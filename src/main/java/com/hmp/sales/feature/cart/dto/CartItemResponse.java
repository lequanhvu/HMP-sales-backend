package com.hmp.sales.feature.cart.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.hmp.sales.feature.cart.entity.CartItem;
import com.hmp.sales.feature.product.dto.ProductTypeResponse;
import com.hmp.sales.feature.product.entity.Product;
import com.hmp.sales.feature.product.entity.ProductImage;
import com.hmp.sales.feature.product.entity.ProductPrice;
import com.hmp.sales.feature.product.entity.ProductStatus;
import com.hmp.sales.feature.product.entity.ProductVariant;
import com.hmp.sales.feature.product.entity.VariantStatus;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;

/**
 * Response DTO chi tiết một món hàng trong giỏ hàng.
 *
 * @param cartItemId      ID dòng giỏ hàng
 * @param productId       ID sản phẩm
 * @param productName     tên sản phẩm
 * @param primaryImageUrl URL ảnh đại diện sản phẩm
 * @param productType     loại sản phẩm kỹ thuật
 * @param variantId       ID biến thể (nếu có)
 * @param variantName     tên biến thể (nếu có)
 * @param colorId         ID màu sắc (nếu có)
 * @param colorName       tên màu sắc (nếu có)
 * @param unitId          ID đơn vị tính (nếu có)
 * @param unitName        tên đơn vị tính (nếu có)
 * @param size            kích thước tính tiền (chiều dài mét, diện tích...)
 * @param dimensions      kích thước quy cách chi tiết
 * @param quantity        số lượng đặt mua
 * @param unitPrice       đơn giá niêm yết (null nếu thiếu quyền xem giá hoặc chưa cấu hình)
 * @param totalPrice      tổng tiền tạm tính = unitPrice * (size != null ? size : 1) * quantity
 * @param priceVisible    true nếu caller có quyền xem giá
 * @param productStatus   trạng thái sản phẩm (ACTIVE / INACTIVE)
 * @param isAvailable     true nếu sản phẩm và biến thể đều ACTIVE và chưa bị xóa
 * @param createdAt       thời điểm thêm vào giỏ
 * @param updatedAt       thời điểm cập nhật gần nhất
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record CartItemResponse(
        Long cartItemId,
        Long productId,
        String productName,
        String primaryImageUrl,
        ProductTypeResponse productType,
        Long variantId,
        String variantName,
        Long colorId,
        String colorName,
        Long unitId,
        String unitName,
        BigDecimal size,
        String dimensions,
        BigDecimal quantity,
        Integer unitPrice,
        Long totalPrice,
        boolean priceVisible,
        ProductStatus productStatus,
        boolean isAvailable,
        Instant createdAt,
        Instant updatedAt
) {

    public static CartItemResponse fromEntity(CartItem item, boolean priceVisible) {
        if (item == null) {
            return null;
        }

        Product product = item.getProduct();
        ProductVariant variant = item.getVariant();

        String primaryImageUrl = null;
        if (product != null && product.getImages() != null) {
            primaryImageUrl = product.getImages().stream()
                    .filter(img -> !img.getIsDeleted() && Boolean.TRUE.equals(img.getIsPrimary()))
                    .findFirst()
                    .map(ProductImage::getImageUrl)
                    .orElse(null);
        }

        boolean isAvailable = product != null
                && product.getStatus() == ProductStatus.ACTIVE
                && !product.getIsDeleted()
                && (variant == null || (variant.getStatus() == VariantStatus.ACTIVE && !variant.getIsDeleted()));

        Integer unitPrice = null;
        Long totalPrice = null;

        if (priceVisible && variant != null && variant.getPrices() != null) {
            ProductPrice priceObj = null;
            if (item.getUnit() != null) {
                priceObj = variant.getPrices().stream()
                        .filter(p -> !p.getIsDeleted() && p.getUnit().getId().equals(item.getUnit().getId()))
                        .findFirst()
                        .orElse(null);
            }
            if (priceObj == null) {
                if (item.getSize() != null && item.getSize().compareTo(BigDecimal.ZERO) > 0) {
                    priceObj = variant.getPrices().stream()
                            .filter(p -> !Boolean.TRUE.equals(p.getIsDeleted()) && p.getUnit() != null && p.getUnit().getUnitName() != null)
                            .filter(p -> p.getUnit().getUnitName().trim().equalsIgnoreCase("m") || p.getUnit().getUnitName().trim().equalsIgnoreCase("mét"))
                            .findFirst()
                            .orElse(null);
                }
                if (priceObj == null) {
                    priceObj = variant.getPrices().stream()
                            .filter(p -> !Boolean.TRUE.equals(p.getIsDeleted()) && p.getPrice() != null)
                            .findFirst()
                            .orElse(null);
                }
            }
            if (priceObj != null && priceObj.getPrice() != null) {
                unitPrice = priceObj.getPrice();
                BigDecimal multiplier = item.getSize() != null ? item.getSize() : BigDecimal.ONE;
                BigDecimal totalAmount = BigDecimal.valueOf(unitPrice)
                        .multiply(multiplier)
                        .multiply(item.getQuantity());
                totalPrice = totalAmount.setScale(0, RoundingMode.HALF_UP).longValue();
            }
        }

        return new CartItemResponse(
                item.getId(),
                product != null ? product.getId() : null,
                product != null ? product.getProductName() : null,
                primaryImageUrl,
                product != null ? ProductTypeResponse.fromEntity(product.getProductType()) : null,
                variant != null ? variant.getId() : null,
                variant != null ? variant.getVariantName() : null,
                item.getColor() != null ? item.getColor().getId() : null,
                item.getColor() != null ? item.getColor().getColorName() : null,
                item.getUnit() != null ? item.getUnit().getId() : null,
                item.getUnit() != null ? item.getUnit().getUnitName() : null,
                item.getSize(),
                item.getDimensions(),
                item.getQuantity(),
                unitPrice,
                totalPrice,
                priceVisible,
                product != null ? product.getStatus() : null,
                isAvailable,
                item.getCreatedAt(),
                item.getUpdatedAt()
        );
    }

    /**
     * Factory phương thức tiện ích cho kiểm thử.
     */
    public static CartItemResponse of(Long cartItemId, Long productId, String productName, BigDecimal size, String dimensions, BigDecimal quantity, Integer unitPrice, Long totalPrice) {
        return new CartItemResponse(
                cartItemId, productId, productName, null, null, null, null, null, null, null, null,
                size, dimensions, quantity, unitPrice, totalPrice, true, ProductStatus.ACTIVE, true, Instant.now(), Instant.now()
        );
    }

    @com.fasterxml.jackson.annotation.JsonProperty("id")
    public Long id() {
        return cartItemId;
    }

    @com.fasterxml.jackson.annotation.JsonProperty("productImageUrl")
    public String productImageUrl() {
        return primaryImageUrl;
    }
}
