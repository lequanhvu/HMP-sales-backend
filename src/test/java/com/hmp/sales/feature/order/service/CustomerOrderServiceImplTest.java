package com.hmp.sales.feature.order.service;

import com.hmp.sales.common.exception.BusinessRuleException;
import com.hmp.sales.common.exception.InvalidOrderStateException;
import com.hmp.sales.common.exception.OtpVerificationException;
import com.hmp.sales.common.exception.ResourceNotFoundException;
import com.hmp.sales.common.response.PageResponse;
import com.hmp.sales.feature.order.dto.CancelOrderRequest;
import com.hmp.sales.feature.order.dto.CreateOrderRequest;
import com.hmp.sales.feature.order.dto.OrderDetailResponse;
import com.hmp.sales.feature.order.dto.OrderFilterRequest;
import com.hmp.sales.feature.order.dto.OrderItemRequest;
import com.hmp.sales.feature.order.dto.OrderSummaryResponse;
import com.hmp.sales.feature.order.entity.Order;
import com.hmp.sales.feature.order.entity.OrderItem;
import com.hmp.sales.feature.order.entity.OrderStatus;
import com.hmp.sales.feature.order.entity.OrderStatusHistory;
import com.hmp.sales.feature.order.repository.OrderItemRepository;
import com.hmp.sales.feature.order.repository.OrderRepository;
import com.hmp.sales.feature.order.repository.OrderStatusHistoryRepository;
import com.hmp.sales.feature.order.service.impl.CustomerOrderServiceImpl;
import com.hmp.sales.feature.otp.service.PhoneOtpService;
import com.hmp.sales.feature.product.entity.Product;
import com.hmp.sales.feature.product.entity.ProductStatus;
import com.hmp.sales.feature.product.entity.ProductVariant;
import com.hmp.sales.feature.product.entity.VariantStatus;
import com.hmp.sales.feature.product.repository.ProductRepository;
import com.hmp.sales.feature.product.repository.ProductVariantRepository;
import com.hmp.sales.feature.setting.service.SystemSettingService;
import com.hmp.sales.feature.user.entity.AuthProvider;
import com.hmp.sales.feature.user.entity.User;
import com.hmp.sales.feature.user.entity.UserStatus;
import com.hmp.sales.feature.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit test cho {@link CustomerOrderServiceImpl}.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("CustomerOrderServiceImpl Unit Tests")
class CustomerOrderServiceImplTest {

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private OrderItemRepository orderItemRepository;

    @Mock
    private OrderStatusHistoryRepository orderStatusHistoryRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private ProductRepository productRepository;

    @Mock
    private ProductVariantRepository productVariantRepository;

    @Mock
    private PhoneOtpService phoneOtpService;

    @Mock
    private SystemSettingService systemSettingService;

    @InjectMocks
    private CustomerOrderServiceImpl customerOrderService;

    private User sampleCustomer;
    private Product sampleProduct;
    private ProductVariant sampleVariant;
    private Order sampleOrder;

    @BeforeEach
    void setUp() {
        sampleCustomer = User.builder()
                .id(1L)
                .fullName("Nguyễn Văn A")
                .email("customer@example.com")
                .phone("0987654321")
                .authProvider(AuthProvider.GOOGLE)
                .status(UserStatus.ACTIVE)
                .isDeleted(false)
                .build();

        sampleProduct = Product.builder()
                .id(100L)
                .productName("Tôn sóng vuông Hoa Sen")
                .status(ProductStatus.ACTIVE)
                .isDeleted(false)
                .build();

        sampleVariant = ProductVariant.builder()
                .id(1001L)
                .product(sampleProduct)
                .variantCode("VAR-001")
                .status(VariantStatus.ACTIVE)
                .isDeleted(false)
                .build();

        sampleOrder = Order.builder()
                .id(10L)
                .customer(sampleCustomer)
                .status(OrderStatus.PENDING)
                .receiverName("Nguyễn Văn A")
                .receiverPhone("0987654321")
                .shippingAddress("123 Đường ABC")
                .createdAt(Instant.now())
                .isDeleted(false)
                .build();
    }

    @Test
    @DisplayName("Đặt hàng thành công khi SĐT có bảo chứng OTP trên Redis")
    void createOrder_success_whenPhoneVerifiedInRedis() {
        CreateOrderRequest request = new CreateOrderRequest(
                "Người Nhận Mới",
                "0912345678",
                "456 Đường XYZ",
                "Giao giờ hành chính",
                List.of(new OrderItemRequest(100L, 1001L, "Xanh", "m", new BigDecimal("10"), new BigDecimal("5")))
        );

        when(userRepository.findById(1L)).thenReturn(Optional.of(sampleCustomer));
        when(phoneOtpService.isVerified("0912345678")).thenReturn(true);
        when(productRepository.findById(100L)).thenReturn(Optional.of(sampleProduct));
        when(productVariantRepository.findById(1001L)).thenReturn(Optional.of(sampleVariant));

        when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> {
            Order o = invocation.getArgument(0);
            o.setId(10L);
            return o;
        });

