package com.hmp.sales.feature.order.controller;

import com.hmp.sales.common.response.ApiSuccessResponse;
import com.hmp.sales.common.response.PageResponse;
import com.hmp.sales.common.util.PaginationUtils;
import com.hmp.sales.common.util.SecurityUtils;
import com.hmp.sales.config.AppProperties;
import com.hmp.sales.feature.order.dto.CancelOrderRequest;
import com.hmp.sales.feature.order.dto.CreateOrderRequest;
import com.hmp.sales.feature.order.dto.OrderDetailResponse;
import com.hmp.sales.feature.order.dto.OrderFilterRequest;
import com.hmp.sales.feature.order.dto.OrderSummaryResponse;
import com.hmp.sales.feature.order.entity.OrderStatus;
import com.hmp.sales.feature.order.service.CustomerOrderService;
import jakarta.validation.Valid;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;

/**
 * REST Controller tiếp nhận các yêu cầu xử lý đơn hàng từ Khách hàng (/api/v1/orders).
 * <p>
 * Bắt buộc người dùng phải đăng nhập hợp lệ với JWT token.
 * Controller đóng vai trò mỏng (thin controller), chỉ điều hướng request và trả về response chuẩn.
 * </p>
 */
@RestController
@RequestMapping("/api/v1/orders")
public class CustomerOrderController {

    private final CustomerOrderService customerOrderService;
    private final AppProperties appProperties;

    /**
     * Khởi tạo CustomerOrderController với interface dịch vụ đơn hàng và cấu hình ứng dụng.
     *
     * @param customerOrderService interface nghiệp vụ đơn hàng khách hàng
     * @param appProperties       cấu hình ứng dụng chung (dùng cho phân trang)
     */
    public CustomerOrderController(CustomerOrderService customerOrderService,
                                   AppProperties appProperties) {
        this.customerOrderService = customerOrderService;
        this.appProperties = appProperties;
    }

    /**
     * Tạo đơn hàng mới từ phía khách hàng.
     *
     * @param request        thông tin người nhận và danh sách sản phẩm đặt mua
     * @param authentication thông tin định danh của khách hàng đăng nhập
     * @return phản hồi 201 Created kèm {@link OrderDetailResponse}
     */
    @PostMapping
    public ResponseEntity<ApiSuccessResponse<OrderDetailResponse>> createOrder(
            @Valid @RequestBody CreateOrderRequest request,
            Authentication authentication) {
        Long customerId = SecurityUtils.extractUserId(authentication);
        OrderDetailResponse response = customerOrderService.createOrder(customerId, request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiSuccessResponse.created("Đặt hàng thành công", response));
    }

    /**
     * Lấy danh sách đơn hàng của khách hàng hiện tại có phân trang và bộ lọc.
     *
     * @param dateFrom       lọc từ ngày (YYYY-MM-DD)
     * @param dateTo         lọc đến ngày (YYYY-MM-DD)
     * @param status         lọc theo trạng thái đơn hàng
     * @param page           chỉ số trang (bắt đầu từ 0)
     * @param size           kích thước mỗi trang
     * @param authentication thông tin định danh khách hàng
     * @return danh sách tóm tắt đơn hàng đóng gói trong {@link PageResponse}
     */
    @GetMapping
    public ResponseEntity<ApiSuccessResponse<PageResponse<OrderSummaryResponse>>> getOwnOrders(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateFrom,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateTo,
            @RequestParam(required = false) OrderStatus status,
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size,
            Authentication authentication) {
        Long customerId = SecurityUtils.extractUserId(authentication);

        int resolvedPage = PaginationUtils.resolvePage(page, appProperties);
        int resolvedSize = PaginationUtils.resolveSize(size, appProperties);
        Pageable pageable = PageRequest.of(resolvedPage, resolvedSize, Sort.by(Sort.Direction.DESC, "createdAt"));

        OrderFilterRequest filter = new OrderFilterRequest(dateFrom, dateTo, status, resolvedPage, resolvedSize);
        PageResponse<OrderSummaryResponse> response = customerOrderService.getOwnOrders(customerId, filter, pageable);

        return ResponseEntity.ok(ApiSuccessResponse.success("Lấy danh sách đơn hàng thành công", response));
    }

    /**
     * Xem thông tin chi tiết một đơn hàng cụ thể của khách hàng.
     *
     * @param orderId        ID đơn hàng cần xem
     * @param authentication thông tin định danh khách hàng
     * @return chi tiết đơn hàng kèm danh sách sản phẩm và lịch sử trạng thái
     */
    @GetMapping("/{orderId}")
    public ResponseEntity<ApiSuccessResponse<OrderDetailResponse>> getOwnOrderDetail(
            @PathVariable Long orderId,
            Authentication authentication) {
        Long customerId = SecurityUtils.extractUserId(authentication);
        OrderDetailResponse response = customerOrderService.getOwnOrderDetail(customerId, orderId);
        return ResponseEntity.ok(ApiSuccessResponse.success("Lấy chi tiết đơn hàng thành công", response));
    }

    /**
     * Khách hàng thực hiện hủy đơn hàng trong thời hạn cho phép.
     *
     * @param orderId        ID đơn hàng cần hủy
     * @param request        thông tin lý do hủy (tùy chọn)
     * @param authentication thông tin định danh khách hàng
     * @return chi tiết đơn hàng sau khi hủy thành công
     */
    @PostMapping("/{orderId}/cancel")
    public ResponseEntity<ApiSuccessResponse<OrderDetailResponse>> cancelOwnOrder(
            @PathVariable Long orderId,
            @Valid @RequestBody(required = false) CancelOrderRequest request,
            Authentication authentication) {
        Long customerId = SecurityUtils.extractUserId(authentication);
        OrderDetailResponse response = customerOrderService.cancelOwnOrder(customerId, orderId, request);
        return ResponseEntity.ok(ApiSuccessResponse.success("Hủy đơn hàng thành công", response));
    }
}

