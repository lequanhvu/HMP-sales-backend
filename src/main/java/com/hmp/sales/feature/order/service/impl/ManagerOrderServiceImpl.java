package com.hmp.sales.feature.order.service.impl;

import com.hmp.sales.common.exception.BusinessRuleException;
import com.hmp.sales.common.exception.InvalidOrderStateException;
import com.hmp.sales.common.exception.ResourceNotFoundException;
import com.hmp.sales.common.response.PageResponse;
import com.hmp.sales.common.util.DateUtils;
import com.hmp.sales.feature.order.dto.CalculateOrderRequest;
import com.hmp.sales.feature.order.dto.CancelOrderRequest;
import com.hmp.sales.feature.order.dto.ManagerOrderFilterRequest;
import com.hmp.sales.feature.order.dto.ManagerOrderSummaryResponse;
import com.hmp.sales.feature.order.dto.OrderDetailResponse;
import com.hmp.sales.feature.order.dto.OrderItemPriceRequest;
import com.hmp.sales.feature.order.dto.OrderItemResponse;
import com.hmp.sales.feature.order.dto.OrderStatusHistoryResponse;
import com.hmp.sales.feature.order.dto.UpdateOrderStatusRequest;
import com.hmp.sales.feature.order.entity.Order;
import com.hmp.sales.feature.order.entity.OrderItem;
import com.hmp.sales.feature.order.entity.OrderStatus;
import com.hmp.sales.feature.order.entity.OrderStatusHistory;
import com.hmp.sales.feature.order.repository.OrderItemRepository;
import com.hmp.sales.feature.order.repository.OrderRepository;
import com.hmp.sales.feature.order.repository.OrderSpecification;
import com.hmp.sales.feature.order.repository.OrderStatusHistoryRepository;
import com.hmp.sales.feature.order.service.ManagerOrderService;
import com.hmp.sales.feature.product.entity.ProductPrice;
import com.hmp.sales.feature.product.repository.ProductPriceRepository;
import com.hmp.sales.feature.user.entity.User;
import com.hmp.sales.feature.user.repository.UserRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Triển khai các nghiệp vụ xử lý, tính giá và chuyển trạng thái đơn hàng dành cho Quản lý / Admin.
 */
@Service
@Slf4j
public class ManagerOrderServiceImpl implements ManagerOrderService {

    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    private final OrderStatusHistoryRepository orderStatusHistoryRepository;
    private final UserRepository userRepository;
    private final ProductPriceRepository productPriceRepository;

