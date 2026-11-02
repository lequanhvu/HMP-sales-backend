package com.hmp.sales.feature.order.entity;

import com.hmp.sales.feature.product.entity.Product;
import com.hmp.sales.feature.product.entity.ProductVariant;
import com.hmp.sales.feature.product.entity.Unit;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.Objects;

/**
 * Thực thể chi tiết dòng sản phẩm trong đơn hàng (OrderItem).
 * <p>
 * Lưu trữ tại bảng {@code order_items}. Chứa thông số kỹ thuật, số lượng, kích thước
 * và các snapshot bất biến về tên sản phẩm, đơn vị tính, màu sắc và đơn giá được tính.
 * </p>
 */
@Entity
@Table(name = "order_items")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OrderItem {

    /**
     * Khóa chính tự tăng (BIGINT IDENTITY).
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "order_item_id")
    private Long id;

    /**
     * Đơn hàng chứa dòng sản phẩm này.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id", nullable = false)
    private Order order;

    /**
     * Sản phẩm liên kết.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    /**
     * Biến thể sản phẩm liên kết (nếu có).
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "variant_id")
    private ProductVariant variant;

    /**
     * Đơn vị tính liên kết.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "unit_id")
    private Unit unit;

    /**
     * Số lượng đặt hàng (tấm, cái, m,...).
     */
    @Column(name = "quantity", nullable = false, precision = 18, scale = 4)
    private BigDecimal quantity;

    /**
     * Kích thước/chiều dài sản xuất do khách hàng nhập (đối với tôn sóng, tính bằng mét).
     */
    @Column(name = "size", precision = 18, scale = 4)
    private BigDecimal size;

    /**
     * Snapshot đơn giá tại thời điểm Quản lý tính toán (VND, số nguyên).
     */
    @Column(name = "unit_price")
    private Integer unitPrice;

    /**
     * Thành tiền của dòng sản phẩm (VND, số nguyên: UnitPrice × Size × Quantity).
     */
    @Column(name = "total_price")
    private Long totalPrice;

    /**
     * Snapshot tên sản phẩm tại thời điểm đặt hàng.
     */
    @Column(name = "product_name_snapshot", nullable = false, length = 250)
    private String productNameSnapshot;

    /**
     * Snapshot tên đơn vị tính tại thời điểm đặt hàng.
     */
    @Column(name = "unit_name_snapshot", length = 50)
    private String unitNameSnapshot;

    /**
     * Snapshot màu sắc sản phẩm khách hàng đã chọn.
     */
    @Column(name = "color_snapshot", length = 100)
    private String colorSnapshot;

    /**
     * Cờ đánh dấu xóa mềm.
     */
    @Column(name = "is_deleted", nullable = false)
    @Builder.Default
    private Boolean isDeleted = false;

    @PrePersist
    public void prePersist() {
        if (this.isDeleted == null) {
            this.isDeleted = false;
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        OrderItem item = (OrderItem) o;
        return id != null && Objects.equals(id, item.id);
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
}

