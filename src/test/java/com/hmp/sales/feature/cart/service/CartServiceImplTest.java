package com.hmp.sales.feature.cart.service;

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
import com.hmp.sales.feature.cart.service.impl.CartServiceImpl;
import com.hmp.sales.feature.product.entity.Color;
import com.hmp.sales.feature.product.entity.Product;
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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("CartServiceImpl Tests")
class CartServiceImplTest {

    @Mock
    private CartRepository cartRepository;

    @Mock
    private CartItemRepository cartItemRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private ProductRepository productRepository;

    @Mock
    private ProductVariantRepository productVariantRepository;

    @Mock
    private ColorRepository colorRepository;

    @Mock
    private UnitRepository unitRepository;

    @Mock
    private PermissionEvaluationService permissionEvaluationService;

    @InjectMocks
    private CartServiceImpl cartService;

    private User user;
    private Cart cart;
    private Product product;
    private ProductVariant variant;
    private Color color;
    private Unit unit;

    @BeforeEach
    void setUp() {
        user = User.builder().id(10L).fullName("Nguyễn Văn A").build();
        cart = Cart.builder().id(1L).user(user).items(new ArrayList<>()).createdAt(Instant.now()).updatedAt(Instant.now()).build();
        product = Product.builder().id(100L).productName("Tôn sóng").status(ProductStatus.ACTIVE).isDeleted(false).build();
        variant = ProductVariant.builder().id(1001L).variantName("0.40mm").product(product).isDeleted(false).build();
        color = Color.builder().id(1L).colorName("Xanh ngọc").isDeleted(false).build();
        unit = Unit.builder().id(2L).unitName("m").isDeleted(false).build();
    }

    @Test
    @DisplayName("getCart - Thành công trả về danh sách các món trong giỏ")
    void getCart_success() {
        CartItem item = CartItem.builder()
                .id(11L)
                .cart(cart)
                .product(product)
                .variant(variant)
                .color(color)
                .unit(unit)
                .size(new BigDecimal("5.5"))
                .dimensions("Gập mép 1cm")
                .quantity(new BigDecimal("10"))
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();

        when(cartRepository.findByUserId(10L)).thenReturn(Optional.of(cart));
        when(cartItemRepository.findByCartIdOrderByUpdatedAtDesc(1L)).thenReturn(List.of(item));
        when(permissionEvaluationService.canViewProductPrice()).thenReturn(true);

        CartResponse response = cartService.getCart(10L);

        assertThat(response).isNotNull();
        assertThat(response.cartId()).isEqualTo(1L);
        assertThat(response.totalItems()).isEqualTo(1);
        assertThat(response.items()).hasSize(1);
        assertThat(response.items().get(0).productName()).isEqualTo("Tôn sóng");
        assertThat(response.items().get(0).dimensions()).isEqualTo("Gập mép 1cm");
    }

    @Test
    @DisplayName("getCartCount - Trả về số lượng món chính xác")
    void getCartCount_success() {
        when(cartRepository.findByUserId(10L)).thenReturn(Optional.of(cart));
        when(cartItemRepository.countByCartId(1L)).thenReturn(5L);

        CartCountResponse response = cartService.getCartCount(10L);

        assertThat(response.totalCount()).isEqualTo(5L);
    }

    @Test
    @DisplayName("addItem - Thêm món mới thành công khi chưa có trong giỏ")
    void addItem_newProduct_success() {
        CartItemAddRequest request = new CartItemAddRequest(
                100L, 1001L, 1L, 2L, new BigDecimal("5.5"), "Gập mép 1cm", new BigDecimal("10")
        );

        when(productRepository.findByIdAndIsDeletedFalse(100L)).thenReturn(Optional.of(product));
        when(productVariantRepository.findByIdAndIsDeletedFalse(1001L)).thenReturn(Optional.of(variant));
        when(colorRepository.findByIdAndIsDeletedFalse(1L)).thenReturn(Optional.of(color));
        when(unitRepository.findByIdAndIsDeletedFalse(2L)).thenReturn(Optional.of(unit));
        when(cartRepository.findByUserId(10L)).thenReturn(Optional.of(cart));
        when(cartItemRepository.findByCartIdOrderByUpdatedAtDesc(1L)).thenReturn(new ArrayList<>());
        when(cartItemRepository.save(any(CartItem.class))).thenAnswer(invocation -> {
            CartItem saved = invocation.getArgument(0);
            saved.setId(99L);
            return saved;
        });
        when(permissionEvaluationService.canViewProductPrice()).thenReturn(false);

        CartItemResponse response = cartService.addItem(10L, request);

        assertThat(response).isNotNull();
        assertThat(response.cartItemId()).isEqualTo(99L);
        assertThat(response.quantity()).isEqualByComparingTo("10");
        assertThat(response.dimensions()).isEqualTo("Gập mép 1cm");
    }

