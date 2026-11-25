package com.hmp.sales.feature.cart.controller;

import com.hmp.sales.common.response.ApiSuccessResponse;
import com.hmp.sales.common.util.SecurityUtils;
import com.hmp.sales.feature.cart.dto.CartCountResponse;
import com.hmp.sales.feature.cart.dto.CartItemAddRequest;
import com.hmp.sales.feature.cart.dto.CartItemResponse;
import com.hmp.sales.feature.cart.dto.CartItemUpdateRequest;
import com.hmp.sales.feature.cart.dto.CartResponse;
import com.hmp.sales.feature.cart.service.CartService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Controller cung cấp các REST API quản lý giỏ hàng của khách hàng.
 * <p>
 * Đường dẫn cơ sở: {@code /api/v1/cart}.
 * Tất cả các thao tác đều yêu cầu xác thực người dùng và được ràng buộc trực tiếp theo User ID.
 * </p>
 */
@RestController
@RequestMapping("/api/v1/cart")
@RequiredArgsConstructor
public class CustomerCartController {

    private final CartService cartService;

    /**
     * Lấy toàn bộ thông tin giỏ hàng của người dùng hiện tại (sắp xếp theo thời gian cập nhật giảm dần).
     *
     * @param authentication thông tin xác thực của người dùng
     * @return giỏ hàng và danh sách món
     */
    @GetMapping
    public ResponseEntity<ApiSuccessResponse<CartResponse>> getCart(Authentication authentication) {
        Long userId = SecurityUtils.extractUserId(authentication);
        CartResponse response = cartService.getCart(userId);
        return ResponseEntity.ok(ApiSuccessResponse.success("Lấy thông tin giỏ hàng thành công", response));
    }

    /**
     * Lấy tổng số lượng món trong giỏ hàng để cập nhật Badge hiển thị trên Header.
     *
     * @param authentication thông tin xác thực của người dùng
     * @return số lượng món
     */
    @GetMapping("/count")
    public ResponseEntity<ApiSuccessResponse<CartCountResponse>> getCartCount(Authentication authentication) {
        Long userId = SecurityUtils.extractUserId(authentication);
        CartCountResponse response = cartService.getCartCount(userId);
        return ResponseEntity.ok(ApiSuccessResponse.success("Lấy số lượng món trong giỏ hàng thành công", response));
    }

    /**
     * Thêm sản phẩm vào giỏ hàng. Tự động gộp dòng nếu trùng sản phẩm và quy cách.
     *
     * @param request        thông tin sản phẩm cần thêm
     * @param authentication thông tin xác thực của người dùng
     * @return dòng sản phẩm đã thêm/cập nhật
     */
    @PostMapping("/items")
    public ResponseEntity<ApiSuccessResponse<CartItemResponse>> addItem(
            @Valid @RequestBody CartItemAddRequest request,
            Authentication authentication) {
        Long userId = SecurityUtils.extractUserId(authentication);
        CartItemResponse response = cartService.addItem(userId, request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiSuccessResponse.created("Thêm sản phẩm vào giỏ hàng thành công", response));
    }

    /**
     * Cập nhật số lượng hoặc kích thước của món hàng trong giỏ.
     *
     * @param cartItemId     ID dòng giỏ hàng
     * @param request        thông tin cập nhật
     * @param authentication thông tin xác thực của người dùng
     * @return dòng sản phẩm sau khi cập nhật
     */
    @PatchMapping("/items/{cartItemId}")
    public ResponseEntity<ApiSuccessResponse<CartItemResponse>> updateItem(
            @PathVariable Long cartItemId,
            @Valid @RequestBody CartItemUpdateRequest request,
            Authentication authentication) {
        Long userId = SecurityUtils.extractUserId(authentication);
        CartItemResponse response = cartService.updateItem(userId, cartItemId, request);
        return ResponseEntity.ok(ApiSuccessResponse.success("Cập nhật sản phẩm trong giỏ hàng thành công", response));
    }

    /**
     * Xóa một dòng sản phẩm khỏi giỏ hàng.
     *
     * @param cartItemId     ID dòng giỏ hàng
     * @param authentication thông tin xác thực của người dùng
     * @return phản hồi thành công
     */
    @DeleteMapping("/items/{cartItemId}")
    public ResponseEntity<ApiSuccessResponse<Void>> removeItem(
            @PathVariable Long cartItemId,
            Authentication authentication) {
        Long userId = SecurityUtils.extractUserId(authentication);
        cartService.removeItem(userId, cartItemId);
        return ResponseEntity.ok(ApiSuccessResponse.success("Xóa sản phẩm khỏi giỏ hàng thành công", null));
    }

    /**
     * Làm trống toàn bộ giỏ hàng của người dùng.
     *
     * @param authentication thông tin xác thực của người dùng
     * @return phản hồi thành công
     */
    @DeleteMapping("/clear")
    public ResponseEntity<ApiSuccessResponse<Void>> clearCart(Authentication authentication) {
        Long userId = SecurityUtils.extractUserId(authentication);
        cartService.clearCart(userId);
        return ResponseEntity.ok(ApiSuccessResponse.success("Làm trống giỏ hàng thành công", null));
    }
}