    /**
     * Khởi tạo ManagerOrderServiceImpl với các phụ thuộc cần thiết.
     *
     * @param orderRepository              repository quản lý thực thể Order
     * @param orderItemRepository          repository quản lý thực thể OrderItem
     * @param orderStatusHistoryRepository repository quản lý thực thể OrderStatusHistory
     * @param userRepository               repository quản lý thực thể User
     * @param productPriceRepository       repository tra cứu bảng giá sản phẩm
     */
    public ManagerOrderServiceImpl(
            OrderRepository orderRepository,
            OrderItemRepository orderItemRepository,
            OrderStatusHistoryRepository orderStatusHistoryRepository,
            UserRepository userRepository,
            ProductPriceRepository productPriceRepository) {
        this.orderRepository = orderRepository;
        this.orderItemRepository = orderItemRepository;
        this.orderStatusHistoryRepository = orderStatusHistoryRepository;
        this.userRepository = userRepository;
        this.productPriceRepository = productPriceRepository;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @Transactional(readOnly = true)
    public PageResponse<ManagerOrderSummaryResponse> getAllOrders(ManagerOrderFilterRequest filter, Pageable pageable) {
        if (filter != null) {
            DateUtils.validateDateRange(filter.dateFrom(), filter.dateTo());
        }

        Specification<Order> spec = OrderSpecification.forManager(filter);
        Page<Order> orderPage = orderRepository.findAll(spec, pageable);

        return PageResponse.from(orderPage.map(ManagerOrderSummaryResponse::fromEntity));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @Transactional(readOnly = true)
    public OrderDetailResponse getOrderDetail(Long orderId) {
        Order order = orderRepository.findByIdAndIsDeletedFalse(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy đơn hàng ID: " + orderId));

        return buildOrderDetailResponse(order);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @Transactional
    public OrderDetailResponse calculateOrderPrice(Long managerId, Long orderId, CalculateOrderRequest request) {
        Order order = orderRepository.findByIdAndIsDeletedFalse(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy đơn hàng ID: " + orderId));

        if (order.getStatus() == OrderStatus.COMPLETED || order.getStatus() == OrderStatus.CANCELLED) {
            throw new InvalidOrderStateException("Không thể tính giá đơn hàng đang ở trạng thái '" + order.getStatus() + "'.");
        }

        User manager = userRepository.findById(managerId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy thông tin quản lý ID: " + managerId));

        List<OrderItem> items = orderItemRepository.findByOrderIdAndIsDeletedFalse(orderId);
        if (items.isEmpty()) {
            throw new BusinessRuleException("Đơn hàng không có sản phẩm nào để tính giá.");
        }

        // Tạo map ghi đè đơn giá và giảm giá theo orderItemId (nếu request truyền vào)
        Map<Long, OrderItemPriceRequest> priceOverrides = Collections.emptyMap();
        if (request != null && request.items() != null && !request.items().isEmpty()) {
            if (request.items().size() != items.size()) {
                throw new BusinessRuleException(
                        "Số lượng dòng sản phẩm trong yêu cầu tính giá (" + request.items().size()
                                + ") không khớp với số lượng dòng sản phẩm trong đơn hàng (" + items.size() + ").");
            }

            java.util.Set<Long> existingItemIds = items.stream().map(OrderItem::getId).collect(Collectors.toSet());
            java.util.Set<Long> requestItemIds = new java.util.HashSet<>();

            for (OrderItemPriceRequest reqItem : request.items()) {
                if (!existingItemIds.contains(reqItem.orderItemId())) {
                    throw new BusinessRuleException(
                            "Dòng sản phẩm ID: " + reqItem.orderItemId() + " không thuộc đơn hàng ID: " + orderId);
                }
                if (!requestItemIds.add(reqItem.orderItemId())) {
                    throw new BusinessRuleException(
                            "Dòng sản phẩm ID: " + reqItem.orderItemId() + " bị lặp lại trong yêu cầu tính giá.");
                }
            }

            priceOverrides = request.items().stream()
                    .collect(Collectors.toMap(OrderItemPriceRequest::orderItemId, Function.identity(), (first, second) -> second));
        }

        // Tính giá cho từng dòng sản phẩm
        for (OrderItem item : items) {
            OrderItemPriceRequest override = priceOverrides.get(item.getId());

            // 1. Xác định đơn giá áp dụng (Applied Unit Price)
            Integer appliedUnitPrice;
            if (override != null && override.unitPrice() != null) {
                // Quản lý chủ động truyền đơn giá (giảm giá theo đơn giá)
                appliedUnitPrice = override.unitPrice();
            } else if (item.getVariant() != null && item.getUnit() != null) {
                // Tự động tra cứu từ bảng giá niêm yết trong DB
                ProductPrice productPrice = productPriceRepository.findByVariantIdAndUnitId(
                                item.getVariant().getId(), item.getUnit().getId())
                        .orElseThrow(() -> new BusinessRuleException(
                                "Không tìm thấy bảng giá niêm yết cho dòng sản phẩm '" + item.getProductNameSnapshot() + "'. Vui lòng nhập đơn giá thủ công."));
                appliedUnitPrice = productPrice.getPrice();
            } else {
                throw new BusinessRuleException(
                        "Dòng sản phẩm '" + item.getProductNameSnapshot() + "' chưa có bảng giá niêm yết. Vui lòng nhập đơn giá thủ công.");
            }

            // 2. Xác định số tiền giảm giá cho dòng sản phẩm (Item Discount Amount)
            Long itemDiscount = (override != null && override.discountAmount() != null)
                    ? override.discountAmount()
                    : 0L;

            // 3. Tính thành tiền dòng sản phẩm (Gross = UnitPrice * (Size != null ? Size : 1) * Quantity)
            BigDecimal sizeMultiplier = item.getSize() != null ? item.getSize() : BigDecimal.ONE;
            BigDecimal totalUnits = sizeMultiplier.multiply(item.getQuantity());
            BigDecimal grossAmount = BigDecimal.valueOf(appliedUnitPrice).multiply(totalUnits);
            long gross = grossAmount.setScale(0, RoundingMode.HALF_UP).longValue();

            // TotalPrice = max(0, Gross - ItemDiscount)
            long totalPrice = Math.max(0L, gross - itemDiscount);

            item.setUnitPrice(appliedUnitPrice);
            item.setDiscountAmount(itemDiscount);
            item.setTotalPrice(totalPrice);
        }

        orderItemRepository.saveAll(items);

        // Tính tổng tiền hàng (subtotalAmount)
        long subtotalAmount = items.stream().mapToLong(OrderItem::getTotalPrice).sum();

        // Xác định số tiền giảm giá tổng đơn hàng (Order Discount Amount)
        Long orderDiscount = (request != null && request.discountAmount() != null)
                ? request.discountAmount()
                : 0L;

        // Tính tổng tiền thanh toán cuối cùng: TotalAmount = max(0, SubtotalAmount - OrderDiscount)
        long totalAmount = Math.max(0L, subtotalAmount - orderDiscount);

        order.setSubtotalAmount(subtotalAmount);
        order.setDiscountAmount(orderDiscount);
        order.setTotalAmount(totalAmount);
        order.setCalculatedBy(manager);
        order.setCalculatedAt(Instant.now());

        // LƯU Ý: Thao tác tính giá KHÔNG tự động chuyển trạng thái đơn (giữ nguyên trạng thái hiện tại)
        Order savedOrder = orderRepository.save(order);
        log.info("[Order] Quản lý ID={} đã tính giá cho đơn hàng ID={}: Tạm tính={}, Giảm giá đơn={}, Tổng thanh toán={}",
                managerId, orderId, subtotalAmount, orderDiscount, totalAmount);

        return buildOrderDetailResponse(savedOrder);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @Transactional
    public OrderDetailResponse confirmOrder(Long managerId, Long orderId) {
        Order order = orderRepository.findByIdAndIsDeletedFalse(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy đơn hàng ID: " + orderId));

        if (order.getStatus() != OrderStatus.PENDING) {
            throw new InvalidOrderStateException("Chỉ có thể xác nhận đơn hàng đang ở trạng thái 'PENDING'. Trạng thái hiện tại: '" + order.getStatus() + "'.");
        }

        if (order.getTotalAmount() == null || order.getCalculatedAt() == null) {
            throw new BusinessRuleException("Đơn hàng chưa được tính giá, không thể xác nhận.");
        }

        User manager = userRepository.findById(managerId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy thông tin quản lý ID: " + managerId));

        OrderStatus fromStatus = order.getStatus();
        order.setStatus(OrderStatus.CONFIRMED);
        order.setConfirmedAt(Instant.now());

        Order savedOrder = orderRepository.save(order);

        OrderStatusHistory history = OrderStatusHistory.builder()
                .order(savedOrder)
                .fromStatus(fromStatus)
                .toStatus(OrderStatus.CONFIRMED)
                .changedBy(manager)
                .reason("Quản lý xác nhận giá đơn hàng")
                .createdAt(Instant.now())
                .build();
        orderStatusHistoryRepository.save(history);

        log.info("[Order] Quản lý ID={} đã xác nhận đơn hàng ID={}", managerId, orderId);

        return buildOrderDetailResponse(savedOrder);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @Transactional
    public OrderDetailResponse updateOrderStatus(Long managerId, Long orderId, UpdateOrderStatusRequest request) {
        Order order = orderRepository.findByIdAndIsDeletedFalse(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy đơn hàng ID: " + orderId));

        User manager = userRepository.findById(managerId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy thông tin quản lý ID: " + managerId));

        OrderStatus fromStatus = order.getStatus();
        OrderStatus toStatus = request.status();

        validateStatusTransition(order, toStatus);

        order.setStatus(toStatus);
        if (toStatus == OrderStatus.COMPLETED) {
            order.setCompletedAt(Instant.now());
        } else if (toStatus == OrderStatus.CONFIRMED && order.getConfirmedAt() == null) {
            order.setConfirmedAt(Instant.now());
        }

        Order savedOrder = orderRepository.save(order);

        String reason = request.reason() != null && !request.reason().isBlank()
                ? request.reason().trim()
                : "Quản lý cập nhật trạng thái đơn hàng sang " + toStatus;

        OrderStatusHistory history = OrderStatusHistory.builder()
                .order(savedOrder)
                .fromStatus(fromStatus)
                .toStatus(toStatus)
                .changedBy(manager)
                .reason(reason)
                .createdAt(Instant.now())
                .build();
        orderStatusHistoryRepository.save(history);

        log.info("[Order] Quản lý ID={} cập nhật trạng thái đơn hàng ID={} từ {} sang {}", managerId, orderId, fromStatus, toStatus);

        return buildOrderDetailResponse(savedOrder);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @Transactional
    public OrderDetailResponse cancelOrder(Long managerId, Long orderId, CancelOrderRequest request) {
        Order order = orderRepository.findByIdAndIsDeletedFalse(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy đơn hàng ID: " + orderId));

        // Quản lý được phép hủy đơn ở các trạng thái PENDING, CONFIRMED, PROCESSING, và SHIPPING
        if (order.getStatus() != OrderStatus.PENDING
                && order.getStatus() != OrderStatus.CONFIRMED
                && order.getStatus() != OrderStatus.PROCESSING
                && order.getStatus() != OrderStatus.SHIPPING) {
            throw new InvalidOrderStateException("Không thể hủy đơn hàng đang ở trạng thái '" + order.getStatus() + "'.");
        }

        User manager = userRepository.findById(managerId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy thông tin quản lý ID: " + managerId));

        OrderStatus fromStatus = order.getStatus();
        String cancellationReason = (request != null && request.reason() != null && !request.reason().isBlank())
                ? request.reason().trim()
                : null;

        order.setStatus(OrderStatus.CANCELLED);
        order.setCancelledBy(manager);
        order.setCancelledAt(Instant.now());
        order.setCancellationReason(cancellationReason);

        Order savedOrder = orderRepository.save(order);

        OrderStatusHistory history = OrderStatusHistory.builder()
                .order(savedOrder)
                .fromStatus(fromStatus)
                .toStatus(OrderStatus.CANCELLED)
                .changedBy(manager)
                .reason(cancellationReason != null ? cancellationReason : "Quản lý hủy đơn hàng")
                .createdAt(Instant.now())
                .build();
        orderStatusHistoryRepository.save(history);

        log.info("[Order] Quản lý ID={} đã hủy đơn hàng ID={}", managerId, orderId);

        return buildOrderDetailResponse(savedOrder);
    }

    /**
     * Kiểm tra tính hợp lệ của việc chuyển đổi trạng thái đơn hàng theo ma trận vòng đời.
     */
    private void validateStatusTransition(Order order, OrderStatus toStatus) {
        OrderStatus currentStatus = order.getStatus();

        if (currentStatus == OrderStatus.COMPLETED || currentStatus == OrderStatus.CANCELLED) {
            throw new InvalidOrderStateException("Đơn hàng đã ở trạng thái đóng '" + currentStatus + "', không thể chuyển đổi trạng thái.");
        }

        boolean isValid = switch (currentStatus) {
            case PENDING -> {
                if (toStatus == OrderStatus.CONFIRMED) {
                    if (order.getTotalAmount() == null || order.getCalculatedAt() == null) {
                        throw new BusinessRuleException("Đơn hàng chưa được tính giá, không thể chuyển sang trạng thái CONFIRMED.");
                    }
                    yield true;
                }
                yield false;
            }
            case CONFIRMED -> toStatus == OrderStatus.PROCESSING;
            case PROCESSING -> toStatus == OrderStatus.SHIPPING;
            case SHIPPING -> toStatus == OrderStatus.COMPLETED;
            default -> false;
        };

        if (!isValid) {
            throw new InvalidOrderStateException("Không thể chuyển trạng thái đơn hàng từ '" + currentStatus + "' sang '" + toStatus + "'.");
        }
    }

    private OrderDetailResponse buildOrderDetailResponse(Order order) {
        List<OrderItem> items = orderItemRepository.findByOrderIdAndIsDeletedFalse(order.getId());
        List<OrderStatusHistory> history = orderStatusHistoryRepository.findByOrderIdOrderByCreatedAtAsc(order.getId());

        List<OrderItemResponse> itemResponses = items.stream()
                .map(OrderItemResponse::fromEntity)
                .toList();

        List<OrderStatusHistoryResponse> historyResponses = history.stream()
                .map(OrderStatusHistoryResponse::fromEntity)
                .toList();

        return OrderDetailResponse.fromEntity(order, itemResponses, historyResponses);
    }
}