        when(orderItemRepository.saveAll(any())).thenAnswer(invocation -> {
            List<OrderItem> items = invocation.getArgument(0);
            items.get(0).setId(1L);
            return items;
        });

        when(orderStatusHistoryRepository.save(any(OrderStatusHistory.class))).thenAnswer(invocation -> {
            OrderStatusHistory h = invocation.getArgument(0);
            h.setId(100L);
            return h;
        });

        OrderDetailResponse response = customerOrderService.createOrder(1L, request);

        assertThat(response).isNotNull();
        assertThat(response.orderId()).isEqualTo(10L);
        assertThat(response.status()).isEqualTo(OrderStatus.PENDING);
        assertThat(response.receiverName()).isEqualTo("Người Nhận Mới");
        assertThat(response.receiverPhone()).isEqualTo("0912345678");
        assertThat(response.items()).hasSize(1);
        assertThat(response.items().get(0).productName()).isEqualTo("Tôn sóng vuông Hoa Sen");
        verify(orderRepository).save(any(Order.class));
        verify(orderStatusHistoryRepository).save(any(OrderStatusHistory.class));
    }

    @Test
    @DisplayName("Đặt hàng thành công khi SĐT trùng khớp SĐT đã lưu trong tài khoản User")
    void createOrder_success_whenPhoneMatchesUserPhone() {
        CreateOrderRequest request = new CreateOrderRequest(
                "Nguyễn Văn A",
                "0987654321", // trùng sampleCustomer.getPhone()
                "123 Đường ABC",
                null,
                List.of(new OrderItemRequest(100L, null, null, "tấm", new BigDecimal("5"), null))
        );

        when(userRepository.findById(1L)).thenReturn(Optional.of(sampleCustomer));
        when(productRepository.findById(100L)).thenReturn(Optional.of(sampleProduct));
        when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> {
            Order o = invocation.getArgument(0);
            o.setId(10L);
            return o;
        });
        when(orderItemRepository.saveAll(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(orderStatusHistoryRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        OrderDetailResponse response = customerOrderService.createOrder(1L, request);
        assertThat(response).isNotNull();
        assertThat(response.status()).isEqualTo(OrderStatus.PENDING);
    }

    @Test
    @DisplayName("Ném OtpVerificationException khi SĐT chưa verify OTP và không khớp SĐT User")
    void createOrder_throwsOtpVerificationException_whenPhoneNotVerified() {
        CreateOrderRequest request = new CreateOrderRequest(
                "Khách Hàng",
                "0999999999",
                "Địa chỉ",
                null,
                List.of(new OrderItemRequest(100L, null, null, "m", new BigDecimal("1"), null))
        );

        when(userRepository.findById(1L)).thenReturn(Optional.of(sampleCustomer));
        when(phoneOtpService.isVerified("0999999999")).thenReturn(false);

        assertThatThrownBy(() -> customerOrderService.createOrder(1L, request))
                .isInstanceOf(OtpVerificationException.class)
                .hasMessageContaining("Số điện thoại nhận hàng chưa được xác thực");
    }

    @Test
    @DisplayName("Ném BusinessRuleException khi sản phẩm ở trạng thái INACTIVE")
    void createOrder_throwsBusinessRuleException_whenProductInactive() {
        sampleProduct.setStatus(ProductStatus.INACTIVE);

        CreateOrderRequest request = new CreateOrderRequest(
                "Nguyễn Văn A",
                "0987654321",
                "Địa chỉ",
                null,
                List.of(new OrderItemRequest(100L, null, null, "m", new BigDecimal("1"), null))
        );

        when(userRepository.findById(1L)).thenReturn(Optional.of(sampleCustomer));
        when(productRepository.findById(100L)).thenReturn(Optional.of(sampleProduct));

        assertThatThrownBy(() -> customerOrderService.createOrder(1L, request))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("hiện đang tạm ngừng kinh doanh");
    }

    @Test
    @DisplayName("Ném BusinessRuleException khi biến thể không thuộc sản phẩm")
    void createOrder_throwsBusinessRuleException_whenVariantNotBelongToProduct() {
        Product otherProduct = Product.builder().id(999L).productName("Sản phẩm khác").build();
        ProductVariant foreignVariant = ProductVariant.builder().id(2002L).product(otherProduct).status(VariantStatus.ACTIVE).build();

        CreateOrderRequest request = new CreateOrderRequest(
                "Nguyễn Văn A",
                "0987654321",
                "Địa chỉ",
                null,
                List.of(new OrderItemRequest(100L, 2002L, null, "m", new BigDecimal("1"), null))
        );

        when(userRepository.findById(1L)).thenReturn(Optional.of(sampleCustomer));
        when(productRepository.findById(100L)).thenReturn(Optional.of(sampleProduct));
        when(productVariantRepository.findById(2002L)).thenReturn(Optional.of(foreignVariant));

        assertThatThrownBy(() -> customerOrderService.createOrder(1L, request))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("không thuộc sản phẩm");
    }

    @Test
    @DisplayName("Lấy danh sách đơn hàng phân trang thành công")
    void getOwnOrders_returnsPagedOrders() {
        Pageable pageable = PageRequest.of(0, 10);
        Page<Order> orderPage = new PageImpl<>(List.of(sampleOrder), pageable, 1);

        when(orderRepository.findAll(any(Specification.class), eq(pageable))).thenReturn(orderPage);

        OrderFilterRequest filter = new OrderFilterRequest(null, null, null, 0, 10);
        PageResponse<OrderSummaryResponse> response = customerOrderService.getOwnOrders(1L, filter, pageable);

        assertThat(response).isNotNull();
        assertThat(response.result()).hasSize(1);
        assertThat(response.meta().totalElements()).isEqualTo(1);
    }

    @Test
    @DisplayName("Xem chi tiết đơn hàng thành công")
    void getOwnOrderDetail_success() {
        when(orderRepository.findByIdAndCustomerIdAndIsDeletedFalse(10L, 1L))
                .thenReturn(Optional.of(sampleOrder));
        when(orderItemRepository.findByOrderIdAndIsDeletedFalse(10L))
                .thenReturn(Collections.emptyList());
        when(orderStatusHistoryRepository.findByOrderIdOrderByCreatedAtAsc(10L))
                .thenReturn(Collections.emptyList());

        OrderDetailResponse response = customerOrderService.getOwnOrderDetail(1L, 10L);
        assertThat(response).isNotNull();
        assertThat(response.orderId()).isEqualTo(10L);
        assertThat(response.status()).isEqualTo(OrderStatus.PENDING);
    }

    @Test
    @DisplayName("Hủy đơn hàng thành công trong hạn cho phép (PENDING)")
    void cancelOwnOrder_success_withinCancellationWindow() {
        sampleOrder.setStatus(OrderStatus.PENDING);
        sampleOrder.setCreatedAt(Instant.now().minus(5, ChronoUnit.MINUTES));

        when(orderRepository.findByIdAndCustomerIdAndIsDeletedFalse(10L, 1L)).thenReturn(Optional.of(sampleOrder));
        when(userRepository.findById(1L)).thenReturn(Optional.of(sampleCustomer));
        when(systemSettingService.getCancellationWindowMinutes()).thenReturn(30);
        when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> invocation.getArgument(0));

        CancelOrderRequest cancelReq = new CancelOrderRequest("Thay đổi kế hoạch xây dựng");
        OrderDetailResponse response = customerOrderService.cancelOwnOrder(1L, 10L, cancelReq);

        assertThat(response).isNotNull();
        assertThat(sampleOrder.getStatus()).isEqualTo(OrderStatus.CANCELLED);
        assertThat(sampleOrder.getCancellationReason()).isEqualTo("Thay đổi kế hoạch xây dựng");
        verify(orderStatusHistoryRepository).save(any(OrderStatusHistory.class));
    }

    @Test
    @DisplayName("Ném InvalidOrderStateException khi đơn hàng không ở trạng thái được phép hủy")
    void cancelOwnOrder_throwsInvalidOrderStateException_whenStatusNotCancellable() {
        sampleOrder.setStatus(OrderStatus.PROCESSING);

        when(orderRepository.findByIdAndCustomerIdAndIsDeletedFalse(10L, 1L)).thenReturn(Optional.of(sampleOrder));
        when(userRepository.findById(1L)).thenReturn(Optional.of(sampleCustomer));

        assertThatThrownBy(() -> customerOrderService.cancelOwnOrder(1L, 10L, null))
                .isInstanceOf(InvalidOrderStateException.class)
                .hasMessageContaining("Không thể hủy đơn hàng đang ở trạng thái");
    }

    @Test
    @DisplayName("Ném BusinessRuleException khi đã quá thời hạn cancellationWindowMinutes cho phép hủy")
    void cancelOwnOrder_throwsBusinessRuleException_whenWindowExpired() {
        sampleOrder.setStatus(OrderStatus.PENDING);
        sampleOrder.setCreatedAt(Instant.now().minus(35, ChronoUnit.MINUTES)); // Đã quá 30 phút

        when(orderRepository.findByIdAndCustomerIdAndIsDeletedFalse(10L, 1L)).thenReturn(Optional.of(sampleOrder));
        when(userRepository.findById(1L)).thenReturn(Optional.of(sampleCustomer));
        when(systemSettingService.getCancellationWindowMinutes()).thenReturn(30);

        assertThatThrownBy(() -> customerOrderService.cancelOwnOrder(1L, 10L, null))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("Đã quá thời hạn 30 phút cho phép hủy đơn hàng");
    }
}

