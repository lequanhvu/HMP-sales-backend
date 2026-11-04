# Order Feature Context

## 1. Overview
Phân hệ Đơn hàng (Orders) tại `com.hmp.sales.feature.order`.
Quản lý toàn bộ vòng đời đơn hàng:
- Khách hàng (Customer): Tạo đơn hàng tôn lợp mái, lưu snapshot bất biến thông tin người nhận và quy cách sản phẩm, tra cứu đơn hàng cá nhân, hủy đơn trong thời hạn cho phép.
- Quản lý (Manager) / Admin: Tra cứu, tìm kiếm, lọc toàn bộ đơn hàng phân trang; tính giá sản xuất và áp dụng chiết khấu linh hoạt; xác nhận chốt giá; cập nhật tiến trình trạng thái; chủ động hủy đơn hàng ở các giai đoạn (kể cả khi đang giao hàng `SHIPPING`).
- Scheduler nền (`OrderAutoTransitionScheduler`): Tự động chuyển đơn sang `PROCESSING` sau khi hết thời hạn hủy.

## 2. Entities & Schema Mapping
Các thực thể JPA tại `com.hmp.sales.feature.order.entity`:
- `Order`: Đại diện bảng vật lý `orders` trong PostgreSQL (Durable Source of Truth).
  - Khóa chính `order_id` (`Long`).
  - Liên kết khách hàng `@ManyToOne` với `User` (`customer_id`).
  - `status`: `OrderStatus` (`VARCHAR(30)`).
  - Snapshot người nhận bất biến: `receiver_name` (`VARCHAR(150)`), `receiver_phone` (`VARCHAR(20)`), `shipping_address` (`VARCHAR(1000)`), `note` (`VARCHAR(1000)`).
  - Snapshot tài chính:
    - `subtotal_amount` (`Long`, VND): Tổng tiền hàng trước khi giảm giá đơn hàng (tổng `totalPrice` các items).
    - `discount_amount` (`Long`, VND, default 0): Số tiền giảm giá cho toàn bộ đơn hàng.
    - `total_amount` (`Long`, VND): Tổng số tiền thanh toán thực tế (`subtotalAmount - discountAmount`).
  - Snapshot mốc thời gian & nhân sự: `calculated_by`, `calculated_at`, `confirmed_at`, `completed_at`, `cancelled_by`, `cancelled_at`, `cancellation_reason`.
  - Audit: `created_at`, `updated_at`, `is_deleted`.
- `OrderItem`: Đại diện bảng `order_items`.
  - Khóa chính `order_item_id` (`Long`).
  - Liên kết `@ManyToOne` với `Order`, `Product`, `ProductVariant`, `Unit`.
  - Thông số kỹ thuật: `quantity` (`BigDecimal`), `size` (`BigDecimal`, chiều dài tôn sóng tính bằng mét).
  - Snapshot bất biến: `product_name_snapshot` (`VARCHAR(250)`), `variant_name_snapshot` (`VARCHAR(150)`), `unit_name_snapshot` (`VARCHAR(50)`), `color_snapshot` (`VARCHAR(100)`).
  - Đơn giá & chiết khấu:
    - `unit_price` (`Integer`): Đơn giá áp dụng (tra cứu từ `ProductPrice` hoặc Quản lý nhập giảm theo đơn giá).
    - `discount_amount` (`Long`, default 0): Số tiền giảm giá trên dòng sản phẩm.
    - `total_price` (`Long`): Thành tiền của dòng sản phẩm (`max(0, UnitPrice * (Size != null ? Size : 1) * Quantity - discountAmount)`).
- `OrderStatusHistory`: Đại diện bảng `order_status_histories`.
  - Khóa chính `history_id` (`Long`).
  - `order_id`: Liên kết đơn hàng.
  - `from_status` (nullable cho trạng thái ban đầu), `to_status` (`OrderStatus`).
  - `changed_by` (FK `User`, nullable khi hệ thống tự động chuyển).
  - `reason` (`VARCHAR(500)`), `created_at` (`Instant`).

## 3. Enums
- `OrderStatus`:
  - `PENDING`: Đơn mới tạo, chờ Quản lý tiếp nhận và tính giá.
  - `CONFIRMED`: Quản lý đã tính giá và chốt giá (kích hoạt `confirmedAt` và cửa sổ hủy đơn của khách).
  - `PROCESSING`: Đang gia công sản xuất tôn / chuẩn bị hàng. Chuyển tự động từ `CONFIRMED` sau khi hết thời hạn hủy hoặc do Quản lý chủ động thao tác.
  - `SHIPPING`: Đang giao hàng đến khách. Quản lý vẫn có thể hủy đơn nếu xảy ra sự cố giao hàng.
  - `COMPLETED`: Giao hàng thành công và hoàn tất đơn.
  - `CANCELLED`: Đã hủy đơn hàng (bởi Khách hàng hoặc Quản lý).

