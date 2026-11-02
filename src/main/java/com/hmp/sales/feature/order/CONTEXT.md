# Customer Order Feature Context

## 1. Overview
Phân hệ Đơn hàng Khách hàng (Customer Orders) tại `com.hmp.sales.feature.order`.
Quản lý toàn bộ tiến trình từ khi khách hàng tạo đơn hàng tôn lợp mái, lưu snapshot bất biến thông tin người nhận và quy cách sản phẩm, tra cứu đơn hàng cá nhân, hủy đơn trong thời hạn cho phép, và tự động chuyển trạng thái đơn sang `PROCESSING` sau khi hết thời hạn hủy.

## 2. Entities & Schema Mapping
Các thực thể JPA tại `com.hmp.sales.feature.order.entity`:
- `Order`: Đại diện bảng vật lý `orders` trong PostgreSQL (Durable Source of Truth).
  - Khóa chính `order_id` (`Long`).
  - Liên kết khách hàng `@ManyToOne` với `User` (`customer_id`).
  - `status`: `OrderStatus` (`VARCHAR(30)`).
  - Snapshot người nhận bất biến: `receiver_name` (`VARCHAR(150)`), `receiver_phone` (`VARCHAR(20)`), `shipping_address` (`VARCHAR(1000)`), `note` (`VARCHAR(1000)`).
  - Snapshot tài chính & thời gian: `total_amount` (`Long`, VND), `confirmed_at` (thời điểm Quản lý chốt giá), `cancelled_by`, `cancelled_at`, `cancellation_reason`.
  - Audit: `created_at`, `updated_at`, `is_deleted`.
- `OrderItem`: Đại diện bảng `order_items`.
  - Khóa chính `order_item_id` (`Long`).
  - Liên kết `@ManyToOne` với `Order`, `Product`, `ProductVariant`, `Unit`.
  - Thông số kỹ thuật: `quantity` (`BigDecimal`), `size` (`BigDecimal`, chiều dài tôn sóng tính bằng mét).
  - Snapshot bất biến: `product_name_snapshot` (`VARCHAR(250)`), `unit_name_snapshot` (`VARCHAR(50)`), `color_snapshot` (`VARCHAR(100)`).
  - Đơn giá & thành tiền: `unit_price` (`Integer`), `total_price` (`Long`).
- `OrderStatusHistory`: Đại diện bảng `order_status_histories`.
  - Khóa chính `history_id` (`Long`).
  - `order_id`: Liên kết đơn hàng.
  - `from_status` (nullable cho trạng thái ban đầu), `to_status` (`OrderStatus`).
  - `changed_by` (FK `User`, nullable khi hệ thống tự động chuyển).
  - `reason` (`VARCHAR(500)`), `created_at` (`Instant`).

## 3. Enums
- `OrderStatus`:
  - `PENDING`: Đơn mới tạo, chờ Quản lý tiếp nhận và tính giá.
  - `CONFIRMED`: Quản lý đã tính giá và chốt giá. Đơn hàng ở trạng thái này trong khoảng thời gian `cancellationWindowMinutes` để khách xem giá và có quyền hủy.
  - `PROCESSING`: Đang gia công sản xuất tôn / chuẩn bị hàng. Chuyển tự động từ `CONFIRMED` sau khi hết thời hạn hủy hoặc do Quản lý chủ động thao tác.
  - `SHIPPING`: Đang giao hàng.
  - `COMPLETED`: Giao hàng thành công và hoàn tất đơn.
  - `CANCELLED`: Đã hủy đơn hàng.

## 4. Repositories
Tại `com.hmp.sales.feature.order.repository`:
- `OrderRepository`:
  - `findByIdAndIsDeletedFalse`: Tìm đơn theo ID chưa xóa mềm.
  - `findByIdAndCustomerIdAndIsDeletedFalse`: Tìm đơn theo ID và khách hàng sở hữu.
  - `findByStatusAndConfirmedAtBeforeAndIsDeletedFalse`: Quét các đơn `CONFIRMED` hết hạn hủy cho Scheduler.
  - Kế thừa `JpaSpecificationExecutor<Order>` phục vụ bộ lọc linh hoạt (ngày, trạng thái).
- `OrderItemRepository`:
  - `findByOrderIdAndIsDeletedFalse`: Lấy danh sách dòng sản phẩm của đơn hàng.
