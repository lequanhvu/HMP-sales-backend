package com.hmp.sales.feature.cart.service;

import com.hmp.sales.feature.cart.dto.CartCountResponse;
import com.hmp.sales.feature.cart.dto.CartItemAddRequest;
import com.hmp.sales.feature.cart.dto.CartItemResponse;
import com.hmp.sales.feature.cart.dto.CartItemUpdateRequest;
import com.hmp.sales.feature.cart.dto.CartResponse;

/**
 * Interface dịch vụ quản lý giỏ hàng của người dùng.
 */
public interface CartService {

    /**
     * Lấy toàn bộ thông tin giỏ hàng của người dùng hiện tại.
     *
     * @param userId ID người dùng
     * @return CartResponse chứa danh sách món sắp xếp theo thời gian mới nhất
     */
    CartResponse getCart(Long userId);

    /**
     * Lấy số lượng món trong giỏ hàng để cập nhật Badge trên Header.
     *
     * @param userId ID người dùng
     * @return CartCountResponse chứa tổng số dòng món
     */
    CartCountResponse getCartCount(Long userId);

    /**
     * Thêm sản phẩm vào giỏ hàng. Nếu sản phẩm đã tồn tại cùng quy cách thì cộng dồn số lượng.
     *
     * @param userId  ID người dùng
     * @param request DTO chứa thông tin sản phẩm và quy cách
     * @return CartItemResponse của dòng sản phẩm sau khi lưu
     */
    CartItemResponse addItem(Long userId, CartItemAddRequest request);

    /**
     * Cập nhật số lượng hoặc kích thước của món hàng trong giỏ.
     *
     * @param userId     ID người dùng
     * @param cartItemId ID dòng giỏ hàng
     * @param request    DTO cập nhật
     * @return CartItemResponse sau khi cập nhật
     */
    CartItemResponse updateItem(Long userId, Long cartItemId, CartItemUpdateRequest request);

    /**
     * Xóa một dòng món hàng khỏi giỏ.
     *
     * @param userId     ID người dùng
     * @param cartItemId ID dòng giỏ hàng cần xóa
     */
    void removeItem(Long userId, Long cartItemId);

    /**
     * Làm trống toàn bộ giỏ hàng của người dùng.
     *
     * @param userId ID người dùng
     */
    void clearCart(Long userId);
}
