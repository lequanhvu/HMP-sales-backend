package com.hmp.sales.feature.product.repository;

import com.hmp.sales.feature.product.entity.ProductPrice;
import com.hmp.sales.feature.product.entity.ProductPriceId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Repository truy vấn dữ liệu cho thực thể {@link ProductPrice}.
 * Khóa chính phức hợp {@code (variant_id, unit_id)}.
 * Giá lưu theo số nguyên VND để tránh sai số dấu phẩy động.
 */
public interface ProductPriceRepository extends JpaRepository<ProductPrice, ProductPriceId> {

    /**
     * Lấy danh sách bảng giá chưa xóa của một biến thể sản phẩm.
     *
     * @param variantId ID biến thể sản phẩm
     * @return danh sách bảng giá theo từng đơn vị tính
     */
    List<ProductPrice> findByVariantIdAndIsDeletedFalse(Long variantId);

    /**
     * Tìm bảng giá của biến thể sản phẩm theo đơn vị tính và chưa bị xóa mềm.
     *
     * @param variantId ID biến thể sản phẩm
     * @param unitId    ID đơn vị tính
     * @return Optional chứa bảng giá nếu tồn tại
     */
    @Query("SELECT pp FROM ProductPrice pp WHERE pp.variant.id = :variantId AND pp.unit.id = :unitId AND pp.isDeleted = false")
    java.util.Optional<ProductPrice> findByVariantIdAndUnitId(@Param("variantId") Long variantId, @Param("unitId") Long unitId);

    /**
     * Kiểm tra xem đơn vị tính có trong bảng giá của biến thể sản phẩm và chưa bị xóa mềm hay không.
     *
     * @param variantId ID biến thể sản phẩm
     * @param unitId    ID đơn vị tính
     * @return true nếu đơn vị tính tồn tại trong bảng giá của biến thể và chưa xóa mềm
     */
    @Query("SELECT COUNT(pp) > 0 FROM ProductPrice pp WHERE pp.variant.id = :variantId AND pp.unit.id = :unitId AND pp.isDeleted = false")
    boolean existsByVariantIdAndUnitId(@Param("variantId") Long variantId, @Param("unitId") Long unitId);

    /**
     * Soft-delete tất cả bảng giá của một biến thể (dùng khi xóa mềm biến thể).
     *
     * @param variantId ID biến thể cần xóa mềm toàn bộ bảng giá
     */
    @Modifying
    @Transactional
    @Query("UPDATE ProductPrice pp SET pp.isDeleted = true WHERE pp.variant.id = :variantId")
    void softDeleteByVariantId(@Param("variantId") Long variantId);
}
