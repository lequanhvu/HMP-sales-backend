package com.hmp.sales.feature.cart.service.impl;

import com.hmp.sales.common.exception.BusinessRuleException;
import com.hmp.sales.common.exception.ResourceNotFoundException;
import com.hmp.sales.feature.cart.dto.CartCountResponse;
import com.hmp.sales.feature.cart.dto.CartItemAddRequest;
import com.hmp.sales.feature.cart.dto.CartItemResponse;
import com.hmp.sales.feature.cart.dto.CartItemUpdateRequest;
import com.hmp.sales.feature.cart.dto.CartResponse;
import com.hmp.sales.feature.cart.entity.Cart;
import com.hmp.sales.feature.cart.entity.CartItem;
import com.hmp.sales.feature.cart.repository.CartItemRepository;
import com.hmp.sales.feature.cart.repository.CartRepository;
import com.hmp.sales.feature.cart.service.CartService;
import com.hmp.sales.feature.product.entity.Color;
import com.hmp.sales.feature.product.entity.Product;
import com.hmp.sales.feature.product.entity.ProductPrice;
import com.hmp.sales.feature.product.entity.ProductStatus;
import com.hmp.sales.feature.product.entity.ProductVariant;
import com.hmp.sales.feature.product.entity.Unit;
import com.hmp.sales.feature.product.repository.ColorRepository;
import com.hmp.sales.feature.product.repository.ProductRepository;
import com.hmp.sales.feature.product.repository.ProductVariantRepository;
import com.hmp.sales.feature.product.repository.UnitRepository;
import com.hmp.sales.feature.user.entity.User;
import com.hmp.sales.feature.user.repository.UserRepository;
import com.hmp.sales.security.PermissionEvaluationService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * Triển khai dịch vụ quản lý giỏ hàng của người dùng.
 */
@Service
@RequiredArgsConstructor
public class CartServiceImpl implements CartService {

    private final CartRepository cartRepository;
    private final CartItemRepository cartItemRepository;
    private final UserRepository userRepository;
    private final ProductRepository productRepository;
    private final ProductVariantRepository productVariantRepository;
    private final ColorRepository colorRepository;
    private final UnitRepository unitRepository;
    private final PermissionEvaluationService permissionEvaluationService;

    @Override
    @Transactional
    public CartResponse getCart(Long userId) {
        Cart cart = getOrCreateCart(userId);
        List<CartItem> items = cartItemRepository.findByCartIdOrderByUpdatedAtDesc(cart.getId());
        boolean priceVisible = permissionEvaluationService.canViewProductPrice();

        List<CartItemResponse> itemResponses = items.stream()
                .map(item -> CartItemResponse.fromEntity(item, priceVisible))
                .collect(Collectors.toList());

        return new CartResponse(cart.getId(), itemResponses.size(), itemResponses);
    }

    @Override
    @Transactional(readOnly = true)
    public CartCountResponse getCartCount(Long userId) {
        Cart cart = cartRepository.findByUserId(userId).orElse(null);
        if (cart == null) {
            return new CartCountResponse(0);
        }
        long count = cartItemRepository.countByCartId(cart.getId());
        return new CartCountResponse(count);
    }

    @Override
    @Transactional
    public CartItemResponse addItem(Long userId, CartItemAddRequest request) {
        Product product = productRepository.findByIdAndIsDeletedFalse(request.productId())
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy sản phẩm với ID: " + request.productId()));

        if (product.getStatus() != ProductStatus.ACTIVE) {
            throw new BusinessRuleException("Sản phẩm '" + product.getProductName() + "' hiện đang tạm ngưng kinh doanh.");
        }

        ProductVariant variant = null;
        if (request.variantId() != null) {
            variant = productVariantRepository.findByIdAndIsDeletedFalse(request.variantId())
                    .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy biến thể với ID: " + request.variantId()));
            if (!variant.getProduct().getId().equals(product.getId())) {
                throw new BusinessRuleException("Biến thể không thuộc sản phẩm này.");
            }
        }

        Color color = null;
        if (request.colorId() != null) {
            color = colorRepository.findByIdAndIsDeletedFalse(request.colorId())
                    .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy màu sắc với ID: " + request.colorId()));
        }

        Unit unit = null;
        if (request.unitId() != null) {
            unit = unitRepository.findByIdAndIsDeletedFalse(request.unitId())
                    .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy đơn vị tính với ID: " + request.unitId()));
        } else if (variant != null && variant.getPrices() != null && !variant.getPrices().isEmpty()) {
            ProductPrice autoPp = null;
            if (request.size() != null && request.size().compareTo(BigDecimal.ZERO) > 0) {
                autoPp = variant.getPrices().stream()
                        .filter(p -> !Boolean.TRUE.equals(p.getIsDeleted()) && p.getUnit() != null)
                        .filter(p -> p.getUnit().getUnitName() != null && (p.getUnit().getUnitName().trim().equalsIgnoreCase("m") || p.getUnit().getUnitName().trim().equalsIgnoreCase("mét")))
                        .findFirst()
                        .orElse(null);
            }
            if (autoPp == null) {
                autoPp = variant.getPrices().stream()
                        .filter(p -> !Boolean.TRUE.equals(p.getIsDeleted()) && p.getUnit() != null && p.getPrice() != null)
                        .findFirst()
                        .orElse(null);
            }
            if (autoPp != null) {
                unit = autoPp.getUnit();
            }
        }

        Cart cart = getOrCreateCart(userId);
        List<CartItem> currentItems = cartItemRepository.findByCartIdOrderByUpdatedAtDesc(cart.getId());

        // Kiểm tra xem đã có món tương đồng chưa để cộng dồn
        CartItem matchedItem = null;
        for (CartItem item : currentItems) {
            if (isSameProductSpec(item, product.getId(), request.variantId(), request.colorId(), request.unitId(), request.size(), request.dimensions())) {
                matchedItem = item;
                break;
            }
        }

        CartItem savedItem;
        if (matchedItem != null) {
            matchedItem.setQuantity(matchedItem.getQuantity().add(request.quantity()));
            matchedItem.setUpdatedAt(Instant.now());
            savedItem = cartItemRepository.save(matchedItem);
        } else {
            CartItem newItem = CartItem.builder()
                    .cart(cart)
                    .product(product)
                    .variant(variant)
                    .color(color)
                    .unit(unit)
                    .size(request.size())
                    .dimensions(trimOrNull(request.dimensions()))
                    .quantity(request.quantity())
                    .build();
            savedItem = cartItemRepository.save(newItem);
        }

        cart.setUpdatedAt(Instant.now());
        cartRepository.save(cart);

        boolean priceVisible = permissionEvaluationService.canViewProductPrice();
        return CartItemResponse.fromEntity(savedItem, priceVisible);
    }

