package com.hmp.sales.feature.product.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
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
 * Thực thể đại diện cho loại sản phẩm kỹ thuật (ProductType).
 * <p>
 * Lưu trữ tại bảng {@code product_types}. Dùng để xác định tập thông số kỹ thuật
 * tương ứng trên giao diện đặt hàng (ví dụ: Tôn lợp mái, Máng xối, Diềm sườn, Hàng bán lẻ...).
 * </p>
 */
@Entity
@Table(name = "product_types")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProductType {

    /**
     * Khóa chính định danh loại sản phẩm (BIGINT IDENTITY).
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "product_type_id")
    private Long id;

    /**
     * Mã định danh loại sản phẩm (ví dụ: ROOFING_SHEET, GUTTER, GABLE_FLASHING, RIDGE_CAP, RETAIL, UNKNOWN).
     */
    @Column(name = "code", nullable = false, unique = true, length = 50)
    private String code;

    /**
     * Tên hiển thị tiếng Việt của loại sản phẩm.
     */
    @Column(name = "name", nullable = false, length = 150)
    private String name;

    /**
     * Hướng dẫn công thức hoặc cấu trúc kích thước mặt cắt.
     */
    @Column(name = "note", columnDefinition = "TEXT")
    private String note;

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

    @PrePersist
    protected void onCreate() {
        this.createdAt = Instant.now();
        this.updatedAt = Instant.now();
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = Instant.now();
    }
}
