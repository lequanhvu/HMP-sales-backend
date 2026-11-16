package com.hmp.sales.feature.product.entity;

import com.hmp.sales.feature.user.entity.User;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

/**
 * Thực thể đại diện cho sản phẩm nổi bật (Featured Product).
 * <p>
 * Lưu trữ tại bảng {@code featured_products}. Cho phép Quản lý (Manager/Admin) lựa chọn
 * những sản phẩm tiêu biểu để trưng bày trên trang chủ hoặc các mục đề xuất nổi bật.
 * <p>
 * Quy tắc nghiệp vụ:
 * <ul>
 *   <li>Mỗi sản phẩm chỉ xuất hiện tối đa một lần trong danh sách nổi bật chưa bị xóa mềm (isDeleted = false).</li>
 *   <li>Chỉ có thể thêm sản phẩm có trạng thái {@code ACTIVE} và chưa bị xóa mềm vào danh sách nổi bật.</li>
 *   <li>Thứ tự hiển thị được sắp xếp tăng dần theo {@code displayOrder}; các bản ghi cùng thứ tự sẽ xếp theo {@code createdAt} giảm dần.</li>
 * </ul>
 */
@Entity
@Table(name = "featured_products")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FeaturedProduct {

    /**
     * Khóa chính định danh bản ghi sản phẩm nổi bật (BIGINT IDENTITY).
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "featured_id")
    private Long id;

    /**
     * Sản phẩm được đánh dấu nổi bật (bắt buộc).
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    /**
     * Thứ tự ưu tiên hiển thị (số nhỏ xếp trước, mặc định 0).
     */
    @Column(name = "display_order", nullable = false)
    @Builder.Default
    private Integer displayOrder = 0;

    /**
     * Trạng thái hiển thị (ACTIVE, INACTIVE). Mặc định là ACTIVE.
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    @Builder.Default
    private FeaturedProductStatus status = FeaturedProductStatus.ACTIVE;

    /**
     * Người dùng (Quản lý) thiết lập sản phẩm nổi bật.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by")
    private User createdBy;

    /**
     * Thời điểm tạo bản ghi.
     */
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    /**
     * Thời điểm cập nhật bản ghi gần nhất.
     */
    @Column(name = "updated_at")
    private Instant updatedAt;

    /**
     * Cờ đánh dấu xóa mềm (false: còn hiệu lực, true: đã xóa).
     */
    @Column(name = "is_deleted", nullable = false)
    @Builder.Default
    private Boolean isDeleted = false;

    /**
     * Tự động khởi tạo giá trị mặc định và thời gian trước khi insert.
     */
    @PrePersist
    public void prePersist() {
        if (createdAt == null) {
            createdAt = Instant.now();
        }
        if (isDeleted == null) {
            isDeleted = false;
        }
        if (status == null) {
            status = FeaturedProductStatus.ACTIVE;
        }
        if (displayOrder == null) {
            displayOrder = 0;
        }
    }

    /**
     * Tự động cập nhật thời gian trước khi update.
     */
    @PreUpdate
    public void preUpdate() {
        updatedAt = Instant.now();
    }
}
