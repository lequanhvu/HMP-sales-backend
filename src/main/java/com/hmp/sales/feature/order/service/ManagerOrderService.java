package com.hmp.sales.feature.order.service;

import com.hmp.sales.common.response.PageResponse;
import com.hmp.sales.feature.order.dto.CalculateOrderRequest;
import com.hmp.sales.feature.order.dto.CancelOrderRequest;
import com.hmp.sales.feature.order.dto.ManagerOrderFilterRequest;
import com.hmp.sales.feature.order.dto.ManagerOrderSummaryResponse;
import com.hmp.sales.feature.order.dto.OrderDetailResponse;
import com.hmp.sales.feature.order.dto.UpdateOrderStatusRequest;
import org.springframework.data.domain.Pageable;

/**
 * Interface định nghĩa các nghiệp vụ xử lý, tính giá và quản lý vòng đời đơn hàng dành cho Quản lý (Manager) và Admin.
 */
public interface ManagerOrderService {

    /**
     * Lấy danh sách toàn bộ đơn hàng trong hệ thống có phân trang và bộ lọc nâng cao.
     *
     * @param filter   bộ lọc theo ngày, trạng thái, tên, số điện thoại, email
     * @param pageable thông tin phân trang và sắp xếp
     * @return danh sách tóm tắt đơn hàng đóng gói trong {@link PageResponse}
     */
    PageResponse<ManagerOrderSummaryResponse> getAllOrders(ManagerOrderFilterRequest filter, Pageable pageable);

    /**
     * Xem thông tin chi tiết một đơn hàng bất kỳ trong hệ thống.
     *
     * @param orderId ID đơn hàng cần xem
     * @return {@link OrderDetailResponse} chi tiết đơn hàng kèm danh mục sản phẩm và lịch sử trạng thái
     */
    OrderDetailResponse getOrderDetail(Long orderId);

    /**
     * Tính toán đơn giá và chiết khấu giảm giá cho đơn hàng.
     * <p>
     * Hỗ trợ 3 cơ chế giảm giá:
     * <ul>
     *   <li>Giảm theo đơn giá: truyền {@code unitPrice} mới cho từng dòng sản phẩm.</li>
     *   <li>Giảm theo số tiền: truyền {@code discountAmount} cho từng dòng hoặc cho toàn bộ đơn hàng.</li>
     *   <li>Kết hợp cả hai: vừa điều chỉnh đơn giá, vừa giảm thêm số tiền thành tiền.</li>
     * </ul>
     * Thao tác này chỉ tính toán và cập nhật số tiền, không tự động chuyển trạng thái đơn hàng (giữ nguyên {@code PENDING}).
     * </p>
     *
     * @param managerId ID Quản lý thực hiện tính giá
     * @param orderId   ID đơn hàng cần tính giá
     * @param request   thông tin đơn giá tùy chỉnh và giảm giá
     * @return {@link OrderDetailResponse} chi tiết đơn hàng sau khi tính toán
     */
    OrderDetailResponse calculateOrderPrice(Long managerId, Long orderId, CalculateOrderRequest request);

    /**
     * Xác nhận chốt giá đơn hàng và chuyển trạng thái sang {@code CONFIRMED}.
     * <p>
     * Yêu cầu đơn hàng bắt buộc phải đã được tính giá trước đó ({@code totalAmount != null}).
     * Kích hoạt thời điểm xác nhận {@code confirmedAt} và cửa sổ hủy đơn của khách hàng.
     * </p>
     *
     * @param managerId ID Quản lý thực hiện xác nhận
     * @param orderId   ID đơn hàng cần xác nhận
     * @return {@link OrderDetailResponse} chi tiết đơn hàng sau khi xác nhận
     */
    OrderDetailResponse confirmOrder(Long managerId, Long orderId);

    /**
     * Cập nhật trạng thái tiến trình của đơn hàng theo vòng đời (CONFIRMED -> PROCESSING -> SHIPPING -> COMPLETED).
     *
     * @param managerId ID Quản lý thực hiện cập nhật
     * @param orderId   ID đơn hàng cần cập nhật
     * @param request   thông tin trạng thái mới và lý do
     * @return {@link OrderDetailResponse} chi tiết đơn hàng sau khi cập nhật trạng thái
     */
    OrderDetailResponse updateOrderStatus(Long managerId, Long orderId, UpdateOrderStatusRequest request);

    /**
     * Quản lý hủy đơn hàng (áp dụng cho các trạng thái PENDING, CONFIRMED, PROCESSING, SHIPPING).
     *
     * @param managerId ID Quản lý thực hiện hủy
     * @param orderId   ID đơn hàng cần hủy
     * @param request   thông tin yêu cầu hủy (lý do hủy là tùy chọn)
     * @return {@link OrderDetailResponse} chi tiết đơn hàng sau khi hủy
     */
    OrderDetailResponse cancelOrder(Long managerId, Long orderId, CancelOrderRequest request);
}

