package com.hmp.sales.feature.order.controller;

import com.hmp.sales.common.response.ApiSuccessResponse;
import com.hmp.sales.common.response.PageResponse;
import com.hmp.sales.common.util.PaginationUtils;
import com.hmp.sales.common.util.SecurityUtils;
import com.hmp.sales.config.AppProperties;
import com.hmp.sales.feature.order.dto.CalculateOrderRequest;
import com.hmp.sales.feature.order.dto.CancelOrderRequest;
import com.hmp.sales.feature.order.dto.ManagerOrderFilterRequest;
import com.hmp.sales.feature.order.dto.ManagerOrderSummaryResponse;
import com.hmp.sales.feature.order.dto.OrderDetailResponse;
import com.hmp.sales.feature.order.dto.UpdateOrderStatusRequest;
import com.hmp.sales.feature.order.entity.OrderStatus;
import com.hmp.sales.feature.order.service.ManagerOrderService;
import jakarta.validation.Valid;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;

/**
 * REST Controller quản lý đơn hàng dành riêng cho Quản lý (Manager) và Admin (/api/v1/manager/orders).
 * <p>
 * Cung cấp các thao tác: tra cứu danh sách toàn bộ đơn hàng phân trang, xem chi tiết,
 * tính giá sản xuất, xác nhận giá, cập nhật tiến trình trạng thái và hủy đơn hàng.
 * </p>
 */
@RestController
@RequestMapping("/api/v1/manager/orders")
public class ManagerOrderController {

    private final ManagerOrderService managerOrderService;
    private final AppProperties appProperties;

    /**
     * Khởi tạo ManagerOrderController với service xử lý đơn hàng quản lý và cấu hình phân trang.
     *
     * @param managerOrderService interface dịch vụ quản lý đơn hàng
     * @param appProperties       cấu hình hệ thống (chứa giới hạn pagination)
     */
    public ManagerOrderController(ManagerOrderService managerOrderService, AppProperties appProperties) {
        this.managerOrderService = managerOrderService;
        this.appProperties = appProperties;
    }

    /**
     * Lấy danh sách toàn bộ đơn hàng trong hệ thống có phân trang và bộ lọc nâng cao.
     *
     * @param dateFrom      lọc đơn hàng tạo từ ngày (YYYY-MM-DD)
     * @param dateTo        lọc đơn hàng tạo đến ngày (YYYY-MM-DD)
     * @param customerName  tìm kiếm theo họ tên khách hàng (hỗ trợ có dấu và không dấu)
     * @param customerPhone tìm kiếm theo số điện thoại khách hàng hoặc người nhận
     * @param customerEmail tìm kiếm theo email khách hàng
     * @param status        lọc theo trạng thái đơn hàng (PENDING, CONFIRMED, PROCESSING, SHIPPING, COMPLETED, CANCELLED)
     * @param page          chỉ số trang (mặc định 0)
     * @param size          kích thước trang (mặc định 20)
     * @return danh sách tóm tắt đơn hàng đóng gói trong {@link ApiSuccessResponse}
     */
    @GetMapping
    public ResponseEntity<ApiSuccessResponse<PageResponse<ManagerOrderSummaryResponse>>> getAllOrders(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateFrom,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateTo,
            @RequestParam(required = false) String customerName,
            @RequestParam(required = false) String customerPhone,
            @RequestParam(required = false) String customerEmail,
            @RequestParam(required = false) OrderStatus status,
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size
    ) {
        int resolvedPage = PaginationUtils.resolvePage(page, appProperties);
        int resolvedSize = PaginationUtils.resolveSize(size, appProperties);
        Pageable pageable = PageRequest.of(resolvedPage, resolvedSize, Sort.by(Sort.Direction.DESC, "createdAt"));
        ManagerOrderFilterRequest filter = new ManagerOrderFilterRequest(dateFrom, dateTo, customerName, customerPhone, customerEmail, status);

        PageResponse<ManagerOrderSummaryResponse> result = managerOrderService.getAllOrders(filter, pageable);
        return ResponseEntity.ok(ApiSuccessResponse.success("Lấy danh sách đơn hàng thành công.", result));
    }

    /**
     * Xem thông tin chi tiết của một đơn hàng bất kỳ trong hệ thống.
     *
     * @param orderId ID đơn hàng cần xem
     * @return chi tiết đơn hàng đóng gói trong {@link ApiSuccessResponse}
     */
    @GetMapping("/{orderId}")
    public ResponseEntity<ApiSuccessResponse<OrderDetailResponse>> getOrderDetail(
            @PathVariable Long orderId
    ) {
        OrderDetailResponse result = managerOrderService.getOrderDetail(orderId);
        return ResponseEntity.ok(ApiSuccessResponse.success("Lấy thông tin chi tiết đơn hàng thành công.", result));
    }

