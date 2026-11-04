package com.hmp.sales.feature.order.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.hmp.sales.feature.order.entity.OrderItem;

import java.math.BigDecimal;

/**
 * Record chứa thông tin chi tiết của một dòng sản phẩm trong đơn hàng.
 *
 * @param orderItemId ID dòng sản phẩm
 * @param productId   ID sản phẩm
 * @param productName tên sản phẩm (snapshot)
 * @param variantId   ID biến thể sản phẩm (nếu có)
 * @param variantName tên biến thể sản phẩm (snapshot)
 * @param color       màu sắc đã chọn (snapshot)
 * @param unit        đơn vị tính (snapshot)
 * @param quantity    số lượng đặt mua
 * @param size           kích thước/chiều dài sản xuất
 * @param unitPrice      đơn giá snapshot được tính (null khi chưa tính giá)
 * @param discountAmount số tiền giảm giá dòng sản phẩm
 * @param totalPrice     thành tiền dòng sản phẩm (null khi chưa tính giá)
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record OrderItemResponse(
        Long orderItemId,
        Long productId,
        String productName,
        Long variantId,
        String variantName,
        String color,
        String unit,
        BigDecimal quantity,
        BigDecimal size,
        Integer unitPrice,
        Long discountAmount,
        Long totalPrice
) {

    /**
     * Chuyển đổi từ entity {@link OrderItem} sang DTO {@link OrderItemResponse}.
     *
     * @param item entity dòng sản phẩm
     * @return đối tượng DTO tương ứng
     */
    public static OrderItemResponse fromEntity(OrderItem item) {
        if (item == null) {
            return null;
        }
        return new OrderItemResponse(
                item.getId(),
                item.getProduct() != null ? item.getProduct().getId() : null,
                item.getProductNameSnapshot(),
                item.getVariant() != null ? item.getVariant().getId() : null,
                item.getVariantNameSnapshot(),
                item.getColorSnapshot(),
                item.getUnitNameSnapshot(),
                item.getQuantity(),
                item.getSize(),
                item.getUnitPrice(),
                item.getDiscountAmount(),
                item.getTotalPrice()
        );
    }
}
