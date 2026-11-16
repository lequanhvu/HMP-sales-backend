package com.hmp.sales.feature.product.repository;

import com.hmp.sales.feature.product.entity.FeaturedProduct;
import com.hmp.sales.feature.product.entity.FeaturedProductStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository thao tác truy vấn bảng {@code featured_products}.
 */
@Repository
public interface FeaturedProductRepository extends JpaRepository<FeaturedProduct, Long> {

    /**
     * Tìm kiếm bản ghi sản phẩm nổi bật theo ID khi chưa bị xóa mềm.
     *
     * @param id ID sản phẩm nổi bật
     * @return Optional chứa {@link FeaturedProduct} nếu tồn tại
     */
    Optional<FeaturedProduct> findByIdAndIsDeletedFalse(Long id);

    /**
     * Tìm kiếm bản ghi sản phẩm nổi bật theo ID sản phẩm khi chưa bị xóa mềm.
     *
     * @param productId ID sản phẩm
     * @return Optional chứa {@link FeaturedProduct} nếu tồn tại
     */
    Optional<FeaturedProduct> findByProductIdAndIsDeletedFalse(Long productId);

    /**
     * Kiểm tra xem sản phẩm đã tồn tại trong danh sách nổi bật chưa bị xóa mềm hay chưa.
     *
     * @param productId ID sản phẩm
     * @return {@code true} nếu đã tồn tại, ngược lại {@code false}
     */
    boolean existsByProductIdAndIsDeletedFalse(Long productId);

    /**
     * Lấy toàn bộ danh sách sản phẩm nổi bật phục vụ Quản lý (bao gồm cả ACTIVE và INACTIVE).
     * Sắp xếp theo {@code displayOrder ASC}, sau đó theo {@code createdAt DESC}.
     *
     * @return danh sách sản phẩm nổi bật chưa xóa mềm
     */
    List<FeaturedProduct> findByIsDeletedFalseOrderByDisplayOrderAscCreatedAtDesc();

    /**
     * Lấy danh sách sản phẩm nổi bật theo trạng thái (thường là {@code ACTIVE}) cho trang chủ công khai.
     * Sắp xếp theo {@code displayOrder ASC}, sau đó theo {@code createdAt DESC}.
     *
     * @param status trạng thái hiển thị
     * @return danh sách sản phẩm nổi bật thỏa mãn điều kiện
     */
    List<FeaturedProduct> findByStatusAndIsDeletedFalseOrderByDisplayOrderAscCreatedAtDesc(FeaturedProductStatus status);

    /**
     * Xóa mềm toàn bộ bản ghi sản phẩm nổi bật liên kết với một sản phẩm cụ thể.
     *
     * @param productId ID sản phẩm
     */
    @Modifying
    @Query("UPDATE FeaturedProduct fp SET fp.isDeleted = true WHERE fp.product.id = :productId AND fp.isDeleted = false")
    void softDeleteByProductId(@Param("productId") Long productId);
}
