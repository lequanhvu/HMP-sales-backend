package com.hmp.sales.feature.order.service;

import com.hmp.sales.common.exception.BusinessRuleException;
import com.hmp.sales.common.exception.InvalidOrderStateException;
import com.hmp.sales.common.exception.ResourceNotFoundException;
import com.hmp.sales.common.response.PageResponse;
import com.hmp.sales.feature.order.dto.CalculateOrderRequest;
import com.hmp.sales.feature.order.dto.CancelOrderRequest;
import com.hmp.sales.feature.order.dto.ManagerOrderFilterRequest;
import com.hmp.sales.feature.order.dto.ManagerOrderSummaryResponse;
import com.hmp.sales.feature.order.dto.OrderDetailResponse;
import com.hmp.sales.feature.order.dto.OrderItemPriceRequest;
import com.hmp.sales.feature.order.dto.UpdateOrderStatusRequest;
import com.hmp.sales.feature.order.entity.Order;
import com.hmp.sales.feature.order.entity.OrderItem;
import com.hmp.sales.feature.order.entity.OrderStatus;
import com.hmp.sales.feature.order.entity.OrderStatusHistory;
import com.hmp.sales.feature.order.repository.OrderItemRepository;
import com.hmp.sales.feature.order.repository.OrderRepository;
import com.hmp.sales.feature.order.repository.OrderStatusHistoryRepository;
import com.hmp.sales.feature.order.service.impl.ManagerOrderServiceImpl;
import com.hmp.sales.feature.product.entity.Product;
import com.hmp.sales.feature.product.entity.ProductPrice;
import com.hmp.sales.feature.product.entity.ProductVariant;
import com.hmp.sales.feature.product.entity.Unit;
import com.hmp.sales.feature.product.repository.ProductPriceRepository;
import com.hmp.sales.feature.user.entity.User;
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
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ManagerOrderServiceImplTest {

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private OrderItemRepository orderItemRepository;

    @Mock
    private OrderStatusHistoryRepository orderStatusHistoryRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private ProductPriceRepository productPriceRepository;

    @InjectMocks
    private ManagerOrderServiceImpl managerOrderService;

    private User manager;
    private User customer;
    private Order order;
    private OrderItem item;
    private Product product;
    private ProductVariant variant;
    private Unit unit;

    @BeforeEach
    void setUp() {
        manager = User.builder()
                .id(2L)
                .fullName("Quản lý A")
                .email("manager@example.com")
                .phone("0987654321")
                .build();

        customer = User.builder()
                .id(1L)
                .fullName("Nguyễn Văn Khách")
                .email("customer@example.com")
                .phone("0123456789")
                .build();

        order = Order.builder()
                .id(100L)
                .customer(customer)
                .status(OrderStatus.PENDING)
                .receiverName("Nguyễn Văn Khách")
                .receiverPhone("0123456789")
                .shippingAddress("123 Đường Tôn, Quận 1")
                .createdAt(Instant.now())
                .isDeleted(false)
                .items(new ArrayList<>())
                .build();

        product = Product.builder()
                .id(10L)
                .productName("Tôn sóng Hoa Sen")
                .build();

        variant = ProductVariant.builder()
                .id(20L)
                .variantName("Tôn 0.40mm")
                .build();

        unit = Unit.builder()
                .id(30L)
                .unitName("m")
                .build();

        item = OrderItem.builder()
                .id(1L)
                .order(order)
                .product(product)
                .variant(variant)
                .unit(unit)
                .productNameSnapshot("Tôn sóng Hoa Sen")
                .variantNameSnapshot("Tôn 0.40mm")
                .unitNameSnapshot("m")
                .quantity(new BigDecimal("10")) // 10 tấm
                .size(new BigDecimal("5"))     // 5 mét/tấm
                .discountAmount(0L)
                .build();

        order.getItems().add(item);
    }

    @Test
    @DisplayName("getAllOrders - Phân trang danh sách đơn hàng thành công")
    void getAllOrders_success() {
        Pageable pageable = PageRequest.of(0, 10);
        Page<Order> page = new PageImpl<>(List.of(order), pageable, 1);
        when(orderRepository.findAll(any(Specification.class), eq(pageable))).thenReturn(page);

        ManagerOrderFilterRequest filter = new ManagerOrderFilterRequest(
                LocalDate.of(2026, 1, 1), LocalDate.of(2026, 12, 31),
                "Khách", "0123", "customer@example.com", OrderStatus.PENDING);

        PageResponse<ManagerOrderSummaryResponse> result = managerOrderService.getAllOrders(filter, pageable);

        assertThat(result.result()).hasSize(1);
        assertThat(result.result().get(0).orderId()).isEqualTo(100L);
        assertThat(result.result().get(0).customer().fullName()).isEqualTo("Nguyễn Văn Khách");
        assertThat(result.meta().totalElements()).isEqualTo(1);
    }

    @Test
    @DisplayName("getOrderDetail - Tìm thấy đơn hàng thành công")
    void getOrderDetail_success() {
        when(orderRepository.findByIdAndIsDeletedFalse(100L)).thenReturn(Optional.of(order));
        when(orderItemRepository.findByOrderIdAndIsDeletedFalse(100L)).thenReturn(List.of(item));
        when(orderStatusHistoryRepository.findByOrderIdOrderByCreatedAtAsc(100L)).thenReturn(List.of());

        OrderDetailResponse result = managerOrderService.getOrderDetail(100L);

        assertThat(result.orderId()).isEqualTo(100L);
        assertThat(result.receiverName()).isEqualTo("Nguyễn Văn Khách");
        assertThat(result.items()).hasSize(1);
    }

    @Test
    @DisplayName("getOrderDetail - Không tìm thấy đơn hàng ném ResourceNotFoundException")
    void getOrderDetail_notFound_throwsException() {
        when(orderRepository.findByIdAndIsDeletedFalse(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> managerOrderService.getOrderDetail(999L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("999");
    }

    @Test
    @DisplayName("calculateOrderPrice - Tự động tra cứu giá niêm yết từ ProductPrice và tính thành tiền chính xác")
    void calculateOrderPrice_autoPrice_success() {
        // unitPrice = 100,000 đ/m
        // size = 5m, quantity = 10 tấm -> totalUnits = 50m
        // Gross = 100,000 * 50 = 5,000,000 đ. discountAmount = 0 -> totalPrice = 5,000,000 đ.
        when(orderRepository.findByIdAndIsDeletedFalse(100L)).thenReturn(Optional.of(order));
        when(userRepository.findById(2L)).thenReturn(Optional.of(manager));
        when(orderItemRepository.findByOrderIdAndIsDeletedFalse(100L)).thenReturn(List.of(item));

        ProductPrice pp = ProductPrice.builder().price(100_000).build();
        when(productPriceRepository.findByVariantIdAndUnitId(20L, 30L)).thenReturn(Optional.of(pp));
        when(orderRepository.save(any(Order.class))).thenAnswer(i -> i.getArgument(0));

        CalculateOrderRequest request = new CalculateOrderRequest(null, null, "Không chiết khấu");

        OrderDetailResponse response = managerOrderService.calculateOrderPrice(2L, 100L, request);

        assertThat(item.getUnitPrice()).isEqualTo(100_000);
        assertThat(item.getDiscountAmount()).isEqualTo(0L);
        assertThat(item.getTotalPrice()).isEqualTo(5_000_000L);

        assertThat(order.getSubtotalAmount()).isEqualTo(5_000_000L);
        assertThat(order.getDiscountAmount()).isEqualTo(0L);
        assertThat(order.getTotalAmount()).isEqualTo(5_000_000L);
        assertThat(order.getCalculatedBy()).isEqualTo(manager);
        assertThat(order.getCalculatedAt()).isNotNull();
        // LƯU Ý: Trạng thái KHÔNG đổi, vẫn là PENDING
        assertThat(order.getStatus()).isEqualTo(OrderStatus.PENDING);
    }

    @Test
    @DisplayName("calculateOrderPrice - Giảm giá theo đơn giá tùy chỉnh (override unitPrice)")
    void calculateOrderPrice_overrideUnitPrice_success() {
        // Quản lý giảm đơn giá từ 100,000 đ/m xuống 90,000 đ/m
        // 90,000 * 5m * 10 tấm = 4,500,000 đ
        when(orderRepository.findByIdAndIsDeletedFalse(100L)).thenReturn(Optional.of(order));
        when(userRepository.findById(2L)).thenReturn(Optional.of(manager));
        when(orderItemRepository.findByOrderIdAndIsDeletedFalse(100L)).thenReturn(List.of(item));
        when(orderRepository.save(any(Order.class))).thenAnswer(i -> i.getArgument(0));

        CalculateOrderRequest request = new CalculateOrderRequest(
                0L,
                List.of(new OrderItemPriceRequest(1L, 90_000, 0L)),
                "Giảm đơn giá 10k/m"
        );

        managerOrderService.calculateOrderPrice(2L, 100L, request);

        assertThat(item.getUnitPrice()).isEqualTo(90_000);
        assertThat(item.getDiscountAmount()).isEqualTo(0L);
        assertThat(item.getTotalPrice()).isEqualTo(4_500_000L);
        assertThat(order.getSubtotalAmount()).isEqualTo(4_500_000L);
        assertThat(order.getTotalAmount()).isEqualTo(4_500_000L);
    }

    @Test
    @DisplayName("calculateOrderPrice - Giảm giá theo số tiền trên item và kết hợp giảm giá tổng đơn hàng")
    void calculateOrderPrice_discountAmount_itemAndOrder_success() {
        // Đơn giá = 100,000 đ/m (tự tra cứu)
        // Gross = 100,000 * 50 = 5,000,000 đ
        // Giảm giá item = 200,000 đ -> Item TotalPrice = 4,800,000 đ
        // Giảm giá tổng đơn = 300,000 đ -> Order TotalAmount = 4,500,000 đ
        when(orderRepository.findByIdAndIsDeletedFalse(100L)).thenReturn(Optional.of(order));
        when(userRepository.findById(2L)).thenReturn(Optional.of(manager));
        when(orderItemRepository.findByOrderIdAndIsDeletedFalse(100L)).thenReturn(List.of(item));

        ProductPrice pp = ProductPrice.builder().price(100_000).build();
        when(productPriceRepository.findByVariantIdAndUnitId(20L, 30L)).thenReturn(Optional.of(pp));
        when(orderRepository.save(any(Order.class))).thenAnswer(i -> i.getArgument(0));

        CalculateOrderRequest request = new CalculateOrderRequest(
                300_000L,
                List.of(new OrderItemPriceRequest(1L, null, 200_000L)),
                "Giảm giá item 200k và giảm đơn 300k"
        );

        managerOrderService.calculateOrderPrice(2L, 100L, request);

        assertThat(item.getUnitPrice()).isEqualTo(100_000);
        assertThat(item.getDiscountAmount()).isEqualTo(200_000L);
        assertThat(item.getTotalPrice()).isEqualTo(4_800_000L);

        assertThat(order.getSubtotalAmount()).isEqualTo(4_800_000L);
        assertThat(order.getDiscountAmount()).isEqualTo(300_000L);
        assertThat(order.getTotalAmount()).isEqualTo(4_500_000L);
    }

    @Test
    @DisplayName("calculateOrderPrice - Kết hợp cả hai trên item: vừa giảm đơn giá vừa giảm thêm số tiền")
    void calculateOrderPrice_combineUnitPriceAndDiscountAmount_success() {
        // Đơn vị tính giảm xuống 95,000 đ/m
        // Giảm thêm 50,000 đ trên thành tiền dòng
        // Gross = 95,000 * 50 = 4,750,000 đ
        // TotalPrice = 4,750,000 - 50,000 = 4,700,000 đ
        when(orderRepository.findByIdAndIsDeletedFalse(100L)).thenReturn(Optional.of(order));
        when(userRepository.findById(2L)).thenReturn(Optional.of(manager));
        when(orderItemRepository.findByOrderIdAndIsDeletedFalse(100L)).thenReturn(List.of(item));
        when(orderRepository.save(any(Order.class))).thenAnswer(i -> i.getArgument(0));

        CalculateOrderRequest request = new CalculateOrderRequest(
                0L,
                List.of(new OrderItemPriceRequest(1L, 95_000, 50_000L)),
                "Giảm đơn giá xuống 95k và bớt thêm 50k"
        );

        managerOrderService.calculateOrderPrice(2L, 100L, request);

        assertThat(item.getUnitPrice()).isEqualTo(95_000);
        assertThat(item.getDiscountAmount()).isEqualTo(50_000L);
        assertThat(item.getTotalPrice()).isEqualTo(4_700_000L);
        assertThat(order.getSubtotalAmount()).isEqualTo(4_700_000L);
        assertThat(order.getTotalAmount()).isEqualTo(4_700_000L);
    }

    @Test
    @DisplayName("calculateOrderPrice - Đơn hàng đã COMPLETED không thể tính giá")
    void calculateOrderPrice_completedOrder_throwsException() {
        order.setStatus(OrderStatus.COMPLETED);
        when(orderRepository.findByIdAndIsDeletedFalse(100L)).thenReturn(Optional.of(order));

        assertThatThrownBy(() -> managerOrderService.calculateOrderPrice(2L, 100L, null))
                .isInstanceOf(InvalidOrderStateException.class)
                .hasMessageContaining("COMPLETED");
    }

    @Test
    @DisplayName("confirmOrder - Xác nhận đơn hàng thành công khi đã tính giá")
    void confirmOrder_success() {
        order.setTotalAmount(5_000_000L);
        order.setCalculatedAt(Instant.now());

        when(orderRepository.findByIdAndIsDeletedFalse(100L)).thenReturn(Optional.of(order));
        when(userRepository.findById(2L)).thenReturn(Optional.of(manager));
        when(orderRepository.save(any(Order.class))).thenAnswer(i -> i.getArgument(0));

        OrderDetailResponse response = managerOrderService.confirmOrder(2L, 100L);

        assertThat(order.getStatus()).isEqualTo(OrderStatus.CONFIRMED);
        assertThat(order.getConfirmedAt()).isNotNull();
        verify(orderStatusHistoryRepository).save(any(OrderStatusHistory.class));
    }

    @Test
    @DisplayName("confirmOrder - Đơn hàng chưa tính giá ném BusinessRuleException")
    void confirmOrder_notCalculated_throwsException() {
        order.setTotalAmount(null);
        order.setCalculatedAt(null);

        when(orderRepository.findByIdAndIsDeletedFalse(100L)).thenReturn(Optional.of(order));

        assertThatThrownBy(() -> managerOrderService.confirmOrder(2L, 100L))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("chưa được tính giá");
    }

    @Test
    @DisplayName("confirmOrder - Đơn hàng không ở trạng thái PENDING ném InvalidOrderStateException")
    void confirmOrder_invalidState_throwsException() {
        order.setStatus(OrderStatus.CONFIRMED);

        when(orderRepository.findByIdAndIsDeletedFalse(100L)).thenReturn(Optional.of(order));

        assertThatThrownBy(() -> managerOrderService.confirmOrder(2L, 100L))
                .isInstanceOf(InvalidOrderStateException.class)
                .hasMessageContaining("PENDING");
    }

    @Test
    @DisplayName("updateOrderStatus - Chuyển đổi trạng thái tuần tự CONFIRMED -> PROCESSING -> SHIPPING -> COMPLETED")
    void updateOrderStatus_transitions_success() {
        order.setStatus(OrderStatus.CONFIRMED);
        when(orderRepository.findByIdAndIsDeletedFalse(100L)).thenReturn(Optional.of(order));
        when(userRepository.findById(2L)).thenReturn(Optional.of(manager));
        when(orderRepository.save(any(Order.class))).thenAnswer(i -> i.getArgument(0));

        // 1. CONFIRMED -> PROCESSING
        managerOrderService.updateOrderStatus(2L, 100L, new UpdateOrderStatusRequest(OrderStatus.PROCESSING, "Bắt đầu gia công"));
        assertThat(order.getStatus()).isEqualTo(OrderStatus.PROCESSING);

        // 2. PROCESSING -> SHIPPING
        managerOrderService.updateOrderStatus(2L, 100L, new UpdateOrderStatusRequest(OrderStatus.SHIPPING, "Bắt đầu giao"));
        assertThat(order.getStatus()).isEqualTo(OrderStatus.SHIPPING);

        // 3. SHIPPING -> COMPLETED
        managerOrderService.updateOrderStatus(2L, 100L, new UpdateOrderStatusRequest(OrderStatus.COMPLETED, "Giao thành công"));
        assertThat(order.getStatus()).isEqualTo(OrderStatus.COMPLETED);
        assertThat(order.getCompletedAt()).isNotNull();
    }

    @Test
    @DisplayName("updateOrderStatus - Chuyển đổi trạng thái không hợp lệ ném InvalidOrderStateException")
    void updateOrderStatus_invalidTransition_throwsException() {
        order.setStatus(OrderStatus.CONFIRMED);
        when(orderRepository.findByIdAndIsDeletedFalse(100L)).thenReturn(Optional.of(order));
        when(userRepository.findById(2L)).thenReturn(Optional.of(manager));

        // CONFIRMED không thể nhảy cóc sang COMPLETED
        UpdateOrderStatusRequest request = new UpdateOrderStatusRequest(OrderStatus.COMPLETED, "Giao luôn");

        assertThatThrownBy(() -> managerOrderService.updateOrderStatus(2L, 100L, request))
                .isInstanceOf(InvalidOrderStateException.class)
                .hasMessageContaining("Không thể chuyển trạng thái");
    }

    @Test
    @DisplayName("cancelOrder - Quản lý hủy đơn hàng ở trạng thái SHIPPING thành công")
    void cancelOrder_atShippingState_success() {
        order.setStatus(OrderStatus.SHIPPING);
        when(orderRepository.findByIdAndIsDeletedFalse(100L)).thenReturn(Optional.of(order));
        when(userRepository.findById(2L)).thenReturn(Optional.of(manager));
        when(orderRepository.save(any(Order.class))).thenAnswer(i -> i.getArgument(0));

        CancelOrderRequest request = new CancelOrderRequest("Khách từ chối nhận hàng lúc giao");

        OrderDetailResponse response = managerOrderService.cancelOrder(2L, 100L, request);

        assertThat(order.getStatus()).isEqualTo(OrderStatus.CANCELLED);
        assertThat(order.getCancelledBy()).isEqualTo(manager);
        assertThat(order.getCancelledAt()).isNotNull();
        assertThat(order.getCancellationReason()).isEqualTo("Khách từ chối nhận hàng lúc giao");
        verify(orderStatusHistoryRepository).save(any(OrderStatusHistory.class));
    }

    @Test
    @DisplayName("cancelOrder - Quản lý hủy đơn hàng ở trạng thái PENDING thành công không cần lý do")
    void cancelOrder_atPendingState_withoutReason_success() {
        order.setStatus(OrderStatus.PENDING);
        when(orderRepository.findByIdAndIsDeletedFalse(100L)).thenReturn(Optional.of(order));
        when(userRepository.findById(2L)).thenReturn(Optional.of(manager));
        when(orderRepository.save(any(Order.class))).thenAnswer(i -> i.getArgument(0));

        OrderDetailResponse response = managerOrderService.cancelOrder(2L, 100L, null);

        assertThat(order.getStatus()).isEqualTo(OrderStatus.CANCELLED);
        assertThat(order.getCancelledBy()).isEqualTo(manager);
        assertThat(order.getCancelledAt()).isNotNull();
        assertThat(order.getCancellationReason()).isNull();
        verify(orderStatusHistoryRepository).save(any(OrderStatusHistory.class));
    }

    @Test
    @DisplayName("cancelOrder - Đơn hàng đã COMPLETED không thể hủy")
    void cancelOrder_completedOrder_throwsException() {
        order.setStatus(OrderStatus.COMPLETED);
        when(orderRepository.findByIdAndIsDeletedFalse(100L)).thenReturn(Optional.of(order));

        assertThatThrownBy(() -> managerOrderService.cancelOrder(2L, 100L, null))
                .isInstanceOf(InvalidOrderStateException.class)
                .hasMessageContaining("COMPLETED");
    }
}
