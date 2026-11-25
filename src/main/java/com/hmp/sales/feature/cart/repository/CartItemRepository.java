package com.hmp.sales.feature.cart.repository;

import com.hmp.sales.feature.cart.entity.CartItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository truy xuất các dòng sản phẩm trong giỏ hàng {@link CartItem}.
 */
@Repository
public interface CartItemRepository extends JpaRepository<CartItem, Long> {

    /**
     * Lấy toàn bộ sản phẩm trong giỏ hàng sắp xếp theo thời điểm cập nhật giảm dần.
     *
     * @param cartId ID giỏ hàng
     * @return danh sách các món trong giỏ hàng
     */
    List<CartItem> findByCartIdOrderByUpdatedAtDesc(Long cartId);

    /**
     * Đếm tổng số món trong giỏ hàng của người dùng.
     *
     * @param cartId ID giỏ hàng
     * @return tổng số dòng món
     */
    long countByCartId(Long cartId);

    /**
     * Tìm món theo ID và ID giỏ hàng.
     *
     * @param itemId ID dòng giỏ hàng
     * @param cartId ID giỏ hàng
     * @return Optional chứa CartItem nếu tìm thấy
     */
    Optional<CartItem> findByIdAndCartId(Long itemId, Long cartId);

    /**
     * Xóa toàn bộ sản phẩm trong giỏ hàng.
     *
     * @param cartId ID giỏ hàng
     */
    void deleteAllByCartId(Long cartId);
}