    /**
     * Tính toán đơn giá và chiết khấu giảm giá cho đơn hàng.
     * <p>
     * Cho phép Quản lý tùy chỉnh đơn giá (unitPrice) hoặc giảm giá (discountAmount) cho từng dòng sản phẩm,
     * hoặc áp dụng giảm giá tổng hóa đơn cho cả đơn hàng.
     * Không tự động chuyển trạng thái đơn hàng (giữ nguyên trạng thái PENDING).
     * </p>
     *
     * @param orderId        ID đơn hàng cần tính giá
     * @param request        thông tin chiết khấu và đơn giá tùy biến
     * @param authentication thông tin xác thực của Quản lý
     * @return chi tiết đơn hàng sau khi tính giá
     */
    @PostMapping("/{orderId}/calculate")
    public ResponseEntity<ApiSuccessResponse<OrderDetailResponse>> calculateOrderPrice(
            @PathVariable Long orderId,
            @Valid @RequestBody(required = false) CalculateOrderRequest request,
            Authentication authentication
    ) {
        Long managerId = SecurityUtils.extractUserId(authentication);
        OrderDetailResponse result = managerOrderService.calculateOrderPrice(managerId, orderId, request);
        return ResponseEntity.ok(ApiSuccessResponse.success("Tính giá đơn hàng thành công.", result));
    }

    /**
     * Quản lý xác nhận chốt giá đơn hàng và chuyển trạng thái sang CONFIRMED.
     * <p>
     * Yêu cầu đơn hàng bắt buộc phải đã được tính giá trước đó.
     * Thao tác này kích hoạt thời điểm xác nhận confirmedAt và bắt đầu cửa sổ hủy đơn của khách hàng.
     * </p>
     *
     * @param orderId        ID đơn hàng cần xác nhận
     * @param authentication thông tin xác thực của Quản lý
     * @return chi tiết đơn hàng sau khi xác nhận
     */
    @PostMapping("/{orderId}/confirm")
    public ResponseEntity<ApiSuccessResponse<OrderDetailResponse>> confirmOrder(
            @PathVariable Long orderId,
            Authentication authentication
    ) {
        Long managerId = SecurityUtils.extractUserId(authentication);
        OrderDetailResponse result = managerOrderService.confirmOrder(managerId, orderId);
        return ResponseEntity.ok(ApiSuccessResponse.success("Xác nhận đơn hàng thành công.", result));
    }

    /**
     * Cập nhật trạng thái tiến trình của đơn hàng (CONFIRMED -> PROCESSING -> SHIPPING -> COMPLETED).
     *
     * @param orderId        ID đơn hàng cần cập nhật
     * @param request        trạng thái mới và lý do
     * @param authentication thông tin xác thực của Quản lý
     * @return chi tiết đơn hàng sau khi cập nhật
     */
    @PatchMapping("/{orderId}/status")
    public ResponseEntity<ApiSuccessResponse<OrderDetailResponse>> updateOrderStatus(
            @PathVariable Long orderId,
            @Valid @RequestBody UpdateOrderStatusRequest request,
            Authentication authentication
    ) {
        Long managerId = SecurityUtils.extractUserId(authentication);
        OrderDetailResponse result = managerOrderService.updateOrderStatus(managerId, orderId, request);
        return ResponseEntity.ok(ApiSuccessResponse.success("Cập nhật trạng thái đơn hàng thành công.", result));
    }

    /**
     * Quản lý chủ động hủy đơn hàng (áp dụng cho các trạng thái PENDING, CONFIRMED, PROCESSING, SHIPPING).
     *
     * @param orderId        ID đơn hàng cần hủy
     * @param request        lý do hủy (tùy chọn)
     * @param authentication thông tin xác thực của Quản lý
     * @return chi tiết đơn hàng sau khi hủy
     */
    @PostMapping("/{orderId}/cancel")
    public ResponseEntity<ApiSuccessResponse<OrderDetailResponse>> cancelOrder(
            @PathVariable Long orderId,
            @Valid @RequestBody(required = false) CancelOrderRequest request,
            Authentication authentication
    ) {
        Long managerId = SecurityUtils.extractUserId(authentication);
        OrderDetailResponse result = managerOrderService.cancelOrder(managerId, orderId, request);
        return ResponseEntity.ok(ApiSuccessResponse.success("Hủy đơn hàng thành công.", result));
    }

    /**
     * Quản lý thực hiện hoàn tác hủy đơn hàng (áp dụng cho đơn do Quản lý/Admin hủy).
     *
     * @param orderId        ID đơn hàng cần hoàn tác hủy
     * @param authentication thông tin xác thực của Quản lý
     * @return chi tiết đơn hàng sau khi hoàn tác
     */
    @PostMapping("/{orderId}/undo-cancel")
    public ResponseEntity<ApiSuccessResponse<OrderDetailResponse>> undoCancelOrder(
            @PathVariable Long orderId,
            Authentication authentication
    ) {
        Long managerId = SecurityUtils.extractUserId(authentication);
        OrderDetailResponse result = managerOrderService.undoCancelOrder(managerId, orderId);
        return ResponseEntity.ok(ApiSuccessResponse.success("Hoàn tác hủy đơn hàng thành công.", result));
    }
}
