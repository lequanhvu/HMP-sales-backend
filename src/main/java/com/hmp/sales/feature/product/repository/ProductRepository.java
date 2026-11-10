package com.hmp.sales.feature.product.repository;

import com.hmp.sales.feature.product.entity.Product;
import com.hmp.sales.feature.product.entity.ProductStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

/**
 * Repository truy vấn dữ liệu cho thực thể {@link Product}.
 * Hỗ trợ tìm kiếm theo tên, danh mục, trạng thái và kiểm tra ràng buộc đơn hàng.
 */
public interface ProductRepository extends JpaRepository<Product, Long>, JpaSpecificationExecutor<Product> {

    /**
     * Tìm sản phẩm theo ID và chưa bị xóa mềm.
     *
     * @param id ID sản phẩm cần tìm
     * @return Optional chứa sản phẩm nếu tồn tại và chưa xóa
     */
    Optional<Product> findByIdAndIsDeletedFalse(Long id);

    /**
     * Lấy danh sách sản phẩm ACTIVE thuộc một danh mục cụ thể.
     *
     * @param categoryId ID danh mục cần lấy sản phẩm
     * @param status     trạng thái sản phẩm (ACTIVE)
     * @return danh sách sản phẩm ACTIVE trong danh mục
     */
    List<Product> findByCategoryIdAndStatusAndIsDeletedFalse(Long categoryId, ProductStatus status);

    /**
     * Lấy danh sách sản phẩm ACTIVE thuộc một danh mục cụ thể có phân trang.
     *
     * @param categoryId ID danh mục cần lấy sản phẩm
     * @param status     trạng thái sản phẩm (ACTIVE)
     * @param pageable   thông tin phân trang
     * @return trang danh sách sản phẩm ACTIVE trong danh mục
     */
    Page<Product> findByCategoryIdAndStatusAndIsDeletedFalse(Long categoryId, ProductStatus status, Pageable pageable);

    /**
     * Lấy danh sách tất cả sản phẩm ACTIVE chưa bị xóa kèm ảnh và danh mục để nạp chỉ mục tìm kiếm vào RAM.
     * Dùng LEFT JOIN FETCH để tránh LazyInitializationException khi open-in-view = false.
     *
     * @return danh sách sản phẩm active đã fetch đầy đủ quan hệ
     */
    @Query("""
            SELECT DISTINCT p FROM Product p
            LEFT JOIN FETCH p.category c
            LEFT JOIN FETCH p.images i
            WHERE p.isDeleted = false AND p.status = 'ACTIVE'
            """)
    List<Product> findAllActiveForSearchIndex();

    /**
     * Lấy thông tin một sản phẩm kèm ảnh và danh mục phục vụ cập nhật chỉ mục tìm kiếm trong RAM.
     *
     * @param id ID sản phẩm
     * @return Optional chứa sản phẩm đã fetch đầy đủ quan hệ
     */
    @Query("""
            SELECT DISTINCT p FROM Product p
            LEFT JOIN FETCH p.category c
            LEFT JOIN FETCH p.images i
            WHERE p.id = :id AND p.isDeleted = false
            """)
    Optional<Product> findByIdForSearchIndex(@Param("id") Long id);

    /**
     * Lấy danh sách tất cả sản phẩm (bao gồm cả ACTIVE và INACTIVE) thuộc một danh mục cụ thể chưa bị xóa mềm.
     * Dành riêng cho màn hình điều hành của Quản lý (Manager) và Quản trị viên (Admin).
     *
     * @param categoryId ID danh mục cần lấy sản phẩm
     * @return danh sách tất cả sản phẩm thuộc danh mục
     */
    List<Product> findByCategoryIdAndIsDeletedFalse(Long categoryId);

    /**
     * Lấy danh sách tất cả sản phẩm (bao gồm cả ACTIVE và INACTIVE) thuộc một danh mục cụ thể chưa bị xóa mềm có phân trang.
     * Dành riêng cho màn hình điều hành của Quản lý (Manager) và Quản trị viên (Admin).
     *
     * @param categoryId ID danh mục cần lấy sản phẩm
     * @param pageable   thông tin phân trang
     * @return trang danh sách tất cả sản phẩm thuộc danh mục
     */
    Page<Product> findByCategoryIdAndIsDeletedFalse(Long categoryId, Pageable pageable);