    @Test
    @DisplayName("addItem - Tự động cộng dồn số lượng khi trùng khớp quy cách")
    void addItem_existingProductSameSpec_mergesQuantity() {
        CartItem existingItem = CartItem.builder()
                .id(11L)
                .cart(cart)
                .product(product)
                .variant(variant)
                .color(color)
                .unit(unit)
                .size(new BigDecimal("5.5"))
                .dimensions("Gập mép 1cm")
                .quantity(new BigDecimal("10"))
                .build();

        CartItemAddRequest request = new CartItemAddRequest(
                100L, 1001L, 1L, 2L, new BigDecimal("5.5"), "Gập mép 1cm", new BigDecimal("5")
        );

        when(productRepository.findByIdAndIsDeletedFalse(100L)).thenReturn(Optional.of(product));
        when(productVariantRepository.findByIdAndIsDeletedFalse(1001L)).thenReturn(Optional.of(variant));
        when(colorRepository.findByIdAndIsDeletedFalse(1L)).thenReturn(Optional.of(color));
        when(unitRepository.findByIdAndIsDeletedFalse(2L)).thenReturn(Optional.of(unit));
        when(cartRepository.findByUserId(10L)).thenReturn(Optional.of(cart));
        when(cartItemRepository.findByCartIdOrderByUpdatedAtDesc(1L)).thenReturn(List.of(existingItem));
        when(cartItemRepository.save(existingItem)).thenReturn(existingItem);
        when(permissionEvaluationService.canViewProductPrice()).thenReturn(false);

        CartItemResponse response = cartService.addItem(10L, request);

        assertThat(response).isNotNull();
        assertThat(existingItem.getQuantity()).isEqualByComparingTo("15");
    }

    @Test
    @DisplayName("addItem - Thất bại nếu sản phẩm không còn ACTIVE")
    void addItem_productInactive_throwsException() {
        product.setStatus(ProductStatus.INACTIVE);
        when(productRepository.findByIdAndIsDeletedFalse(100L)).thenReturn(Optional.of(product));

        CartItemAddRequest request = new CartItemAddRequest(100L, null, null, null, null, null, BigDecimal.ONE);

        assertThatThrownBy(() -> cartService.addItem(10L, request))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("tạm ngưng kinh doanh");
    }

    @Test
    @DisplayName("updateItem - Cập nhật số lượng và quy cách thành công")
    void updateItem_success() {
        CartItem item = CartItem.builder()
                .id(11L)
                .cart(cart)
                .product(product)
                .quantity(new BigDecimal("10"))
                .size(new BigDecimal("5.5"))
                .build();

        when(cartRepository.findByUserId(10L)).thenReturn(Optional.of(cart));
        when(cartItemRepository.findByIdAndCartId(11L, 1L)).thenReturn(Optional.of(item));
        when(cartItemRepository.save(item)).thenReturn(item);

        CartItemUpdateRequest updateReq = new CartItemUpdateRequest(new BigDecimal("20"), new BigDecimal("6.0"), "Mép 2cm");
        CartItemResponse response = cartService.updateItem(10L, 11L, updateReq);

        assertThat(response).isNotNull();
        assertThat(item.getQuantity()).isEqualByComparingTo("20");
        assertThat(item.getSize()).isEqualByComparingTo("6.0");
        assertThat(item.getDimensions()).isEqualTo("Mép 2cm");
    }

    @Test
    @DisplayName("removeItem - Xóa thành công món khỏi giỏ")
    void removeItem_success() {
        CartItem item = CartItem.builder().id(11L).cart(cart).build();
        when(cartRepository.findByUserId(10L)).thenReturn(Optional.of(cart));
        when(cartItemRepository.findByIdAndCartId(11L, 1L)).thenReturn(Optional.of(item));

        cartService.removeItem(10L, 11L);

        verify(cartItemRepository).delete(item);
    }

    @Test
    @DisplayName("clearCart - Làm trống toàn bộ giỏ thành công")
    void clearCart_success() {
        when(cartRepository.findByUserId(10L)).thenReturn(Optional.of(cart));

        cartService.clearCart(10L);

        verify(cartItemRepository).deleteAllByCartId(1L);
    }
}
