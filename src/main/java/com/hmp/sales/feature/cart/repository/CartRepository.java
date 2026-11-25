package com.hmp.sales.feature.cart.repository;

import com.hmp.sales.feature.cart.entity.Cart;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Repository truy xuất giỏ hàng {@link Cart}.
 */
@Repository
public interface CartRepository extends JpaRepository<Cart, Long> {

    /**
     * Tìm giỏ hàng theo User ID.
     *
     * @param userId ID người dùng
     * @return Optional chứa Cart nếu có
     */
    Optional<Cart> findByUserId(Long userId);

    /**
     * Kiểm tra người dùng đã có giỏ hàng hay chưa.
     *
     * @param userId ID người dùng
     * @return true nếu đã có giỏ hàng
     */
    boolean existsByUserId(Long userId);
}
