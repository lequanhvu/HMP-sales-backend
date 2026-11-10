package com.hmp.sales.feature.order.service;

import com.hmp.sales.common.exception.BusinessRuleException;
import com.hmp.sales.common.exception.InvalidOrderStateException;
import com.hmp.sales.common.exception.OtpVerificationException;
import com.hmp.sales.common.exception.ResourceNotFoundException;
import com.hmp.sales.common.response.PageResponse;
import com.hmp.sales.feature.notification.event.OrderStatusChangedEvent;
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
import com.hmp.sales.feature.product.entity.Color;
import com.hmp.sales.feature.product.entity.ColorStatus;
import com.hmp.sales.feature.product.entity.Product;
import com.hmp.sales.feature.product.entity.ProductPrice;
import com.hmp.sales.feature.product.entity.ProductStatus;
import com.hmp.sales.feature.product.entity.ProductVariant;
import com.hmp.sales.feature.product.entity.Unit;
import com.hmp.sales.feature.product.entity.UnitStatus;
import com.hmp.sales.feature.product.entity.VariantStatus;
import com.hmp.sales.feature.product.repository.ColorRepository;
import com.hmp.sales.feature.product.repository.ProductColorRepository;
import com.hmp.sales.feature.product.repository.ProductPriceRepository;
import com.hmp.sales.feature.product.repository.ProductRepository;
import com.hmp.sales.feature.product.repository.ProductVariantRepository;
import com.hmp.sales.feature.product.repository.UnitRepository;
import com.hmp.sales.feature.setting.service.SystemSettingService;
import com.hmp.sales.feature.user.entity.AuthProvider;
import com.hmp.sales.feature.user.entity.CustomerAddress;
import com.hmp.sales.feature.user.entity.User;
import com.hmp.sales.feature.user.entity.UserStatus;
import com.hmp.sales.feature.user.repository.CustomerAddressRepository;
import com.hmp.sales.feature.user.repository.UserRepository;
import com.hmp.sales.security.PermissionEvaluationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
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
    private ColorRepository colorRepository;

    @Mock
    private UnitRepository unitRepository;

    @Mock
    private ProductColorRepository productColorRepository;

    @Mock
    private ProductPriceRepository productPriceRepository;

    @Mock
    private PhoneOtpService phoneOtpService;

    @Mock
    private SystemSettingService systemSettingService;

    @Mock
    private CustomerAddressRepository customerAddressRepository;

    @Mock
    private PermissionEvaluationService permissionEvaluationService;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    @InjectMocks
    private CustomerOrderServiceImpl customerOrderService;

    private User sampleCustomer;
    private Product sampleProduct;
    private ProductVariant sampleVariant;
    private Color sampleColor;
    private Unit sampleUnit;
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
                .variantName("Tôn 0.35mm - 5 sóng")
                .status(VariantStatus.ACTIVE)
                .isDeleted(false)
                .build();

        sampleColor = Color.builder()
                .id(1L)
                .colorName("Xanh ngọc")
                .status(ColorStatus.ACTIVE)
                .isDeleted(false)
                .build();

        sampleUnit = Unit.builder()
                .id(2L)
                .unitName("m")
                .status(UnitStatus.ACTIVE)
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
    @DisplayName("Đặt hàng thành công khi SĐT có bảo chứng OTP trên Redis và tự động đặt địa chỉ làm mặc định nếu chưa mặc định")
    void createOrder_success_whenPhoneVerifiedInRedis() {
        CustomerAddress savedAddress = CustomerAddress.builder()
                .id(5L)
                .customer(sampleCustomer)
                .addressLine("456 Đường XYZ")
                .isDefault(false)
                .build();

        CreateOrderRequest request = new CreateOrderRequest(
                "Người Nhận Mới",
                "0912345678",
                5L,
                "Giao giờ hành chính",
                List.of(new OrderItemRequest(100L, 1001L, 1L, 2L, new BigDecimal("10"), new BigDecimal("5")))
        );

        when(userRepository.findById(1L)).thenReturn(Optional.of(sampleCustomer));
        when(phoneOtpService.isVerified("0912345678")).thenReturn(true);
        when(customerAddressRepository.findByIdAndCustomerIdAndIsDeletedFalse(5L, 1L))
                .thenReturn(Optional.of(savedAddress));
        when(productRepository.findById(100L)).thenReturn(Optional.of(sampleProduct));
        when(productVariantRepository.findById(1001L)).thenReturn(Optional.of(sampleVariant));
        when(colorRepository.findByIdAndIsDeletedFalse(1L)).thenReturn(Optional.of(sampleColor));
        when(unitRepository.findByIdAndIsDeletedFalse(2L)).thenReturn(Optional.of(sampleUnit));
        when(productColorRepository.existsByVariantIdAndColorId(1001L, 1L)).thenReturn(true);
        when(productPriceRepository.existsByVariantIdAndUnitId(1001L, 2L)).thenReturn(true);

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
        assertThat(response.shippingAddress()).isEqualTo("456 Đường XYZ");
        assertThat(response.items()).hasSize(1);
        assertThat(response.items().get(0).productName()).isEqualTo("Tôn sóng vuông Hoa Sen");
        assertThat(response.items().get(0).variantName()).isEqualTo("Tôn 0.35mm - 5 sóng");
        assertThat(response.items().get(0).color()).isEqualTo("Xanh ngọc");
        assertThat(response.items().get(0).unit()).isEqualTo("m");

        verify(customerAddressRepository).resetDefaultAddress(1L);
        verify(customerAddressRepository).save(savedAddress);
        assertThat(savedAddress.getIsDefault()).isTrue();
        verify(orderRepository).save(any(Order.class));
        verify(orderStatusHistoryRepository).save(any(OrderStatusHistory.class));
    }

    @Test
    @DisplayName("Đặt hàng thành công khi SĐT trùng khớp SĐT đã lưu trong tài khoản User và địa chỉ đã là mặc định")
    void createOrder_success_whenPhoneMatchesUserPhone() {
        CustomerAddress savedAddress = CustomerAddress.builder()
                .id(5L)
                .customer(sampleCustomer)
                .addressLine("123 Đường ABC")
                .isDefault(true)
                .build();

        CreateOrderRequest request = new CreateOrderRequest(
                "Nguyễn Văn A",
                "0987654321", // trùng sampleCustomer.getPhone()
                5L,
                null,
                List.of(new OrderItemRequest(100L, null, null, null, new BigDecimal("5"), null))
        );

        when(userRepository.findById(1L)).thenReturn(Optional.of(sampleCustomer));
        when(customerAddressRepository.findByIdAndCustomerIdAndIsDeletedFalse(5L, 1L))
                .thenReturn(Optional.of(savedAddress));
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
        assertThat(response.shippingAddress()).isEqualTo("123 Đường ABC");
    }

    @Test
    @DisplayName("Ném BusinessRuleException khi địa chỉ không có ID")
    void createOrder_throwsBusinessRuleException_whenAddressIdNull() {
        CreateOrderRequest request = new CreateOrderRequest(
                "Nguyễn Văn A",
                "0987654321",
                null,
                null,
                List.of(new OrderItemRequest(100L, null, null, null, new BigDecimal("1"), null))
        );

        when(userRepository.findById(1L)).thenReturn(Optional.of(sampleCustomer));

        assertThatThrownBy(() -> customerOrderService.createOrder(1L, request))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("Địa chỉ nhận hàng không được để trống.");
    }

    @Test
    @DisplayName("Ném ResourceNotFoundException khi địa chỉ nhận hàng ID không tồn tại")
    void createOrder_throwsResourceNotFoundException_whenAddressNotFound() {
        CreateOrderRequest request = new CreateOrderRequest(
                "Nguyễn Văn A",
                "0987654321",
                999L,
                null,
                List.of(new OrderItemRequest(100L, null, null, null, new BigDecimal("1"), null))
        );

        when(userRepository.findById(1L)).thenReturn(Optional.of(sampleCustomer));
        when(customerAddressRepository.findByIdAndCustomerIdAndIsDeletedFalse(999L, 1L))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> customerOrderService.createOrder(1L, request))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Không tìm thấy địa chỉ nhận hàng ID: 999");
    }

    @Test
    @DisplayName("Ném OtpVerificationException khi SĐT chưa verify OTP và không khớp SĐT User")
    void createOrder_throwsOtpVerificationException_whenPhoneNotVerified() {
        CreateOrderRequest request = new CreateOrderRequest(
                "Khách Hàng",
                "0999999999",
                5L,
                null,
                List.of(new OrderItemRequest(100L, null, null, null, new BigDecimal("1"), null))
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

        CustomerAddress savedAddress = CustomerAddress.builder()
                .id(5L)
                .customer(sampleCustomer)
                .addressLine("Địa chỉ")
                .isDefault(true)
                .build();

        CreateOrderRequest request = new CreateOrderRequest(
                "Nguyễn Văn A",
                "0987654321",
                5L,
                null,
                List.of(new OrderItemRequest(100L, null, null, null, new BigDecimal("1"), null))
        );

        when(userRepository.findById(1L)).thenReturn(Optional.of(sampleCustomer));
        when(customerAddressRepository.findByIdAndCustomerIdAndIsDeletedFalse(5L, 1L))
                .thenReturn(Optional.of(savedAddress));
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

        CustomerAddress savedAddress = CustomerAddress.builder()
                .id(5L)
                .customer(sampleCustomer)
                .addressLine("Địa chỉ")
                .isDefault(true)
                .build();

        CreateOrderRequest request = new CreateOrderRequest(
                "Nguyễn Văn A",
                "0987654321",
                5L,
                null,
                List.of(new OrderItemRequest(100L, 2002L, null, null, new BigDecimal("1"), null))
        );

        when(userRepository.findById(1L)).thenReturn(Optional.of(sampleCustomer));
        when(customerAddressRepository.findByIdAndCustomerIdAndIsDeletedFalse(5L, 1L))
                .thenReturn(Optional.of(savedAddress));
        when(productRepository.findById(100L)).thenReturn(Optional.of(sampleProduct));
        when(productVariantRepository.findById(2002L)).thenReturn(Optional.of(foreignVariant));

        assertThatThrownBy(() -> customerOrderService.createOrder(1L, request))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("không thuộc sản phẩm");
    }

    @Test
    @DisplayName("Ném ResourceNotFoundException khi màu sắc colorId không tồn tại")
    void createOrder_throwsResourceNotFoundException_whenColorNotFound() {
        CustomerAddress savedAddress = CustomerAddress.builder().id(5L).customer(sampleCustomer).addressLine("Địa chỉ").isDefault(true).build();
        CreateOrderRequest request = new CreateOrderRequest(
                "Nguyễn Văn A", "0987654321", 5L, null,
                List.of(new OrderItemRequest(100L, 1001L, 999L, null, new BigDecimal("1"), null))
        );

        when(userRepository.findById(1L)).thenReturn(Optional.of(sampleCustomer));
        when(customerAddressRepository.findByIdAndCustomerIdAndIsDeletedFalse(5L, 1L)).thenReturn(Optional.of(savedAddress));
        when(productRepository.findById(100L)).thenReturn(Optional.of(sampleProduct));
        when(productVariantRepository.findById(1001L)).thenReturn(Optional.of(sampleVariant));
        when(colorRepository.findByIdAndIsDeletedFalse(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> customerOrderService.createOrder(1L, request))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Không tìm thấy màu sắc ID: 999");
    }

    @Test
    @DisplayName("Ném BusinessRuleException khi màu sắc colorId đang bị khóa")
    void createOrder_throwsBusinessRuleException_whenColorLocked() {
        sampleColor.setStatus(ColorStatus.LOCKED);
        CustomerAddress savedAddress = CustomerAddress.builder().id(5L).customer(sampleCustomer).addressLine("Địa chỉ").isDefault(true).build();
        CreateOrderRequest request = new CreateOrderRequest(
                "Nguyễn Văn A", "0987654321", 5L, null,
                List.of(new OrderItemRequest(100L, 1001L, 1L, null, new BigDecimal("1"), null))
        );

        when(userRepository.findById(1L)).thenReturn(Optional.of(sampleCustomer));
        when(customerAddressRepository.findByIdAndCustomerIdAndIsDeletedFalse(5L, 1L)).thenReturn(Optional.of(savedAddress));
        when(productRepository.findById(100L)).thenReturn(Optional.of(sampleProduct));
        when(productVariantRepository.findById(1001L)).thenReturn(Optional.of(sampleVariant));
        when(colorRepository.findByIdAndIsDeletedFalse(1L)).thenReturn(Optional.of(sampleColor));

        assertThatThrownBy(() -> customerOrderService.createOrder(1L, request))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("Màu sắc 'Xanh ngọc' hiện đang bị khóa.");
    }

    @Test
    @DisplayName("Ném BusinessRuleException khi màu sắc không thuộc biến thể của sản phẩm")
    void createOrder_throwsBusinessRuleException_whenColorNotBelongToVariant() {
        CustomerAddress savedAddress = CustomerAddress.builder().id(5L).customer(sampleCustomer).addressLine("Địa chỉ").isDefault(true).build();
        CreateOrderRequest request = new CreateOrderRequest(
                "Nguyễn Văn A", "0987654321", 5L, null,
                List.of(new OrderItemRequest(100L, 1001L, 1L, null, new BigDecimal("1"), null))
        );

        when(userRepository.findById(1L)).thenReturn(Optional.of(sampleCustomer));
        when(customerAddressRepository.findByIdAndCustomerIdAndIsDeletedFalse(5L, 1L)).thenReturn(Optional.of(savedAddress));
        when(productRepository.findById(100L)).thenReturn(Optional.of(sampleProduct));
        when(productVariantRepository.findById(1001L)).thenReturn(Optional.of(sampleVariant));
        when(colorRepository.findByIdAndIsDeletedFalse(1L)).thenReturn(Optional.of(sampleColor));
        when(productColorRepository.existsByVariantIdAndColorId(1001L, 1L)).thenReturn(false);

        assertThatThrownBy(() -> customerOrderService.createOrder(1L, request))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("không thuộc biến thể ID 1001");
    }

    @Test
    @DisplayName("Ném BusinessRuleException khi chọn màu sắc nhưng sản phẩm chưa chọn biến thể")
    void createOrder_throwsBusinessRuleException_whenColorSelectedWithoutVariant() {
        CustomerAddress savedAddress = CustomerAddress.builder().id(5L).customer(sampleCustomer).addressLine("Địa chỉ").isDefault(true).build();
        CreateOrderRequest request = new CreateOrderRequest(
                "Nguyễn Văn A", "0987654321", 5L, null,
                List.of(new OrderItemRequest(100L, null, 1L, null, new BigDecimal("1"), null))
        );

        when(userRepository.findById(1L)).thenReturn(Optional.of(sampleCustomer));
        when(customerAddressRepository.findByIdAndCustomerIdAndIsDeletedFalse(5L, 1L)).thenReturn(Optional.of(savedAddress));
        when(productRepository.findById(100L)).thenReturn(Optional.of(sampleProduct));
        when(colorRepository.findByIdAndIsDeletedFalse(1L)).thenReturn(Optional.of(sampleColor));

        assertThatThrownBy(() -> customerOrderService.createOrder(1L, request))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("chưa chọn biến thể, không thể chọn màu sắc");
    }

    @Test
    @DisplayName("Ném ResourceNotFoundException khi đơn vị tính unitId không tồn tại")
    void createOrder_throwsResourceNotFoundException_whenUnitNotFound() {
        CustomerAddress savedAddress = CustomerAddress.builder().id(5L).customer(sampleCustomer).addressLine("Địa chỉ").isDefault(true).build();
        CreateOrderRequest request = new CreateOrderRequest(
                "Nguyễn Văn A", "0987654321", 5L, null,
                List.of(new OrderItemRequest(100L, 1001L, null, 999L, new BigDecimal("1"), null))
        );

        when(userRepository.findById(1L)).thenReturn(Optional.of(sampleCustomer));
        when(customerAddressRepository.findByIdAndCustomerIdAndIsDeletedFalse(5L, 1L)).thenReturn(Optional.of(savedAddress));
        when(productRepository.findById(100L)).thenReturn(Optional.of(sampleProduct));
        when(productVariantRepository.findById(1001L)).thenReturn(Optional.of(sampleVariant));
        when(unitRepository.findByIdAndIsDeletedFalse(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> customerOrderService.createOrder(1L, request))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Không tìm thấy đơn vị tính ID: 999");
    }

    @Test
    @DisplayName("Ném BusinessRuleException khi đơn vị tính unitId đang bị khóa")
    void createOrder_throwsBusinessRuleException_whenUnitLocked() {
        sampleUnit.setStatus(UnitStatus.LOCKED);
        CustomerAddress savedAddress = CustomerAddress.builder().id(5L).customer(sampleCustomer).addressLine("Địa chỉ").isDefault(true).build();
        CreateOrderRequest request = new CreateOrderRequest(
                "Nguyễn Văn A", "0987654321", 5L, null,
                List.of(new OrderItemRequest(100L, 1001L, null, 2L, new BigDecimal("1"), null))
        );

        when(userRepository.findById(1L)).thenReturn(Optional.of(sampleCustomer));
        when(customerAddressRepository.findByIdAndCustomerIdAndIsDeletedFalse(5L, 1L)).thenReturn(Optional.of(savedAddress));
        when(productRepository.findById(100L)).thenReturn(Optional.of(sampleProduct));
        when(productVariantRepository.findById(1001L)).thenReturn(Optional.of(sampleVariant));
        when(unitRepository.findByIdAndIsDeletedFalse(2L)).thenReturn(Optional.of(sampleUnit));

        assertThatThrownBy(() -> customerOrderService.createOrder(1L, request))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("Đơn vị tính 'm' hiện đang bị khóa.");
    }

    @Test
    @DisplayName("Ném BusinessRuleException khi đơn vị tính không thuộc biến thể của sản phẩm")
    void createOrder_throwsBusinessRuleException_whenUnitNotBelongToVariant() {
        CustomerAddress savedAddress = CustomerAddress.builder().id(5L).customer(sampleCustomer).addressLine("Địa chỉ").isDefault(true).build();
        CreateOrderRequest request = new CreateOrderRequest(
                "Nguyễn Văn A", "0987654321", 5L, null,
                List.of(new OrderItemRequest(100L, 1001L, null, 2L, new BigDecimal("1"), null))
        );

        when(userRepository.findById(1L)).thenReturn(Optional.of(sampleCustomer));
        when(customerAddressRepository.findByIdAndCustomerIdAndIsDeletedFalse(5L, 1L)).thenReturn(Optional.of(savedAddress));
        when(productRepository.findById(100L)).thenReturn(Optional.of(sampleProduct));
        when(productVariantRepository.findById(1001L)).thenReturn(Optional.of(sampleVariant));
        when(unitRepository.findByIdAndIsDeletedFalse(2L)).thenReturn(Optional.of(sampleUnit));
        when(productPriceRepository.existsByVariantIdAndUnitId(1001L, 2L)).thenReturn(false);

        assertThatThrownBy(() -> customerOrderService.createOrder(1L, request))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("không thuộc biến thể ID 1001");
    }

    @Test
    @DisplayName("Ném BusinessRuleException khi chọn đơn vị tính nhưng sản phẩm chưa chọn biến thể")
    void createOrder_throwsBusinessRuleException_whenUnitSelectedWithoutVariant() {
        CustomerAddress savedAddress = CustomerAddress.builder().id(5L).customer(sampleCustomer).addressLine("Địa chỉ").isDefault(true).build();
        CreateOrderRequest request = new CreateOrderRequest(
                "Nguyễn Văn A", "0987654321", 5L, null,
                List.of(new OrderItemRequest(100L, null, null, 2L, new BigDecimal("1"), null))
        );

        when(userRepository.findById(1L)).thenReturn(Optional.of(sampleCustomer));
        when(customerAddressRepository.findByIdAndCustomerIdAndIsDeletedFalse(5L, 1L)).thenReturn(Optional.of(savedAddress));
        when(productRepository.findById(100L)).thenReturn(Optional.of(sampleProduct));
        when(unitRepository.findByIdAndIsDeletedFalse(2L)).thenReturn(Optional.of(sampleUnit));

        assertThatThrownBy(() -> customerOrderService.createOrder(1L, request))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("chưa chọn biến thể, không thể chọn đơn vị tính");
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
    @DisplayName("Ném BusinessRuleException khi dateFrom lớn hơn dateTo")
    void getOwnOrders_throwsBusinessRuleException_whenDateFromAfterDateTo() {
        OrderFilterRequest filter = new OrderFilterRequest(
                LocalDate.of(2026, 11, 10),
                LocalDate.of(2026, 11, 1),
                null, 0, 10
        );
        Pageable pageable = PageRequest.of(0, 10);

        assertThatThrownBy(() -> customerOrderService.getOwnOrders(1L, filter, pageable))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("Ngày bắt đầu");
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
    @DisplayName("Hủy đơn hàng thành công với trạng thái pending (PENDING)")
    void cancelOwnOrder_success_withPendingStatus_withinCancellationWindow() {
        sampleOrder.setStatus(OrderStatus.PENDING);
        sampleOrder.setCreatedAt(Instant.now().minus(35, ChronoUnit.MINUTES));

        when(orderRepository.findByIdAndCustomerIdAndIsDeletedFalse(10L, 1L)).thenReturn(Optional.of(sampleOrder));
        when(userRepository.findById(1L)).thenReturn(Optional.of(sampleCustomer));
        when(systemSettingService.getCancellationWindowMinutes()).thenReturn(30);
        when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> invocation.getArgument(0));

        CancelOrderRequest cancelReq = new CancelOrderRequest("");
        OrderDetailResponse response = customerOrderService.cancelOwnOrder(1L, 10L, cancelReq);

        assertThat(response).isNotNull();
        assertThat(sampleOrder.getStatus()).isEqualTo(OrderStatus.CANCELLED);
        assertThat(sampleOrder.getCancellationReason()).isEqualTo(null);
        verify(orderStatusHistoryRepository).save(any(OrderStatusHistory.class));
    }

    @Test
    @DisplayName("Hủy đơn hàng thành công trong hạn cho phép (CONFIRMED)")
    void cancelOwnOrder_success_withConfirmedStatus_withinCancellationWindow() {
        sampleOrder.setStatus(OrderStatus.CONFIRMED);
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
        sampleOrder.setStatus(OrderStatus.CONFIRMED);
        sampleOrder.setCreatedAt(Instant.now().minus(35, ChronoUnit.MINUTES)); // Đã quá 30 phút

        when(orderRepository.findByIdAndCustomerIdAndIsDeletedFalse(10L, 1L)).thenReturn(Optional.of(sampleOrder));
        when(userRepository.findById(1L)).thenReturn(Optional.of(sampleCustomer));
        when(systemSettingService.getCancellationWindowMinutes()).thenReturn(30);

        assertThatThrownBy(() -> customerOrderService.cancelOwnOrder(1L, 10L, null))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("Đã quá thời hạn 30 phút cho phép hủy đơn hàng");
    }

    @Test
    @DisplayName("Đặt hàng thành công và tự động tính giá theo niêm yết khi khách hàng CÓ quyền xem giá (PRODUCT_PRICE_VIEW)")
    void createOrder_autoCalculatesPrice_whenCustomerCanViewPrice() {
        CustomerAddress savedAddress = CustomerAddress.builder()
                .id(5L)
                .customer(sampleCustomer)
                .addressLine("456 Đường XYZ")
                .isDefault(true)
                .build();

        CreateOrderRequest request = new CreateOrderRequest(
                "Người Nhận",
                "0912345678",
                5L,
                null,
                List.of(new OrderItemRequest(100L, 1001L, 1L, 2L, new BigDecimal("10"), new BigDecimal("5")))
        );

        when(userRepository.findById(1L)).thenReturn(Optional.of(sampleCustomer));
        when(phoneOtpService.isVerified("0912345678")).thenReturn(true);
        when(customerAddressRepository.findByIdAndCustomerIdAndIsDeletedFalse(5L, 1L)).thenReturn(Optional.of(savedAddress));
        when(productRepository.findById(100L)).thenReturn(Optional.of(sampleProduct));
        when(productVariantRepository.findById(1001L)).thenReturn(Optional.of(sampleVariant));
        when(colorRepository.findByIdAndIsDeletedFalse(1L)).thenReturn(Optional.of(sampleColor));
        when(unitRepository.findByIdAndIsDeletedFalse(2L)).thenReturn(Optional.of(sampleUnit));
        when(productColorRepository.existsByVariantIdAndColorId(1001L, 1L)).thenReturn(true);
        when(productPriceRepository.existsByVariantIdAndUnitId(1001L, 2L)).thenReturn(true);

        // Khách hàng CÓ quyền xem giá
        when(permissionEvaluationService.canViewProductPrice()).thenReturn(true);
        ProductPrice pp = ProductPrice.builder().price(100_000).build();
        when(productPriceRepository.findByVariantIdAndUnitId(1001L, 2L)).thenReturn(Optional.of(pp));

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
        // 100,000 * 5m * 10 tấm = 5,000,000 đ
        assertThat(response.totalAmount()).isEqualTo(5_000_000L);
        assertThat(response.subtotalAmount()).isEqualTo(5_000_000L);
        assertThat(response.items().get(0).unitPrice()).isEqualTo(100_000);
        assertThat(response.items().get(0).totalPrice()).isEqualTo(5_000_000L);
    }

    @Test
    @DisplayName("Xem chi tiết đơn hàng: Ẩn giá khi đơn PENDING và khách hàng KHÔNG CÓ quyền xem giá")
    void getOwnOrderDetail_masksPrice_whenPendingAndCannotViewPrice() {
        sampleOrder.setStatus(OrderStatus.PENDING);
        sampleOrder.setSubtotalAmount(5_000_000L);
        sampleOrder.setTotalAmount(5_000_000L);

        OrderItem itemWithPrice = OrderItem.builder()
                .id(1L)
                .order(sampleOrder)
                .product(sampleProduct)
                .unitPrice(100_000)
                .totalPrice(5_000_000L)
                .productNameSnapshot("Tôn sóng")
                .quantity(BigDecimal.TEN)
                .build();

        when(orderRepository.findByIdAndCustomerIdAndIsDeletedFalse(10L, 1L)).thenReturn(Optional.of(sampleOrder));
        when(orderItemRepository.findByOrderIdAndIsDeletedFalse(10L)).thenReturn(List.of(itemWithPrice));
        when(orderStatusHistoryRepository.findByOrderIdOrderByCreatedAtAsc(10L)).thenReturn(List.of());
        when(permissionEvaluationService.canViewProductPrice()).thenReturn(false);

        OrderDetailResponse response = customerOrderService.getOwnOrderDetail(1L, 10L);

        assertThat(response.totalAmount()).isNull();
        assertThat(response.subtotalAmount()).isNull();
        assertThat(response.items().get(0).unitPrice()).isNull();
        assertThat(response.items().get(0).totalPrice()).isNull();
    }

    @Test
    @DisplayName("Xem chi tiết đơn hàng: Hiển thị giá khi đơn đã CONFIRMED cho dù khách hàng KHÔNG CÓ quyền xem giá")
    void getOwnOrderDetail_showsPrice_whenConfirmedEvenIfCannotViewPrice() {
        sampleOrder.setStatus(OrderStatus.CONFIRMED);
        sampleOrder.setSubtotalAmount(5_000_000L);
        sampleOrder.setTotalAmount(5_000_000L);

        OrderItem itemWithPrice = OrderItem.builder()
                .id(1L)
                .order(sampleOrder)
                .product(sampleProduct)
                .unitPrice(100_000)
                .totalPrice(5_000_000L)
                .productNameSnapshot("Tôn sóng")
                .quantity(BigDecimal.TEN)
                .build();

        when(orderRepository.findByIdAndCustomerIdAndIsDeletedFalse(10L, 1L)).thenReturn(Optional.of(sampleOrder));
        when(orderItemRepository.findByOrderIdAndIsDeletedFalse(10L)).thenReturn(List.of(itemWithPrice));
        when(orderStatusHistoryRepository.findByOrderIdOrderByCreatedAtAsc(10L)).thenReturn(List.of());
        when(permissionEvaluationService.canViewProductPrice()).thenReturn(false);

        OrderDetailResponse response = customerOrderService.getOwnOrderDetail(1L, 10L);

        // Đơn đã CONFIRMED -> khách thấy giá
        assertThat(response.totalAmount()).isEqualTo(5_000_000L);
        assertThat(response.items().get(0).unitPrice()).isEqualTo(100_000);
        assertThat(response.items().get(0).totalPrice()).isEqualTo(5_000_000L);
    }

    @Test
    @DisplayName("Hoàn tác hủy đơn thành công: Khôi phục về trạng thái PENDING")
    void undoCancelOrder_success_restoresToPending() {
        sampleOrder.setStatus(OrderStatus.CANCELLED);
        sampleOrder.setCancelledBy(sampleCustomer);
        sampleOrder.setCancelledAt(Instant.now().minus(10, ChronoUnit.MINUTES));
        sampleOrder.setCancellationReason("Lỡ tay nhấn hủy");

        OrderStatusHistory cancelHistory = OrderStatusHistory.builder()
                .id(1L)
                .order(sampleOrder)
                .fromStatus(OrderStatus.PENDING)
                .toStatus(OrderStatus.CANCELLED)
                .changedBy(sampleCustomer)
                .createdAt(Instant.now().minus(10, ChronoUnit.MINUTES))
                .build();

        when(orderRepository.findByIdAndCustomerIdAndIsDeletedFalse(10L, 1L)).thenReturn(Optional.of(sampleOrder));
        when(userRepository.findById(1L)).thenReturn(Optional.of(sampleCustomer));
        when(systemSettingService.getUndoCancelWindowMinutes()).thenReturn(60);
        when(orderStatusHistoryRepository.findTopByOrderIdAndToStatusAndIsDeletedFalseOrderByCreatedAtDesc(10L, OrderStatus.CANCELLED))
                .thenReturn(Optional.of(cancelHistory));
        when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> invocation.getArgument(0));

        OrderDetailResponse response = customerOrderService.undoCancelOrder(1L, 10L);

        assertThat(response).isNotNull();
        assertThat(sampleOrder.getStatus()).isEqualTo(OrderStatus.PENDING);
        assertThat(sampleOrder.getCancelledBy()).isNull();
        assertThat(sampleOrder.getCancelledAt()).isNull();
        assertThat(sampleOrder.getCancellationReason()).isNull();

        verify(orderStatusHistoryRepository).save(any(OrderStatusHistory.class));
        verify(eventPublisher).publishEvent(any(OrderStatusChangedEvent.class));
    }

    @Test
    @DisplayName("Hoàn tác hủy đơn thành công: Khôi phục về CONFIRMED và giữ nguyên confirmedAt cũ")
    void undoCancelOrder_success_restoresToConfirmed_preservesConfirmedAt() {
        Instant originalConfirmedAt = Instant.now().minus(40, ChronoUnit.MINUTES);
        sampleOrder.setStatus(OrderStatus.CANCELLED);
        sampleOrder.setConfirmedAt(originalConfirmedAt);
        sampleOrder.setCancelledBy(sampleCustomer);
        sampleOrder.setCancelledAt(Instant.now().minus(5, ChronoUnit.MINUTES));

        OrderStatusHistory cancelHistory = OrderStatusHistory.builder()
                .id(2L)
                .order(sampleOrder)
                .fromStatus(OrderStatus.CONFIRMED)
                .toStatus(OrderStatus.CANCELLED)
                .changedBy(sampleCustomer)
                .createdAt(Instant.now().minus(5, ChronoUnit.MINUTES))
                .build();

        when(orderRepository.findByIdAndCustomerIdAndIsDeletedFalse(10L, 1L)).thenReturn(Optional.of(sampleOrder));
        when(userRepository.findById(1L)).thenReturn(Optional.of(sampleCustomer));
        when(systemSettingService.getUndoCancelWindowMinutes()).thenReturn(60);
        when(orderStatusHistoryRepository.findTopByOrderIdAndToStatusAndIsDeletedFalseOrderByCreatedAtDesc(10L, OrderStatus.CANCELLED))
                .thenReturn(Optional.of(cancelHistory));
        when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> invocation.getArgument(0));

        OrderDetailResponse response = customerOrderService.undoCancelOrder(1L, 10L);

        assertThat(response).isNotNull();
        assertThat(sampleOrder.getStatus()).isEqualTo(OrderStatus.CONFIRMED);
        assertThat(sampleOrder.getConfirmedAt()).isEqualTo(originalConfirmedAt);
        assertThat(sampleOrder.getCancelledBy()).isNull();
        assertThat(sampleOrder.getCancelledAt()).isNull();
    }

    @Test
    @DisplayName("Hoàn tác hủy đơn thất bại: Đơn hàng không ở trạng thái CANCELLED")
    void undoCancelOrder_fails_whenNotCancelled() {
        sampleOrder.setStatus(OrderStatus.PENDING);

        when(orderRepository.findByIdAndCustomerIdAndIsDeletedFalse(10L, 1L)).thenReturn(Optional.of(sampleOrder));
        when(userRepository.findById(1L)).thenReturn(Optional.of(sampleCustomer));

        assertThatThrownBy(() -> customerOrderService.undoCancelOrder(1L, 10L))
                .isInstanceOf(InvalidOrderStateException.class)
                .hasMessageContaining("CANCELLED");
    }

    @Test
    @DisplayName("Hoàn tác hủy đơn thất bại: Không tìm thấy đơn hàng")
    void undoCancelOrder_fails_whenOrderNotFound() {
        when(orderRepository.findByIdAndCustomerIdAndIsDeletedFalse(99L, 1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> customerOrderService.undoCancelOrder(1L, 99L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("99");
    }

    @Test
    @DisplayName("Hoàn tác hủy đơn thất bại: Đơn hàng do Quản lý hủy thì Khách hàng không thể hoàn tác")
    void undoCancelOrder_fails_whenCancelledByManager() {
        User managerUser = User.builder().id(999L).fullName("Quản lý").build();
        sampleOrder.setStatus(OrderStatus.CANCELLED);
        sampleOrder.setCancelledBy(managerUser);
        sampleOrder.setCancelledAt(Instant.now().minus(5, ChronoUnit.MINUTES));

        when(orderRepository.findByIdAndCustomerIdAndIsDeletedFalse(10L, 1L)).thenReturn(Optional.of(sampleOrder));
        when(userRepository.findById(1L)).thenReturn(Optional.of(sampleCustomer));

        assertThatThrownBy(() -> customerOrderService.undoCancelOrder(1L, 10L))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("không phải do bạn hủy");
    }

    @Test
    @DisplayName("Hoàn tác hủy đơn thất bại: Quá thời hạn hoàn tác 60 phút")
    void undoCancelOrder_fails_whenWindowExpired() {
        sampleOrder.setStatus(OrderStatus.CANCELLED);
        sampleOrder.setCancelledBy(sampleCustomer);
        sampleOrder.setCancelledAt(Instant.now().minus(65, ChronoUnit.MINUTES));

        when(orderRepository.findByIdAndCustomerIdAndIsDeletedFalse(10L, 1L)).thenReturn(Optional.of(sampleOrder));
        when(userRepository.findById(1L)).thenReturn(Optional.of(sampleCustomer));
        when(systemSettingService.getUndoCancelWindowMinutes()).thenReturn(60);

        assertThatThrownBy(() -> customerOrderService.undoCancelOrder(1L, 10L))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("quá thời hạn 60 phút");
    }

    @Test
    @DisplayName("Hoàn tác hủy đơn thất bại: Không tìm thấy lịch sử hủy của đơn hàng")
    void undoCancelOrder_fails_whenNoCancelHistory() {
        sampleOrder.setStatus(OrderStatus.CANCELLED);
        sampleOrder.setCancelledBy(sampleCustomer);
        sampleOrder.setCancelledAt(Instant.now().minus(10, ChronoUnit.MINUTES));

        when(orderRepository.findByIdAndCustomerIdAndIsDeletedFalse(10L, 1L)).thenReturn(Optional.of(sampleOrder));
        when(userRepository.findById(1L)).thenReturn(Optional.of(sampleCustomer));
        when(systemSettingService.getUndoCancelWindowMinutes()).thenReturn(60);
        when(orderStatusHistoryRepository.findTopByOrderIdAndToStatusAndIsDeletedFalseOrderByCreatedAtDesc(10L, OrderStatus.CANCELLED))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> customerOrderService.undoCancelOrder(1L, 10L))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("Không tìm thấy lịch sử hủy");
    }

    @Test
    @DisplayName("Hoàn tác hủy đơn thất bại: Trạng thái trước khi hủy không hợp lệ cho Khách hàng (ví dụ PROCESSING)")
    void undoCancelOrder_fails_whenPreviousStatusInvalidForCustomer() {
        sampleOrder.setStatus(OrderStatus.CANCELLED);
        sampleOrder.setCancelledBy(sampleCustomer);
        sampleOrder.setCancelledAt(Instant.now().minus(10, ChronoUnit.MINUTES));

        OrderStatusHistory cancelHistory = OrderStatusHistory.builder()
                .id(3L)
                .order(sampleOrder)
                .fromStatus(OrderStatus.PROCESSING)
                .toStatus(OrderStatus.CANCELLED)
                .changedBy(sampleCustomer)
                .build();

        when(orderRepository.findByIdAndCustomerIdAndIsDeletedFalse(10L, 1L)).thenReturn(Optional.of(sampleOrder));
        when(userRepository.findById(1L)).thenReturn(Optional.of(sampleCustomer));
        when(systemSettingService.getUndoCancelWindowMinutes()).thenReturn(60);
        when(orderStatusHistoryRepository.findTopByOrderIdAndToStatusAndIsDeletedFalseOrderByCreatedAtDesc(10L, OrderStatus.CANCELLED))
                .thenReturn(Optional.of(cancelHistory));

        assertThatThrownBy(() -> customerOrderService.undoCancelOrder(1L, 10L))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("PENDING hoặc CONFIRMED");
    }
}

