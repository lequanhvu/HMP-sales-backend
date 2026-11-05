package com.hmp.sales.feature.order.service.impl;

import com.hmp.sales.common.exception.BusinessRuleException;
import com.hmp.sales.common.exception.InvalidOrderStateException;
import com.hmp.sales.common.exception.OtpVerificationException;
import com.hmp.sales.common.exception.ResourceNotFoundException;
import com.hmp.sales.common.response.PageResponse;
import com.hmp.sales.common.util.DateUtils;
import com.hmp.sales.feature.order.dto.CancelOrderRequest;
import com.hmp.sales.feature.order.dto.CreateOrderRequest;
import com.hmp.sales.feature.order.dto.OrderDetailResponse;
import com.hmp.sales.feature.order.dto.OrderFilterRequest;
import com.hmp.sales.feature.order.dto.OrderItemRequest;
import com.hmp.sales.feature.order.dto.OrderItemResponse;
import com.hmp.sales.feature.order.dto.OrderStatusHistoryResponse;
import com.hmp.sales.feature.order.dto.OrderSummaryResponse;
import com.hmp.sales.feature.order.entity.Order;
import com.hmp.sales.feature.order.entity.OrderItem;
import com.hmp.sales.feature.order.entity.OrderStatus;
import com.hmp.sales.feature.order.entity.OrderStatusHistory;
import com.hmp.sales.feature.order.repository.OrderItemRepository;
import com.hmp.sales.feature.order.repository.OrderRepository;
import com.hmp.sales.feature.order.repository.OrderSpecification;
import com.hmp.sales.feature.order.repository.OrderStatusHistoryRepository;
import com.hmp.sales.feature.order.service.CustomerOrderService;
import com.hmp.sales.feature.notification.event.OrderStatusChangedEvent;
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
import com.hmp.sales.feature.user.entity.CustomerAddress;
import com.hmp.sales.feature.user.entity.User;
import com.hmp.sales.feature.user.repository.CustomerAddressRepository;
import com.hmp.sales.feature.user.repository.UserRepository;
import jakarta.persistence.criteria.Predicate;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.LocalTime;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Lớp triển khai các nghiệp vụ xử lý đơn hàng cho Khách hàng (CustomerOrderService).
 */
@Slf4j
@Service
public class CustomerOrderServiceImpl implements CustomerOrderService {

    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    private final OrderStatusHistoryRepository orderStatusHistoryRepository;
    private final UserRepository userRepository;
    private final ProductRepository productRepository;
    private final ProductVariantRepository productVariantRepository;
    private final ColorRepository colorRepository;
    private final UnitRepository unitRepository;
    private final ProductColorRepository productColorRepository;
    private final ProductPriceRepository productPriceRepository;
    private final PhoneOtpService phoneOtpService;
    private final SystemSettingService systemSettingService;
    private final CustomerAddressRepository customerAddressRepository;
    private final com.hmp.sales.security.PermissionEvaluationService permissionEvaluationService;
    private final ApplicationEventPublisher eventPublisher;

