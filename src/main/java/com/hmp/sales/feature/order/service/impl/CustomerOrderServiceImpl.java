package com.hmp.sales.feature.order.service.impl;

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
import com.hmp.sales.feature.order.dto.OrderItemResponse;
import com.hmp.sales.feature.order.dto.OrderStatusHistoryResponse;
import com.hmp.sales.feature.order.dto.OrderSummaryResponse;
import com.hmp.sales.feature.order.entity.Order;
import com.hmp.sales.feature.order.entity.OrderItem;
import com.hmp.sales.feature.order.entity.OrderStatus;
import com.hmp.sales.feature.order.entity.OrderStatusHistory;
import com.hmp.sales.feature.order.repository.OrderItemRepository;
import com.hmp.sales.feature.order.repository.OrderRepository;
import com.hmp.sales.feature.order.repository.OrderStatusHistoryRepository;
import com.hmp.sales.feature.order.service.CustomerOrderService;
import com.hmp.sales.feature.otp.service.PhoneOtpService;
import com.hmp.sales.feature.product.entity.Product;
import com.hmp.sales.feature.product.entity.ProductStatus;
import com.hmp.sales.feature.product.entity.ProductVariant;
import com.hmp.sales.feature.product.entity.VariantStatus;
import com.hmp.sales.feature.product.repository.ProductRepository;
import com.hmp.sales.feature.product.repository.ProductVariantRepository;
import com.hmp.sales.feature.setting.service.SystemSettingService;
import com.hmp.sales.feature.user.entity.User;
import com.hmp.sales.feature.user.repository.UserRepository;
import jakarta.persistence.criteria.Predicate;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalTime;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;

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
    private final PhoneOtpService phoneOtpService;
    private final SystemSettingService systemSettingService;

    /**
     * Khởi tạo service với các repositories và services phụ thuộc.
     */
    public CustomerOrderServiceImpl(
            OrderRepository orderRepository,
            OrderItemRepository orderItemRepository,
            OrderStatusHistoryRepository orderStatusHistoryRepository,
            UserRepository userRepository,
            ProductRepository productRepository,
            ProductVariantRepository productVariantRepository,
            PhoneOtpService phoneOtpService,
            SystemSettingService systemSettingService) {
        this.orderRepository = orderRepository;
        this.orderItemRepository = orderItemRepository;
        this.orderStatusHistoryRepository = orderStatusHistoryRepository;
        this.userRepository = userRepository;
        this.productRepository = productRepository;
        this.productVariantRepository = productVariantRepository;
        this.phoneOtpService = phoneOtpService;
        this.systemSettingService = systemSettingService;
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

        Order order = Order.builder()
                .customer(customer)
                .status(OrderStatus.PENDING)
                .receiverName(request.receiverName().trim())
                .receiverPhone(request.receiverPhone().trim())
                .shippingAddress(request.shippingAddress().trim())
                .note(request.note() != null ? request.note().trim() : null)
                .build();

        Order savedOrder = orderRepository.save(order);

        List<OrderItem> savedItems = buildAndSaveOrderItems(savedOrder, request.items());

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

        List<OrderItemResponse> itemResponses = savedItems.stream()
                .map(OrderItemResponse::fromEntity)
                .toList();
        List<OrderStatusHistoryResponse> historyResponses = List.of(OrderStatusHistoryResponse.fromEntity(savedHistory));

        return OrderDetailResponse.fromEntity(savedOrder, itemResponses, historyResponses);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @Transactional(readOnly = true)
    public PageResponse<OrderSummaryResponse> getOwnOrders(Long customerId, OrderFilterRequest filter, Pageable pageable) {
        Specification<Order> spec = buildOrderSpecification(customerId, filter);
        Page<Order> orderPage = orderRepository.findAll(spec, pageable);
        return PageResponse.from(orderPage.map(OrderSummaryResponse::fromEntity));
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

        List<OrderItemResponse> itemResponses = items.stream()
                .map(OrderItemResponse::fromEntity)
                .toList();
        List<OrderStatusHistoryResponse> historyResponses = history.stream()
                .map(OrderStatusHistoryResponse::fromEntity)
                .toList();

        return OrderDetailResponse.fromEntity(order, itemResponses, historyResponses);
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

        log.info("[Order] Khách hàng ID={} đã hủy đơn hàng ID={}", customerId, orderId);

        List<OrderItem> items = orderItemRepository.findByOrderIdAndIsDeletedFalse(orderId);
        List<OrderStatusHistory> histories = orderStatusHistoryRepository.findByOrderIdOrderByCreatedAtAsc(orderId);

        return OrderDetailResponse.fromEntity(
                updatedOrder,
                items.stream().map(OrderItemResponse::fromEntity).toList(),
                histories.stream().map(OrderStatusHistoryResponse::fromEntity).toList()
        );
    }

    private void validatePhoneOtp(User customer, String phone) {
        boolean isPhoneMatched = customer.getPhone() != null && customer.getPhone().equals(phone);
        boolean isOtpVerified = phoneOtpService.isVerified(phone);

        if (!isPhoneMatched && !isOtpVerified) {
            throw new OtpVerificationException("Số điện thoại nhận hàng chưa được xác thực mã OTP. Vui lòng xác thực trước khi đặt hàng.");
        }
    }

    private List<OrderItem> buildAndSaveOrderItems(Order order, List<OrderItemRequest> itemRequests) {
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

            OrderItem item = OrderItem.builder()
                    .order(order)
                    .product(product)
                    .variant(variant)
                    .productNameSnapshot(product.getProductName())
                    .colorSnapshot(req.color() != null ? req.color().trim() : null)
                    .unitNameSnapshot(req.unit() != null ? req.unit().trim() : null)
                    .quantity(req.quantity())
                    .size(req.size())
                    .build();

            itemsToSave.add(item);
        }

        return orderItemRepository.saveAll(itemsToSave);
    }

    private void validateOrderCancellable(Order order) {
        if (order.getStatus() != OrderStatus.PENDING && order.getStatus() != OrderStatus.CONFIRMED) {
            throw new InvalidOrderStateException("Không thể hủy đơn hàng đang ở trạng thái '" + order.getStatus() + "'.");
        }

        int cancellationWindow = systemSettingService.getCancellationWindowMinutes();
        Instant baseTime = order.getConfirmedAt() != null ? order.getConfirmedAt() : order.getCreatedAt();
        Instant deadline = baseTime.plus(cancellationWindow, ChronoUnit.MINUTES);

        if (Instant.now().isAfter(deadline)) {
            throw new BusinessRuleException("Đã quá thời hạn " + cancellationWindow + " phút cho phép hủy đơn hàng. Vui lòng liên hệ bộ phận hỗ trợ.");
        }
    }

    private Specification<Order> buildOrderSpecification(Long customerId, OrderFilterRequest filter) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            predicates.add(cb.equal(root.get("customer").get("id"), customerId));
            predicates.add(cb.isFalse(root.get("isDeleted")));

            if (filter != null) {
                if (filter.status() != null) {
                    predicates.add(cb.equal(root.get("status"), filter.status()));
                }
                if (filter.dateFrom() != null) {
                    Instant fromInstant = filter.dateFrom().atStartOfDay().toInstant(ZoneOffset.UTC);
                    predicates.add(cb.greaterThanOrEqualTo(root.get("createdAt"), fromInstant));
                }
                if (filter.dateTo() != null) {
                    Instant toInstant = filter.dateTo().atTime(LocalTime.MAX).toInstant(ZoneOffset.UTC);
                    predicates.add(cb.lessThanOrEqualTo(root.get("createdAt"), toInstant));
                }
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }
}