    @Override
    @Transactional
    public CartItemResponse updateItem(Long userId, Long cartItemId, CartItemUpdateRequest request) {
        Cart cart = getOrCreateCart(userId);
        CartItem item = cartItemRepository.findByIdAndCartId(cartItemId, cart.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy sản phẩm trong giỏ hàng với ID: " + cartItemId));

        item.setQuantity(request.quantity());
        if (request.size() != null) {
            item.setSize(request.size());
        }
        if (request.dimensions() != null) {
            item.setDimensions(trimOrNull(request.dimensions()));
        }
        item.setUpdatedAt(Instant.now());
        CartItem saved = cartItemRepository.save(item);

        cart.setUpdatedAt(Instant.now());
        cartRepository.save(cart);

        boolean priceVisible = permissionEvaluationService.canViewProductPrice();
        return CartItemResponse.fromEntity(saved, priceVisible);
    }

    @Override
    @Transactional
    public void removeItem(Long userId, Long cartItemId) {
        Cart cart = getOrCreateCart(userId);
        CartItem item = cartItemRepository.findByIdAndCartId(cartItemId, cart.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy sản phẩm trong giỏ hàng với ID: " + cartItemId));
        cartItemRepository.delete(item);

        cart.setUpdatedAt(Instant.now());
        cartRepository.save(cart);
    }

    @Override
    @Transactional
    public void clearCart(Long userId) {
        Cart cart = getOrCreateCart(userId);
        cartItemRepository.deleteAllByCartId(cart.getId());
        cart.setUpdatedAt(Instant.now());
        cartRepository.save(cart);
    }

    private Cart getOrCreateCart(Long userId) {
        return cartRepository.findByUserId(userId)
                .orElseGet(() -> {
                    User user = userRepository.findById(userId)
                            .filter(u -> !Boolean.TRUE.equals(u.getIsDeleted()))
                            .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy người dùng với ID: " + userId));
                    Cart newCart = Cart.builder()
                            .user(user)
                            .build();
                    return cartRepository.save(newCart);
                });
    }

    private boolean isSameProductSpec(CartItem item, Long productId, Long variantId, Long colorId, Long unitId, BigDecimal size, String dimensions) {
        if (!item.getProduct().getId().equals(productId)) {
            return false;
        }

        Long itemVariantId = item.getVariant() != null ? item.getVariant().getId() : null;
        if (!Objects.equals(itemVariantId, variantId)) {
            return false;
        }

        Long itemColorId = item.getColor() != null ? item.getColor().getId() : null;
        if (!Objects.equals(itemColorId, colorId)) {
            return false;
        }

        Long itemUnitId = item.getUnit() != null ? item.getUnit().getId() : null;
        if (!Objects.equals(itemUnitId, unitId)) {
            return false;
        }

        if (size == null) {
            if (item.getSize() != null) {
                return false;
            }
        } else {
            if (item.getSize() == null || item.getSize().compareTo(size) != 0) {
                return false;
            }
        }

        String itemDim = trimOrNull(item.getDimensions());
        String reqDim = trimOrNull(dimensions);
        return Objects.equals(itemDim, reqDim);
    }

    private String trimOrNull(String str) {
        if (str == null || str.isBlank()) {
            return null;
        }
        return str.trim();
    }
}