    /**
     * Khởi tạo service với các repositories và services phụ thuộc.
     *
     * @param orderRepository              repository quản lý đơn hàng
     * @param orderItemRepository          repository quản lý chi tiết dòng đơn hàng
     * @param orderStatusHistoryRepository repository quản lý lịch sử trạng thái đơn hàng
     * @param userRepository               repository quản lý người dùng
     * @param productRepository            repository quản lý sản phẩm
     * @param productVariantRepository     repository quản lý biến thể sản phẩm
     * @param colorRepository              repository quản lý màu sắc
     * @param unitRepository               repository quản lý đơn vị tính
     * @param productColorRepository       repository quản lý liên kết màu sắc của biến thể
     * @param productPriceRepository       repository quản lý bảng giá theo đơn vị tính của biến thể
     * @param phoneOtpService              service xác thực mã OTP số điện thoại
     * @param systemSettingService         service cấu hình hệ thống động
     * @param customerAddressRepository    repository quản lý sổ địa chỉ khách hàng
     * @param permissionEvaluationService  service đánh giá quyền hạn động của người dùng
     * @param eventPublisher               bộ điều phối phát sinh sự kiện ứng dụng
     */
    public CustomerOrderServiceImpl(
            OrderRepository orderRepository,
            OrderItemRepository orderItemRepository,
            OrderStatusHistoryRepository orderStatusHistoryRepository,
            UserRepository userRepository,
            ProductRepository productRepository,
            ProductVariantRepository productVariantRepository,
            ColorRepository colorRepository,
            UnitRepository unitRepository,
            ProductColorRepository productColorRepository,
            ProductPriceRepository productPriceRepository,
            PhoneOtpService phoneOtpService,
            SystemSettingService systemSettingService,
            CustomerAddressRepository customerAddressRepository,
            com.hmp.sales.security.PermissionEvaluationService permissionEvaluationService,
            ApplicationEventPublisher eventPublisher) {
        this.orderRepository = orderRepository;
        this.orderItemRepository = orderItemRepository;
        this.orderStatusHistoryRepository = orderStatusHistoryRepository;
        this.userRepository = userRepository;
        this.productRepository = productRepository;
        this.productVariantRepository = productVariantRepository;
        this.colorRepository = colorRepository;
        this.unitRepository = unitRepository;
        this.productColorRepository = productColorRepository;
        this.productPriceRepository = productPriceRepository;
        this.phoneOtpService = phoneOtpService;
        this.systemSettingService = systemSettingService;
        this.customerAddressRepository = customerAddressRepository;
        this.permissionEvaluationService = permissionEvaluationService;
        this.eventPublisher = eventPublisher;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @Transactional
    public OrderDetailResponse createOrder(Long customerId, CreateOrderRequest request) {
        User customer = userRepository.findById(customerId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy thông tin khách hàng ID: " + customerId));

        validatePhoneOtp(customer, request.receiverPhone());

        String shippingAddress = resolveShippingAddress(customer.getId(), request.addressId());

        Order order = Order.builder()
                .customer(customer)
                .status(OrderStatus.PENDING)
                .receiverName(request.receiverName().trim())
                .receiverPhone(request.receiverPhone().trim())
                .shippingAddress(shippingAddress)
                .note(request.note() != null ? request.note().trim() : null)
                .discountAmount(0L)
                .build();

        Order savedOrder = orderRepository.save(order);

        boolean canViewPrice = permissionEvaluationService.canViewProductPrice();
        List<OrderItem> savedItems = buildAndSaveOrderItems(savedOrder, request.items(), canViewPrice);

        if (canViewPrice) {
            long subtotal = savedItems.stream()
                    .mapToLong(i -> i.getTotalPrice() != null ? i.getTotalPrice() : 0L)
                    .sum();
            savedOrder.setSubtotalAmount(subtotal);
            savedOrder.setDiscountAmount(0L);
            savedOrder.setTotalAmount(subtotal);
            savedOrder.setCalculatedAt(Instant.now());
            savedOrder = orderRepository.save(savedOrder);
            log.info("[Order] Khách hàng ID={} có quyền xem giá: Hệ thống tự động tính giá đơn hàng ID={}, Tổng tiền={}",
                    customerId, savedOrder.getId(), subtotal);
        }

        OrderStatusHistory history = OrderStatusHistory.builder()
                .order(savedOrder)
                .fromStatus(null)
                .toStatus(OrderStatus.PENDING)
                .changedBy(customer)
                .reason("Khách hàng tạo đơn hàng mới")
                .createdAt(Instant.now())
                .build();
        OrderStatusHistory savedHistory = orderStatusHistoryRepository.save(history);

        log.info("[Order] Khách hàng ID={} tạo thành công đơn hàng mới ID={}", customerId, savedOrder.getId());

        boolean hidePrice = shouldHidePrice(savedOrder);
        List<OrderItemResponse> itemResponses = savedItems.stream()
                .map(i -> OrderItemResponse.fromEntity(i, hidePrice))
                .toList();
        List<OrderStatusHistoryResponse> historyResponses = List.of(OrderStatusHistoryResponse.fromEntity(savedHistory));

        return OrderDetailResponse.fromEntity(savedOrder, itemResponses, historyResponses, hidePrice);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @Transactional(readOnly = true)
    public PageResponse<OrderSummaryResponse> getOwnOrders(Long customerId, OrderFilterRequest filter, Pageable pageable) {
        if (filter != null) {
            DateUtils.validateDateRange(filter.dateFrom(), filter.dateTo());
        }
        Specification<Order> spec = OrderSpecification.forCustomer(customerId, filter);
        Page<Order> orderPage = orderRepository.findAll(spec, pageable);
        return PageResponse.from(orderPage.map(o -> OrderSummaryResponse.fromEntity(o, shouldHidePrice(o))));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @Transactional(readOnly = true)
    public OrderDetailResponse getOwnOrderDetail(Long customerId, Long orderId) {
        Order order = orderRepository.findByIdAndCustomerIdAndIsDeletedFalse(orderId, customerId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy đơn hàng ID: " + orderId));

        List<OrderItem> items = orderItemRepository.findByOrderIdAndIsDeletedFalse(orderId);
        List<OrderStatusHistory> history = orderStatusHistoryRepository.findByOrderIdOrderByCreatedAtAsc(orderId);

        boolean hidePrice = shouldHidePrice(order);
        List<OrderItemResponse> itemResponses = items.stream()
                .map(i -> OrderItemResponse.fromEntity(i, hidePrice))
                .toList();
        List<OrderStatusHistoryResponse> historyResponses = history.stream()
                .map(OrderStatusHistoryResponse::fromEntity)
                .toList();

        return OrderDetailResponse.fromEntity(order, itemResponses, historyResponses, hidePrice);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @Transactional
    public OrderDetailResponse cancelOwnOrder(Long customerId, Long orderId, CancelOrderRequest request) {
        Order order = orderRepository.findByIdAndCustomerIdAndIsDeletedFalse(orderId, customerId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy đơn hàng ID: " + orderId));

        User customer = userRepository.findById(customerId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy thông tin khách hàng ID: " + customerId));

        validateOrderCancellable(order);

        OrderStatus previousStatus = order.getStatus();
        String reason = (request != null && request.reason() != null && !request.reason().isBlank())
                ? request.reason().trim()
                : null;

        order.setStatus(OrderStatus.CANCELLED);
        order.setCancellationReason(reason);
        order.setCancelledBy(customer);
        order.setCancelledAt(Instant.now());
        Order updatedOrder = orderRepository.save(order);

        OrderStatusHistory history = OrderStatusHistory.builder()
                .order(updatedOrder)
                .fromStatus(previousStatus)
                .toStatus(OrderStatus.CANCELLED)
                .changedBy(customer)
                .reason(reason != null ? reason : "Khách hàng hủy đơn")
                .createdAt(Instant.now())
                .build();
        orderStatusHistoryRepository.save(history);

        eventPublisher.publishEvent(new OrderStatusChangedEvent(
                updatedOrder.getId(),
                customerId,
                previousStatus,
                OrderStatus.CANCELLED,
                customerId
        ));

        log.info("[Order] Khách hàng ID={} đã hủy đơn hàng ID={}", customerId, orderId);

        List<OrderItem> items = orderItemRepository.findByOrderIdAndIsDeletedFalse(orderId);
        List<OrderStatusHistory> histories = orderStatusHistoryRepository.findByOrderIdOrderByCreatedAtAsc(orderId);

        boolean hidePrice = shouldHidePrice(updatedOrder);
        List<OrderItemResponse> itemResponses = items.stream()
                .map(i -> OrderItemResponse.fromEntity(i, hidePrice))
                .toList();
        List<OrderStatusHistoryResponse> historyResponses = histories.stream()
                .map(OrderStatusHistoryResponse::fromEntity)
                .toList();

        return OrderDetailResponse.fromEntity(
                updatedOrder,
                itemResponses,
                historyResponses,
                hidePrice
        );
    }

    private void validatePhoneOtp(User customer, String phone) {
        boolean isPhoneMatched = customer.getPhone() != null && customer.getPhone().equals(phone);
        boolean isOtpVerified = phoneOtpService.isVerified(phone);

        if (!isPhoneMatched && !isOtpVerified) {
            throw new OtpVerificationException("Số điện thoại nhận hàng chưa được xác thực mã OTP. Vui lòng xác thực trước khi đặt hàng.");
        }
    }

    private String resolveShippingAddress(Long customerId, Long addressId) {
        if (addressId == null) {
            throw new BusinessRuleException("Địa chỉ nhận hàng không được để trống.");
        }

        CustomerAddress savedAddress = customerAddressRepository
                .findByIdAndCustomerIdAndIsDeletedFalse(addressId, customerId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy địa chỉ nhận hàng ID: " + addressId));

        if (!Boolean.TRUE.equals(savedAddress.getIsDefault())) {
            customerAddressRepository.resetDefaultAddress(customerId);
            savedAddress.setIsDefault(true);
            customerAddressRepository.save(savedAddress);
            log.info("[Order] Đã tự động cập nhật địa chỉ ID: {} thành địa chỉ mặc định cho khách hàng ID: {}", savedAddress.getId(), customerId);
        }

        return savedAddress.getAddressLine().trim();
    }

    private List<OrderItem> buildAndSaveOrderItems(Order order, List<OrderItemRequest> itemRequests, boolean canViewPrice) {
        List<OrderItem> itemsToSave = new ArrayList<>();

        for (OrderItemRequest req : itemRequests) {
            Product product = productRepository.findById(req.productId())
                    .filter(p -> !Boolean.TRUE.equals(p.getIsDeleted()))
                    .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy sản phẩm ID: " + req.productId()));

            if (product.getStatus() != ProductStatus.ACTIVE) {
                throw new BusinessRuleException("Sản phẩm '" + product.getProductName() + "' hiện đang tạm ngừng kinh doanh, không thể đặt hàng.");
            }

            ProductVariant variant = null;
            if (req.variantId() != null) {
                variant = productVariantRepository.findById(req.variantId())
                        .filter(v -> !Boolean.TRUE.equals(v.getIsDeleted()))
                        .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy biến thể sản phẩm ID: " + req.variantId()));

                if (!variant.getProduct().getId().equals(product.getId())) {
                    throw new BusinessRuleException("Biến thể ID " + req.variantId() + " không thuộc sản phẩm '" + product.getProductName() + "'.");
                }

                if (variant.getStatus() != VariantStatus.ACTIVE) {
                    throw new BusinessRuleException("Biến thể sản phẩm '" + variant.getVariantCode() + "' hiện không hoạt động.");
                }
            }

            Color color = null;
            if (req.colorId() != null) {
                color = colorRepository.findByIdAndIsDeletedFalse(req.colorId())
                        .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy màu sắc ID: " + req.colorId()));

                if (color.getStatus() != ColorStatus.ACTIVE) {
                    throw new BusinessRuleException("Màu sắc '" + color.getColorName() + "' hiện đang bị khóa.");
                }

                if (variant == null) {
                    throw new BusinessRuleException("Sản phẩm '" + product.getProductName() + "' chưa chọn biến thể, không thể chọn màu sắc.");
                }

                if (!productColorRepository.existsByVariantIdAndColorId(variant.getId(), color.getId())) {
                    throw new BusinessRuleException("Màu sắc '" + color.getColorName() + "' không thuộc biến thể ID " + variant.getId() + " của sản phẩm '" + product.getProductName() + "'.");
                }
            }

            Unit unit = null;
            if (req.unitId() != null) {
                unit = unitRepository.findByIdAndIsDeletedFalse(req.unitId())
                        .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy đơn vị tính ID: " + req.unitId()));

                if (unit.getStatus() != UnitStatus.ACTIVE) {
                    throw new BusinessRuleException("Đơn vị tính '" + unit.getUnitName() + "' hiện đang bị khóa.");
                }

                if (variant == null) {
                    throw new BusinessRuleException("Sản phẩm '" + product.getProductName() + "' chưa chọn biến thể, không thể chọn đơn vị tính.");
                }

                if (!productPriceRepository.existsByVariantIdAndUnitId(variant.getId(), unit.getId())) {
                    throw new BusinessRuleException("Đơn vị tính '" + unit.getUnitName() + "' không thuộc biến thể ID " + variant.getId() + " của sản phẩm '" + product.getProductName() + "'.");
                }
            }

            Integer unitPrice = null;
            Long itemDiscount = 0L;
            Long totalPrice = null;

            if (canViewPrice && variant != null && unit != null) {
                Optional<ProductPrice> ppOpt = productPriceRepository.findByVariantIdAndUnitId(variant.getId(), unit.getId());
                if (ppOpt.isPresent()) {
                    unitPrice = ppOpt.get().getPrice();
                    BigDecimal sizeMultiplier = req.size() != null ? req.size() : BigDecimal.ONE;
                    BigDecimal totalUnits = sizeMultiplier.multiply(req.quantity());
                    BigDecimal grossAmount = BigDecimal.valueOf(unitPrice).multiply(totalUnits);
                    totalPrice = grossAmount.setScale(0, java.math.RoundingMode.HALF_UP).longValue();
                }
            }

            OrderItem item = OrderItem.builder()
                    .order(order)
                    .product(product)
                    .variant(variant)
                    .unit(unit)
                    .productNameSnapshot(product.getProductName())
                    .variantNameSnapshot(variant != null ? variant.getVariantName() : null)
                    .colorSnapshot(color != null ? color.getColorName() : null)
                    .unitNameSnapshot(unit != null ? unit.getUnitName() : null)
                    .quantity(req.quantity())
                    .size(req.size())
                    .unitPrice(unitPrice)
                    .discountAmount(itemDiscount)
                    .totalPrice(totalPrice)
                    .build();

            itemsToSave.add(item);
        }

        return orderItemRepository.saveAll(itemsToSave);
    }

    private boolean shouldHidePrice(Order order) {
        boolean canViewPrice = permissionEvaluationService.canViewProductPrice();
        if (canViewPrice) {
            return false;
        }
        return order.getStatus() == OrderStatus.PENDING;
    }

    private void validateOrderCancellable(Order order) {
        if (order.getStatus() != OrderStatus.PENDING && order.getStatus() != OrderStatus.CONFIRMED) {
            throw new InvalidOrderStateException("Không thể hủy đơn hàng đang ở trạng thái '" + order.getStatus() + "'.");
        }

        int cancellationWindow = systemSettingService.getCancellationWindowMinutes();
        Instant baseTime = order.getConfirmedAt() != null ? order.getConfirmedAt() : order.getCreatedAt();
        Instant deadline = baseTime.plus(cancellationWindow, ChronoUnit.MINUTES);

        // khi trạng thái đơn hàng là PENDING thì khách hàng hoàn toàn có thể hủy đơn
        if (Instant.now().isAfter(deadline) && order.getStatus() == OrderStatus.CONFIRMED) {
            throw new BusinessRuleException("Đã quá thời hạn " + cancellationWindow + " phút cho phép hủy đơn hàng. Vui lòng liên hệ bộ phận hỗ trợ.");
        }
    }
}

