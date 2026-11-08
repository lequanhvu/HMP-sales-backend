package com.hmp.sales.feature.order.service;

import com.hmp.sales.common.response.PageResponse;
import com.hmp.sales.feature.order.dto.CancelOrderRequest;
import com.hmp.sales.feature.order.dto.CreateOrderRequest;
import com.hmp.sales.feature.order.dto.OrderDetailResponse;
import com.hmp.sales.feature.order.dto.OrderFilterRequest;
import com.hmp.sales.feature.order.dto.OrderSummaryResponse;
import org.springframework.data.domain.Pageable;

/**
 * Interface định nghĩa các nghiệp vụ xử lý đơn hàng dành riêng cho Khách hàng.
 */
public interface CustomerOrderService {

    /**
     * Tạo đơn hàng mới từ giỏ hàng hoặc thông tin khách hàng nhập.
     * <p>
     * Yêu cầu số điện thoại người nhận phải có bảo chứng xác thực OTP hợp lệ,
     * các sản phẩm phải đang hoạt động (ACTIVE).
     * Đơn hàng được tạo với trạng thái khởi đầu {@code PENDING} và lưu trực tiếp vào PostgreSQL.
     * </p>
     *
     * @param customerId ID khách hàng thực hiện đặt đơn
     * @param request    thông tin chi tiết đơn hàng
     * @return {@link OrderDetailResponse} chi tiết đơn hàng vừa tạo
     */
    OrderDetailResponse createOrder(Long customerId, CreateOrderRequest request);

    /**
     * Tra cứu danh sách đơn hàng của khách hàng hiện tại có phân trang và bộ lọc.
     *
     * @param customerId ID khách hàng
     * @param filter     bộ lọc theo ngày và trạng thái
     * @param pageable   thông tin phân trang và sắp xếp
     * @return danh sách tóm tắt đơn hàng đóng gói trong {@link PageResponse}
     */
    PageResponse<OrderSummaryResponse> getOwnOrders(Long customerId, OrderFilterRequest filter, Pageable pageable);

    /**
     * Xem thông tin chi tiết một đơn hàng cụ thể của khách hàng.
     *
     * @param customerId ID khách hàng
     * @param orderId    ID đơn hàng cần xem
     * @return {@link OrderDetailResponse} chi tiết đơn hàng kèm danh sách sản phẩm và lịch sử
     */
    OrderDetailResponse getOwnOrderDetail(Long customerId, Long orderId);

    /**
     * Hủy đơn hàng của khách hàng trong thời gian cho phép (cancellationWindowMinutes).
     *
     * @param customerId ID khách hàng
     * @param orderId    ID đơn hàng cần hủy
     * @param request    thông tin yêu cầu hủy (lý do hủy là tùy chọn)
     * @return {@link OrderDetailResponse} chi tiết đơn hàng sau khi cập nhật trạng thái CANCELLED
     */
    OrderDetailResponse cancelOwnOrder(Long customerId, Long orderId, CancelOrderRequest request);

    /**
     * Khách hàng thực hiện hoàn tác hủy đơn hàng trong thời hạn cho phép (undoCancelWindowMinutes).
     * <p>
     * Đơn hàng chỉ có thể hoàn tác nếu đang ở trạng thái CANCELLED, do chính khách hàng hủy,
     * và còn trong thời hạn hoàn tác (mặc định 60 phút). Đơn hàng sẽ được khôi phục về trạng thái
     * ngay trước khi bị hủy (PENDING hoặc CONFIRMED) với mốc confirmedAt ban đầu được giữ nguyên.
     * </p>
     *
     * @param customerId ID khách hàng thực hiện hoàn tác
     * @param orderId    ID đơn hàng cần hoàn tác hủy
     * @return {@link OrderDetailResponse} chi tiết đơn hàng sau khi hoàn tác
     */
    OrderDetailResponse undoCancelOrder(Long customerId, Long orderId);
}