    /**
     * Lấy danh sách ID sản phẩm nằm trong cây danh mục (bao gồm cả danh mục con) chưa bị xóa.
     *
     * @param categoryIds danh sách ID danh mục cần kiểm tra
     * @return danh sách ID sản phẩm thuộc cây danh mục
     */
    @Query("SELECT p.id FROM Product p WHERE p.category.id IN :categoryIds AND p.isDeleted = false")
    List<Long> findIdsByCategoryIdIn(@Param("categoryIds") List<Long> categoryIds);

    /**
     * Kiểm tra sản phẩm có đang nằm trong đơn hàng chưa hoàn thành hay không.
     * Chỉ chặn xóa khi có đơn ở trạng thái PENDING, CONFIRMED, PROCESSING, SHIPPING.
     *
     * @param productId ID sản phẩm cần kiểm tra
     * @return true nếu sản phẩm đang có trong đơn hàng chưa hoàn thành
     */
    @Query(value = """
            SELECT COUNT(oi.order_item_id) > 0
            FROM order_items oi
            JOIN orders o ON oi.order_id = o.order_id
            WHERE oi.product_id = :productId
              AND o.status IN ('PENDING', 'CONFIRMED', 'PROCESSING', 'SHIPPING')
              AND o.is_deleted = false
            """, nativeQuery = true)
    boolean hasIncompleteOrders(@Param("productId") Long productId);

    /**
     * Kiểm tra bất kỳ sản phẩm nào trong danh sách có đang nằm trong đơn chưa hoàn thành hay không.
     * Dùng để cascade soft-delete danh mục an toàn.
     *
     * @param productIds danh sách ID sản phẩm cần kiểm tra
     * @return true nếu có ít nhất một sản phẩm trong đơn hàng chưa hoàn thành
     */
    @Query(value = """
            SELECT COUNT(oi.order_item_id) > 0
            FROM order_items oi
            JOIN orders o ON oi.order_id = o.order_id
            WHERE oi.product_id IN (:productIds)
              AND o.status IN ('PENDING', 'CONFIRMED', 'PROCESSING', 'SHIPPING')
              AND o.is_deleted = false
            """, nativeQuery = true)
    boolean anyHasIncompleteOrders(@Param("productIds") List<Long> productIds);

    /**
     * Soft-delete hàng loạt sản phẩm theo danh sách ID.
     *
     * @param productIds danh sách ID sản phẩm cần xóa mềm
     */
    @Query("UPDATE Product p SET p.isDeleted = true WHERE p.id IN :productIds")
    @Modifying 
    @Transactional 
    void softDeleteByIds(@Param("productIds") List<Long> productIds);

    /**
     * Cập nhật trạng thái hàng loạt sản phẩm theo danh sách ID (dùng khi cascade khóa sản phẩm thuộc cây danh mục).
     *
     * @param productIds danh sách ID sản phẩm cần cập nhật
     * @param status     trạng thái mới (ACTIVE / INACTIVE)
     */
    @Query("UPDATE Product p SET p.status = :status WHERE p.id IN :productIds")
    @Modifying
    @Transactional
    void updateStatusByIds(@Param("productIds") List<Long> productIds, @Param("status") ProductStatus status);

    /**
     * Đếm số sản phẩm chưa xóa thuộc một danh mục.
     *
     * @param categoryId ID danh mục
     * @return số lượng sản phẩm chưa xóa
     */
    long countByCategoryIdAndIsDeletedFalse(Long categoryId);

    /**
     * Đếm số sản phẩm chưa xóa thuộc bất kỳ danh mục nào trong danh sách (dùng cho cây con).
     *
     * @param categoryIds danh sách ID danh mục cần đếm
     * @return số lượng sản phẩm chưa xóa trong tất cả danh mục
     */
    long countByCategoryIdInAndIsDeletedFalse(List<Long> categoryIds);
}