- `OrderStatusHistoryRepository`:
  - `findByOrderIdOrderByCreatedAtAsc`: Lấy toàn bộ lịch sử trạng thái của đơn hàng.

## 5. Technical Decisions & Business Rules
1. **Bảo chứng xác thực OTP**:
   - Khi tạo đơn hàng (`POST /api/v1/orders`), số điện thoại người nhận `receiverPhone` bắt buộc phải có bảo chứng xác thực trên Redis (`otp:verified:{phone}`) HOẶC trùng khớp với số điện thoại đã được xác thực lưu trên tài khoản `User`.
   - Nếu không thỏa mãn, ném ngoại lệ `OtpVerificationException`.
2. **Kiểm tra trạng thái sản phẩm (ACTIVE)**:
   - Các sản phẩm đặt mua phải đang ở trạng thái `ACTIVE` và chưa xóa mềm. Nếu sản phẩm bị khóa `INACTIVE`, ném `BusinessRuleException` từ chối đặt hàng.
   - Biến thể sản phẩm phải thuộc đúng sản phẩm và đang ở trạng thái `ACTIVE`.
3. **Snapshot bất biến**:
   - Tên sản phẩm, đơn vị tính, màu sắc được snapshot trực tiếp vào `OrderItem` để bảo vệ lịch sử đơn hàng ngay cả khi sản phẩm hoặc danh mục bị sửa đổi hoặc xóa mềm sau này.
4. **Khách hàng không cần xác nhận duyệt đơn**:
   - Sau khi Quản lý tính giá và confirm, đơn hàng chuyển sang `CONFIRMED`.
   - Khách hàng chỉ cần vào xem giá (`GET /api/v1/orders/{orderId}`). Loại bỏ endpoint `confirm` của khách hàng.
5. **Cấu hình động `cancellationWindowMinutes` từ Database**:
   - Không hardcode thời hạn hủy đơn. Lấy động qua `SystemSettingService.getCancellationWindowMinutes()` tra cứu khóa `ORDER_CUSTOMER_CANCELLATION_WINDOW_MINUTES` trong PostgreSQL (kèm fallback về `app.order.cancellation-window-minutes: 30`).
6. **Hủy đơn hàng**:
   - Chỉ cho phép hủy khi đơn ở trạng thái `PENDING` hoặc `CONFIRMED`.
   - Mốc thời gian tính hạn: `order.getConfirmedAt()` (nếu đã confirm giá) hoặc `order.getCreatedAt()`.
   - Nếu quá thời hạn, ném `BusinessRuleException`.
   - Lý do hủy `cancellationReason` là tùy chọn (optional).
7. **Tự động chuyển sang `PROCESSING`**:
   - `OrderAutoTransitionScheduler` chạy định kỳ mỗi 60 giây (`@Scheduled(fixedDelay = 60000)`).
   - Quét các đơn `CONFIRMED` có `confirmedAt + cancellationWindowMinutes <= now()`.
   - Tự động cập nhật `status = PROCESSING` và ghi nhận bản ghi `OrderStatusHistory` với `changedBy = null` (Hệ thống).

## 6. REST API Endpoints
Prefix: `/api/v1/orders`
- `POST /api/v1/orders`: Đặt đơn hàng mới (201 Created).
- `GET /api/v1/orders`: Danh sách đơn hàng cá nhân (200 OK, phân trang chuẩn `PageResponse<OrderSummaryResponse>`).
- `GET /api/v1/orders/{orderId}`: Xem chi tiết đơn hàng (200 OK, `OrderDetailResponse`).
- `POST /api/v1/orders/{orderId}/cancel`: Hủy đơn hàng trong thời hạn cho phép (200 OK).

## 7. Security & Authorization
- `ORDER_CREATE`: Cấp quyền `POST /api/v1/orders` cho `ROLE_CUSTOMER`, `ROLE_ADMIN`, `ROLE_MANAGER`.
- `ORDER_VIEW_OWN`: Cấp quyền `GET /api/v1/orders/**` cho `ROLE_CUSTOMER`, `ROLE_ADMIN`, `ROLE_MANAGER`.
- `ORDER_CANCEL_OWN`: Cấp quyền `POST /api/v1/orders/**/cancel` cho `ROLE_CUSTOMER`, `ROLE_ADMIN`, `ROLE_MANAGER`.
- Tự động nạp vào bộ nhớ RAM của `PermissionAuthorizationManager` khi ứng dụng khởi động hoặc khi vai trò được cập nhật.