## 4. Repositories
Tại `com.hmp.sales.feature.order.repository`:
- `OrderRepository`:
  - `findByIdAndIsDeletedFalse`: Tìm đơn theo ID chưa xóa mềm.
  - `findByIdAndCustomerIdAndIsDeletedFalse`: Tìm đơn theo ID và khách hàng sở hữu.
  - `findByStatusAndConfirmedAtBeforeAndIsDeletedFalse`: Quét các đơn `CONFIRMED` hết hạn hủy cho Scheduler.
  - Kế thừa `JpaSpecificationExecutor<Order>` phục vụ bộ lọc linh hoạt (ngày, trạng thái, tên, SĐT, email).
- `OrderItemRepository`:
  - `findByOrderIdAndIsDeletedFalse`: Lấy danh sách dòng sản phẩm của đơn hàng.
- `OrderStatusHistoryRepository`:
  - `findByOrderIdOrderByCreatedAtAsc`: Lấy toàn bộ lịch sử trạng thái của đơn hàng.
- `OrderSpecification`:
  - `forCustomer(customerId, filter)`: Specification cho khách hàng tra cứu đơn của mình.
  - `forManager(filter)`: Specification cho Quản lý / Admin tra cứu toàn bộ đơn hàng (tìm kiếm tên không dấu qua `VietnameseStringUtils`, SĐT, email, ngày, trạng thái).

## 5. Technical Decisions & Business Rules
1. **Bảo chứng xác thực OTP**:
   - Khi tạo đơn hàng (`POST /api/v1/orders`), số điện thoại người nhận `receiverPhone` bắt buộc phải có bảo chứng xác thực trên Redis (`otp:verified:{phone}`) HOẶC trùng khớp với số điện thoại đã được xác thực lưu trên tài khoản `User`.
2. **Kiểm tra trạng thái sản phẩm, biến thể, màu sắc (`colorId`) & đơn vị tính (`unitId`)**:
   - Sản phẩm đặt mua phải đang ở trạng thái `ACTIVE` và chưa xóa mềm.
   - Biến thể sản phẩm phải thuộc đúng sản phẩm và đang `ACTIVE`.
   - Màu sắc và đơn vị tính bắt buộc phải tồn tại trong danh mục biến thể (`existsByVariantIdAndColorId` và `existsByVariantIdAndUnitId`).
3. **Cơ chế tính giá & chiết khấu giảm giá linh hoạt (Pricing & Discount)**:
   - **Giảm theo đơn giá**: Quản lý có thể nhập `unitPrice` mới cho từng item. Nếu không nhập, hệ thống tự động tra cứu từ `ProductPrice`.
   - **Giảm theo số tiền**: Quản lý có thể nhập `discountAmount` cho từng dòng item hoặc cho toàn bộ đơn hàng.
   - **Kết hợp cả hai**: Cho phép truyền cả `unitPrice` mới và `discountAmount` cho cùng một dòng sản phẩm.
   - Công thức dòng:
     $$\text{Gross} = \text{UnitPrice} \times (\text{Size} \neq \text{null} \ ?\ \text{Size} : 1) \times \text{Quantity}$$
     $$\text{TotalPrice} = \max(0, \text{Gross} - \text{ItemDiscount})$$
   - Công thức đơn hàng:
     $$\text{SubtotalAmount} = \sum \text{TotalPrice}$$
     $$\text{TotalAmount} = \max(0, \text{SubtotalAmount} - \text{OrderDiscount})$$
4. **Tách biệt hoàn toàn Tính giá (`calculate`) & Xác nhận đơn (`confirm`)**:
   - Thao tác `calculate` chỉ tính toán và cập nhật số tiền, **giữ nguyên trạng thái `PENDING`** (Quản lý có thể tính lại nhiều lần khi thỏa thuận giá thay đổi).
   - Thao tác `confirm` là bước phê duyệt độc lập sau khi Quản lý chốt giá. Yêu cầu đơn hàng bắt buộc phải có `totalAmount != null` mới được confirm. Khi confirm mới bắt đầu kích hoạt `confirmedAt` và cửa sổ hủy đơn của khách hàng.
5. **Mở rộng quyền hủy đơn cho Quản lý**:
   - Quản lý có quyền hủy đơn hàng ở tất cả các trạng thái trước khi hoàn tất: `PENDING`, `CONFIRMED`, `PROCESSING`, và **`SHIPPING`** (lý do hủy là tùy chọn, ghi vết `OrderStatusHistory`).
6. **Cấu hình động `cancellationWindowMinutes` từ Database**:
   - Lấy động qua `SystemSettingService.getCancellationWindowMinutes()` tra cứu khóa `ORDER_CUSTOMER_CANCELLATION_WINDOW_MINUTES` trong PostgreSQL (kèm fallback về 30 phút).
7. **Tự động chuyển sang `PROCESSING`**:
   - `OrderAutoTransitionScheduler` chạy định kỳ mỗi 60 giây (`@Scheduled(fixedDelay = 60000)`).
   - Quét các đơn `CONFIRMED` có `confirmedAt + cancellationWindowMinutes <= now()`.
   - Tự động cập nhật `status = PROCESSING` và ghi nhận bản ghi `OrderStatusHistory` với `changedBy = null` (Hệ thống).

## 6. REST API Endpoints
### Khách hàng (`/api/v1/orders`):
- `POST /api/v1/orders`: Đặt đơn hàng mới (201 Created).
- `GET /api/v1/orders`: Danh sách đơn hàng cá nhân (200 OK, phân trang chuẩn `PageResponse<OrderSummaryResponse>`).
- `GET /api/v1/orders/{orderId}`: Xem chi tiết đơn hàng (200 OK, `OrderDetailResponse`).
- `POST /api/v1/orders/{orderId}/cancel`: Hủy đơn hàng trong thời hạn cho phép (200 OK).

### Quản lý / Admin (`/api/v1/manager/orders`):
- `GET /api/v1/manager/orders`: Tra cứu danh sách toàn bộ đơn hàng phân trang chuẩn `PageResponse<ManagerOrderSummaryResponse>` (bộ lọc ngày, trạng thái, tên khách không dấu, SĐT, email).
- `GET /api/v1/manager/orders/{orderId}`: Xem chi tiết toàn diện đơn hàng (200 OK, `OrderDetailResponse`).
- `POST /api/v1/manager/orders/{orderId}/calculate`: Tính giá sản xuất và áp dụng chiết khấu giảm giá (giữ nguyên trạng thái `PENDING`).
- `POST /api/v1/manager/orders/{orderId}/confirm`: Quản lý xác nhận chốt giá đơn hàng (chuyển sang `CONFIRMED`).
- `PATCH /api/v1/manager/orders/{orderId}/status`: Cập nhật trạng thái tiến trình đơn hàng (`CONFIRMED -> PROCESSING -> SHIPPING -> COMPLETED`).
- `POST /api/v1/manager/orders/{orderId}/cancel`: Quản lý chủ động hủy đơn hàng (áp dụng cho `PENDING`, `CONFIRMED`, `PROCESSING`, `SHIPPING`).

## 7. Security & Authorization
- `ORDER_CREATE`: Cấp quyền `POST /api/v1/orders` cho `ROLE_CUSTOMER`, `ROLE_ADMIN`, `ROLE_MANAGER`.
- `ORDER_VIEW_OWN`: Cấp quyền `GET /api/v1/orders/**` cho `ROLE_CUSTOMER`, `ROLE_ADMIN`, `ROLE_MANAGER`.
- `ORDER_CANCEL_OWN`: Cấp quyền `POST /api/v1/orders/**/cancel` cho `ROLE_CUSTOMER`, `ROLE_ADMIN`, `ROLE_MANAGER`.
- `ORDER_VIEW_ALL`: Cấp quyền `GET /api/v1/manager/orders/**` cho `ROLE_MANAGER`, `ROLE_ADMIN`.
- `ORDER_CALCULATE`: Cấp quyền `POST /api/v1/manager/orders/**/calculate` cho `ROLE_MANAGER`, `ROLE_ADMIN`.
- `ORDER_CONFIRM`: Cấp quyền `POST /api/v1/manager/orders/**/confirm` cho `ROLE_MANAGER`, `ROLE_ADMIN`.
- `ORDER_STATUS_UPDATE`: Cấp quyền `PATCH /api/v1/manager/orders/**/status` cho `ROLE_MANAGER`, `ROLE_ADMIN`.
- `ORDER_CANCEL_ANY`: Cấp quyền `POST /api/v1/manager/orders/**/cancel` cho `ROLE_MANAGER`, `ROLE_ADMIN`.
- Tự động nạp vào bộ nhớ RAM của `PermissionAuthorizationManager` khi ứng dụng khởi động hoặc khi vai trò được cập nhật.
