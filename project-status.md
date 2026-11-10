# Project Status

> Last updated: 2026-11-09 | By: @antigravity | Session: #20
>
> AI: update this file at the end of every session when asked.
> Follow this exact format. Preserve full milestone checklists and project specifications.

---

## Completed
- ✅ **Nâng cấp Kiểm tra Xác thực Khóa Danh mục (`lockCategory`) theo Cơ chế Xác nhận 2 bước & Kiểm tra Đơn dở dang completed**:
  - **Cơ chế Xác nhận Hai Bước khi Khóa (`POST /api/v1/manager/categories/{id}/lock?force=false`)**:
    - Khi danh mục rỗng: khóa ngay lập tức (`status = LOCKED`), ghi audit trail `CATEGORY_LOCKED`.
    - Khi danh mục còn chứa danh mục con hoặc sản phẩm và `force = false`: ném `ConflictWithPayloadException` (HTTP 409 Conflict) kèm DTO `CategoryLockWarningResponse` (`childCategoryCount`, `productCount`, `totalImpactCount`) để frontend hiển thị popup xác nhận.
    - Chặn khóa danh mục đã ở trạng thái `LOCKED` (ném `BusinessRuleException`).
  - **Kiểm tra Toàn vẹn & Cascade Khóa khi `force = true`**:
    - Kiểm tra đơn hàng dở dang: ném `BusinessRuleException` chặn thao tác nếu có bất kỳ sản phẩm nào trong cây con đang nằm trong đơn hàng chưa hoàn thành (`PENDING`, `CONFIRMED`, `PROCESSING`, `SHIPPING`).
    - Cascade khóa: gọi `categoryRepository.updateStatusByIds` chuyển toàn bộ danh mục con sang `LOCKED`, và `productRepository.updateStatusByIds` chuyển toàn bộ sản phẩm thuộc cây con sang `INACTIVE`.
    - Tự động xóa chỉ mục tìm kiếm trong RAM (`SearchIndexInvalidationEvent.Operation.DELETE`) cho toàn bộ danh mục và sản phẩm bị khóa.
    - Ghi log kiểm toán `CATEGORY_LOCKED` lưu vết số lượng con và sản phẩm bị cascade.
  - **Chuẩn hóa Ràng buộc Email Manager (`CreateManagerRequest`)**:
    - Bổ sung regex chặt chẽ `^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$` chặn triệt để email thiếu tên miền / TLD (như `managertest@gmai`), trả về lỗi 400 Bad Request rõ ràng.
  - **Kiểm thử Toàn diện**:
    - Bổ sung 6 unit test cases mới: `CategoryServiceImplTest` (4 tests), `ManagerCategoryControllerTest` (2 tests), `UserControllerTest` (2 tests).
    - Toàn bộ **413/413 tests pass 100%**.
    - Đồng bộ `api-spec.md` Section 9.4.
- ✅ **Phase 5 (SystemSetting & SystemLog) — Quản trị Cấu hình Hệ thống & Nhật ký Audit Trail Toàn diện completed**:
  - **Quản trị Cấu hình Động (`SystemSetting`)**:
    - Repository bổ sung `findAllByIsDeletedFalseOrderBySettingKeyAsc`.
    - `SystemSettingService` & `SystemSettingServiceImpl`:
      - `listSettings()` trả danh sách `SettingResponse` (key, value, dataType, description, updatedAt).
      - `updateSetting(actorId, key, value)` kiểm tra kiểu dữ liệu (`INT`, `BOOLEAN`, `DECIMAL`, `STRING`), bảo đảm cửa sổ thời gian hủy/hoàn tác hủy luôn là số nguyên dương, ghi nhận `updatedBy`, và kích hoạt audit trail `SYSTEM_SETTING_UPDATED` cùng giao dịch.
    - `AdminSettingController`: cung cấp `GET /api/v1/admin/settings` và `PUT /api/v1/admin/settings/{key}`.
    - `DataInitializer`: seed quyền `SYSTEM_SETTING_VIEW` (`GET /api/v1/admin/settings`) và `SYSTEM_SETTING_UPDATE` (`PUT /api/v1/admin/settings/{key}`) cho vai trò `ADMIN`.
  - **Thực thể, Repository & Công cụ An toàn Nhật ký (`SystemLog`)**:
    - Thực thể `SystemLog` mapping bảng `system_logs` (khóa chính tự tăng `log_id`, `user_id` FK nullable, `action`, `entity_type`, `entity_id`, `description`, `old_value`, `new_value`, `created_at`, `is_deleted`). Index tối ưu `(user_id, created_at)`, `(entity_type, entity_id)`, `(action)`.
    - `AuditValueUtils`: tự động loại bỏ các trường nhạy cảm (password, token, otp, secret, authorization, credential), che số điện thoại (`09*****123`), che email (`c***@example.com`), cắt ngắn độ dài chuỗi tối đa tránh phình to DB.
    - `SystemLogSpecification`: lọc động đa tiêu chí (`role` của actor, `activity`, khoảng ngày `fromDate`..`toDate` qua `DateUtils`, tìm tên người thực hiện không dấu qua `VietnameseStringUtils`, SĐT, email), sắp xếp mặc định mới nhất lên đầu.
    - `SystemLogService` & `SystemLogServiceImpl`:
      - `log` và `logWithChange`: tham gia cùng giao dịch nghiệp vụ (`REQUIRED`), cam kết tính toàn vẹn 100% của audit trail (thao tác và log cùng commit hoặc rollback).
      - `logSecurityEvent`: mở giao dịch riêng (`REQUIRES_NEW`), nuốt lỗi khi DB sự cố nhằm bảo đảm ghi nhận các sự kiện bảo mật ngoại luồng (tài khoản khóa cố đăng nhập, replay attack) mà không làm vỡ logic người gọi.
      - `search(filter, page, size)` và `getById(logId)`.
    - `AdminSystemLogController`: cung cấp `GET /api/v1/admin/system-logs` (phân trang chuẩn `PageResponse`) và `GET /api/v1/admin/system-logs/{logId}`.
    - `DataInitializer`: cập nhật đường dẫn quyền `SYSTEM_LOG_VIEW` thành `/api/v1/admin/system-logs/**`.
  - **Gắn Dấu vết Kiểm toán (Audit Trail Wiring) Toàn diện trên Toàn bộ Service Quản trị & Nhân viên**:
    - `CategoryServiceImpl`: `CATEGORY_CREATED` (root/child), `CATEGORY_UPDATED`, `CATEGORY_LOCKED`, `CATEGORY_UNLOCKED`, `CATEGORY_DELETED` (rỗng & cascade force=true).
    - `ProductServiceImpl`: `PRODUCT_CREATED`, `PRODUCT_UPDATED`, `PRODUCT_LOCKED`, `PRODUCT_UNLOCKED`, `PRODUCT_DELETED`, `PRODUCT_IMAGES_UPLOADED`, `PRODUCT_IMAGES_DELETED`, `VARIANT_ADDED`, `VARIANT_UPDATED`, `VARIANT_DELETED`, `VARIANT_LOCKED`, `VARIANT_UNLOCKED`.
    - `BrandServiceImpl`, `UnitServiceImpl`, `ColorServiceImpl`: Đầy đủ `*_CREATED`, `*_UPDATED`, `*_LOCKED`, `*_UNLOCKED`, `*_DELETED`.
    - `ManagerOrderServiceImpl`: `ORDER_PRICE_CALCULATED` (old/new total và discount), `ORDER_CONFIRMED`, `ORDER_STATUS_UPDATED`, `ORDER_CANCELLED_BY_STAFF`, `ORDER_CANCEL_UNDONE_BY_STAFF`.
    - `UserProfileServiceImpl`: `USER_PHONE_CHANGED` (khi đổi SĐT thành công qua OTP, số điện thoại được che).
    - `AuthServiceImpl` & `RefreshTokenServiceImpl`: `CUSTOMER_REGISTERED` (đăng ký mới qua Google), `LOGIN_BLOCKED_LOCKED_ACCOUNT` (chặn đăng nhập tài khoản khóa), `REFRESH_TOKEN_REUSE_DETECTED` (phát hiện tái sử dụng refresh token).
  - **Kiểm thử Toàn diện & Đồng bộ**:
    - Thêm 23 unit tests mới: `SystemSettingServiceImplTest` (4 tests mới), `AdminSettingControllerTest` (3 tests), `SystemLogServiceImplTest` (7 tests), `AdminSystemLogControllerTest` (2 tests), `AuditValueUtilsTest` (5 tests), cập nhật mock cho toàn bộ các test service liên quan.
    - Toàn bộ **403/403 tests pass 100%**.
    - Đồng bộ `api-spec.md` (mục 15 & 16), `setting/CONTEXT.md`, `systemlog/CONTEXT.md`.
- ✅ **Tính năng Hoàn tác Hủy Đơn hàng (`undo-cancel`) cho cả 3 vai trò (Customer, Manager, Admin) completed**:
  - **Nghiệp vụ & Phân quyền Hoàn tác Tách biệt Chặt chẽ**:
    - Áp dụng khi người dùng lỡ tay hủy đơn hàng, cho phép hoàn tác đưa đơn về đúng trạng thái trước khi hủy tra cứu từ bản ghi [`OrderStatusHistory`](file:///d:/HMP/sales-backend/src/main/java/com/hmp/sales/feature/order/entity/OrderStatusHistory.java) gần nhất.
    - Đơn do khách hàng tự hủy (`cancelledBy == customer`): chỉ khách hàng mới được phép hoàn tác (trong cửa sổ 60 phút). Quản lý/Admin bị từ chối nếu can thiệp vào đơn khách đã hủy.
    - Đơn do Quản lý/Admin hủy (`cancelledBy != customer`): chỉ Quản lý/Admin mới được phép hoàn tác. Khách hàng không thể hoàn tác đơn do Quản lý/Admin hủy. Quản lý/Admin hoàn tác không bị giới hạn thời gian 60 phút và có thể khôi phục về mọi trạng thái trước đó (`PENDING`, `CONFIRMED`, `PROCESSING`, `SHIPPING`).
    - Bảo toàn mốc thời gian: Khi hoàn tác về `CONFIRMED`, giữ nguyên mốc `confirmedAt` cũ (không reset). Nếu cửa sổ hủy của đơn đã hết, `OrderAutoTransitionScheduler` tự động quét và chuyển sang `PROCESSING`.
    - Dọn dẹp dấu vết hủy: Xóa `cancelledBy`, `cancelledAt`, `cancellationReason` về `null`; lưu vết lịch sử `OrderStatusHistory` (`fromStatus = CANCELLED`, `toStatus = previousStatus`).
  - **Cấu hình Động Cửa sổ Hoàn tác (`SystemSetting`) & `AppProperties`**:
    - Khởi tạo key [`ORDER_UNDO_CANCEL_WINDOW_MINUTES = "60"`](file:///d:/HMP/sales-backend/src/main/java/com/hmp/sales/feature/setting/service/SystemSettingService.java) trong PostgreSQL qua `DataInitializer`, fallback về `app.order.undo-cancel-window-minutes: 60` trong `application.yaml` qua [`AppProperties`](file:///d:/HMP/sales-backend/src/main/java/com/hmp/sales/config/AppProperties.java).
  - **REST Endpoints Không Request Body**:
    - Khách hàng: `POST /api/v1/orders/{orderId}/undo-cancel` ([`CustomerOrderController`](file:///d:/HMP/sales-backend/src/main/java/com/hmp/sales/feature/order/controller/CustomerOrderController.java)).
    - Quản lý/Admin: `POST /api/v1/manager/orders/{orderId}/undo-cancel` ([`ManagerOrderController`](file:///d:/HMP/sales-backend/src/main/java/com/hmp/sales/feature/order/controller/ManagerOrderController.java)).
    - Cấp quyền động `ORDER_UNDO_CANCEL_OWN` và `ORDER_UNDO_CANCEL_ANY` trong `DataInitializer`, nạp lại RAM cache.
  - **Hệ thống Thông báo Khôi phục Đơn hàng (`ORDER_RESTORED`)**:
    - Mở rộng enum [`NotificationType`](file:///d:/HMP/sales-backend/src/main/java/com/hmp/sales/feature/notification/entity/NotificationType.java) với loại `ORDER_RESTORED`.
    - Cập nhật [`NotificationServiceImpl`](file:///d:/HMP/sales-backend/src/main/java/com/hmp/sales/feature/notification/service/impl/NotificationServiceImpl.java): khi nhận sự kiện chuyển đổi từ `fromStatus = CANCELLED`, tự động tạo thông báo in-app cho khách hàng chủ đơn với tiêu đề *"Đơn hàng đã được khôi phục"*.
    - Thiết kế mẫu HTML email [`order-restored.html`](file:///d:/HMP/sales-backend/src/main/resources/templates/email/order-restored.html) và tích hợp vào [`SendGridEmailNotificationProvider`](file:///d:/HMP/sales-backend/src/main/java/com/hmp/sales/feature/notification/external/SendGridEmailNotificationProvider.java) cùng `application.yaml`.
  - **Kiểm thử Toàn diện**:
    - Bổ sung 23 unit test cases mới: `SystemSettingServiceImplTest` (3 tests), `NotificationServiceImplTest` (1 test), `SendGridEmailNotificationProviderTest` (1 test), `CustomerOrderServiceImplTest` (8 tests), `ManagerOrderServiceImplTest` (6 tests), `CustomerOrderControllerTest` (2 tests), `ManagerOrderControllerTest` (2 tests).
    - Toàn bộ **380/380 tests pass 100%**.
- ✅ **Bổ sung Thông báo Đặt hàng Thành công (`ORDER_CREATED`) completed**:
  - Mở rộng enum [`NotificationType`](file:///d:/HMP/sales-backend/src/main/java/com/hmp/sales/feature/notification/entity/NotificationType.java) với loại `ORDER_CREATED`.
  - Cập nhật [`NotificationServiceImpl`](file:///d:/HMP/sales-backend/src/main/java/com/hmp/sales/feature/notification/service/impl/NotificationServiceImpl.java): khi nhận `OrderStatusChangedEvent` với trạng thái `toStatus = OrderStatus.PENDING` (lúc khách hàng tạo đơn thành công), hệ thống tự động sinh thông báo in-app và gửi thông báo ngoại vi với tiêu đề *"Đặt hàng thành công"* và nội dung *"Đơn hàng #<orderId> đã được tạo thành công và đang chờ Quản lý xác nhận giá."*.
  - Bổ sung mẫu HTML email [`order-created.html`](file:///d:/HMP/sales-backend/src/main/resources/templates/email/order-created.html) trong `src/main/resources/templates/email/` và cấu hình template tương ứng trong [`SendGridEmailNotificationProvider`](file:///d:/HMP/sales-backend/src/main/java/com/hmp/sales/feature/notification/external/SendGridEmailNotificationProvider.java) cùng `application.yaml`.
  - Thêm unit tests cho `NotificationServiceImplTest` và `SendGridEmailNotificationProviderTest`.
  - Toàn bộ **357/357 tests pass 100%**.
- ✅ **Phase 5 (Task 4) — Nhà cung cấp thông báo ngoại vi Zalo ZNS & SendGrid Email (External Notification Providers) completed**:
  - **Tích hợp Thư viện & Cấu hình**:
    - Bổ sung `com.sendgrid:sendgrid-java:4.10.3` và `spring-boot-starter-thymeleaf` vào `pom.xml`.
    - Mở rộng [`AppProperties`](file:///d:/HMP/sales-backend/src/main/java/com/hmp/sales/config/AppProperties.java) và `application.yaml` với nhánh cấu hình `app.notification.zalo` và `app.notification.email` (bảo mật toàn bộ API keys/tokens qua biến môi trường, mặc định `enabled: false`).
  - **Triển khai [`ZaloNotificationProvider`](file:///d:/HMP/sales-backend/src/main/java/com/hmp/sales/feature/notification/external/ZaloNotificationProvider.java)**:
    - Sử dụng `RestClient` (chuẩn Spring Boot 4.x) để gọi Zalo OpenAPI ZNS `POST /message/template`.
    - Chuẩn hóa đầu số quốc tế Việt Nam (`84...`), mapping linh hoạt theo từng loại thông báo [`NotificationType`](file:///d:/HMP/sales-backend/src/main/java/com/hmp/sales/feature/notification/entity/NotificationType.java).
    - Che dấu số điện thoại khi ghi log; kích hoạt linh hoạt qua `@ConditionalOnProperty(name = "app.notification.zalo.enabled", havingValue = "true")`.
  - **Triển khai [`SendGridEmailNotificationProvider`](file:///d:/HMP/sales-backend/src/main/java/com/hmp/sales/feature/notification/external/SendGridEmailNotificationProvider.java)**:
    - Tích hợp SendGrid Java SDK gửi email transactional có độ tin cậy cao.
    - Hỗ trợ đồng thời 2 định dạng: HTML trực quan hiện đại và plain text dự phòng.
    - Thiết kế 5 mẫu HTML email responsive chuyên nghiệp tại `src/main/resources/templates/email/`: `order-confirmed.html`, `order-processing.html`, `order-shipping.html`, `order-completed.html`, `order-cancelled.html`.
    - Che dấu email khi ghi log; kích hoạt qua `@ConditionalOnProperty(name = "app.notification.email.enabled", havingValue = "true")`.
  - **Kiểm thử Đơn vị Toàn diện (Unit Tests)**:
    - [`ZaloNotificationProviderTest`](file:///d:/HMP/sales-backend/src/test/java/com/hmp/sales/feature/notification/external/ZaloNotificationProviderTest.java) (6 tests): Kiểm thử kịch bản gửi thành công, người dùng không tồn tại, người dùng thiếu SĐT, chưa cấu hình template, Zalo API trả mã lỗi, và lỗi mạng/HTTP.
    - [`SendGridEmailNotificationProviderTest`](file:///d:/HMP/sales-backend/src/test/java/com/hmp/sales/feature/notification/external/SendGridEmailNotificationProviderTest.java) (5 tests): Kiểm thử kịch bản gửi thành công (202), người dùng không tồn tại, người dùng thiếu email, SendGrid trả mã lỗi HTTP (401), và lỗi I/O.
    - Toàn bộ **355/355 tests pass 100%**.
- ✅ **Phase 5 (Tasks 1–3) — Hệ thống Thông báo (Notification System) completed**:
  - **Mô hình Dữ liệu & Thực thể JPA (`com.hmp.sales.feature.notification.entity`)**:
    - Thực thể [`Notification`](file:///d:/HMP/sales-backend/src/main/java/com/hmp/sales/feature/notification/entity/Notification.java) (bảng `notifications` trong PostgreSQL): Quản lý người nhận (`user_id`), loại thông báo [`NotificationType`](file:///d:/HMP/sales-backend/src/main/java/com/hmp/sales/feature/notification/entity/NotificationType.java) (`ORDER_CONFIRMED`, `ORDER_PROCESSING`, `ORDER_SHIPPING`, `ORDER_COMPLETED`, `ORDER_CANCELLED`), tiêu đề `title`, nội dung `message`, liên kết thực thể (`related_entity_type = "ORDER"`, `related_entity_id = orderId`), trạng thái đọc `is_read`, thời điểm đọc `read_at`, thời gian tạo `created_at`, và cờ xóa mềm `is_deleted`.
    - [`NotificationRepository`](file:///d:/HMP/sales-backend/src/main/java/com/hmp/sales/feature/notification/repository/NotificationRepository.java): Truy vấn phân trang `findByUser_IdAndIsDeletedFalseOrderByCreatedAtDesc`, tra cứu theo `notificationId` & `userId`, đếm thông báo chưa đọc `countByUser_IdAndIsReadFalseAndIsDeletedFalse`, và cập nhật hàng loạt `@Modifying markAllAsRead`.
  - **Kiến trúc Sự kiện Nội bộ (Event-Driven) & Tách biệt Trách nhiệm**:
    - Định nghĩa Spring Application Event [`OrderStatusChangedEvent`](file:///d:/HMP/sales-backend/src/main/java/com/hmp/sales/feature/notification/event/OrderStatusChangedEvent.java) (orderId, customerId, fromStatus, toStatus, changedBy).
    - Tích hợp phát sinh sự kiện `eventPublisher.publishEvent` tại các điểm chuyển trạng thái đơn hàng:
      - `confirmOrder()`, `updateOrderStatus()`, `cancelOrder()` trong [`ManagerOrderServiceImpl`](file:///d:/HMP/sales-backend/src/main/java/com/hmp/sales/feature/order/service/impl/ManagerOrderServiceImpl.java).
      - `cancelOwnOrder()` trong [`CustomerOrderServiceImpl`](file:///d:/HMP/sales-backend/src/main/java/com/hmp/sales/feature/order/service/impl/CustomerOrderServiceImpl.java).
      - Tác vụ tự động hết hạn hủy trong [`OrderAutoTransitionScheduler`](file:///d:/HMP/sales-backend/src/main/java/com/hmp/sales/feature/order/scheduler/OrderAutoTransitionScheduler.java).
    - Xây dựng [`NotificationServiceImpl.onOrderStatusChanged`](file:///d:/HMP/sales-backend/src/main/java/com/hmp/sales/feature/notification/service/impl/NotificationServiceImpl.java) lắng nghe sự kiện với `@TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)` và `@Transactional(propagation = Propagation.REQUIRES_NEW)`. Đảm bảo đơn hàng commit thành công mới lưu thông báo; lỗi tạo thông báo không ảnh hưởng hay làm rollback đơn hàng.
  - **Tầng Trừu tượng Thông báo Ngoại vi (External Provider Abstraction)**:
    - Interface [`ExternalNotificationProvider`](file:///d:/HMP/sales-backend/src/main/java/com/hmp/sales/feature/notification/external/ExternalNotificationProvider.java) và lớp triển khai mặc định [`NoOpExternalNotificationProvider`](file:///d:/HMP/sales-backend/src/main/java/com/hmp/sales/feature/notification/external/NoOpExternalNotificationProvider.java) cho V1 (ghi log, sẵn sàng cắm Zalo ZNS / Email / SMS ở V2).
  - **REST Controller & Endpoint Quản lý Thông báo**:
    - [`NotificationController`](file:///d:/HMP/sales-backend/src/main/java/com/hmp/sales/feature/notification/controller/NotificationController.java) cung cấp 4 API RESTful:
      1. `GET /api/v1/notifications`: Danh sách thông báo phân trang chuẩn `PageResponse<NotificationResponse>`.
      2. `GET /api/v1/notifications/unread-count`: Lấy số lượng thông báo chưa đọc cho badge số đỏ (`UnreadCountResponse`).
      3. `POST /api/v1/notifications/{notificationId}/read`: Đánh dấu 1 thông báo cụ thể là đã đọc.
      4. `POST /api/v1/notifications/read-all`: Đánh dấu toàn bộ thông báo là đã đọc (`MarkAllReadResponse`).
    - Trích xuất `userId` an toàn từ JWT Authentication qua `SecurityUtils.extractUserId()`.
  - **Phân quyền Động trong `DataInitializer` & Đồng bộ RAM Cache**:
    - Khởi tạo các quyền: `NOTIFICATION_VIEW_OWN`, `NOTIFICATION_MARK_READ`, `NOTIFICATION_MARK_ALL_READ`.
    - Gán quyền cho các vai trò `CUSTOMER`, `MANAGER`, và `ADMIN`.
  - **Đồng bộ Tài liệu & Unit Tests**:
    - Tạo `CONTEXT.md` cho feature `notification`.
    - Đồng bộ `api-spec.md` Section 17.
    - Viết 17 test cases mới: `NotificationServiceImplTest` (12 tests) và `NotificationControllerTest` (5 tests).
    - Toàn bộ **344/344 tests pass 100%**.
- ✅ **Phase 4 (Step 3) — Xử lý Đơn hàng Quản lý, Tính giá & Chiết khấu Linh hoạt, Tách biệt Confirm & Hủy đơn SHIPPING completed**:
  - **Mở rộng Mô hình Thực thể & Schema Database (`orders`, `order_items`)**:
    - [`OrderItem`](file:///d:/HMP/sales-backend/src/main/java/com/hmp/sales/feature/order/entity/OrderItem.java): Bổ sung cột `discount_amount` (`BIGINT NOT NULL DEFAULT 0`) lưu trữ số tiền giảm giá cho từng dòng sản phẩm.
    - [`Order`](file:///d:/HMP/sales-backend/src/main/java/com/hmp/sales/feature/order/entity/Order.java): Bổ sung cột `subtotal_amount` (`BIGINT`) lưu tổng tiền hàng trước giảm giá đơn và `discount_amount` (`BIGINT NOT NULL DEFAULT 0`) lưu số tiền giảm giá chung cho toàn bộ đơn hàng. Tổng tiền thanh toán: `totalAmount = max(0, subtotalAmount - discountAmount)`.
    - Đồng bộ DTOs: [`OrderItemResponse`](file:///d:/HMP/sales-backend/src/main/java/com/hmp/sales/feature/order/dto/OrderItemResponse.java) và [`OrderDetailResponse`](file:///d:/HMP/sales-backend/src/main/java/com/hmp/sales/feature/order/dto/OrderDetailResponse.java).
  - **Cơ chế Tính giá & Giảm giá Đa tầng Linh hoạt (Pricing & Discount)**:
    - Quản lý có thể tùy chọn:
      1. Giảm giá theo đơn giá: Nhập `unitPrice` mới cho từng item. Nếu không truyền, hệ thống tự động tra cứu từ bảng giá niêm yết `ProductPrice` theo `(variantId, unitId)`.
      2. Giảm giá theo số tiền: Nhập `discountAmount` cho từng dòng hoặc cho toàn bộ đơn hàng.
      3. Kết hợp cả hai: Vừa giảm đơn giá, vừa giảm thêm số tiền thành tiền của dòng hoặc của cả đơn.
    - Công thức dòng: $\text{TotalPrice} = \max(0, [\text{UnitPrice} \times (\text{Size} \neq \text{null} \ ?\ \text{Size} : 1) \times \text{Quantity}] - \text{DiscountAmount})$.
  - **Tách biệt Hoàn toàn Tính giá (`calculate`) & Xác nhận đơn (`confirm`)**:
    - `POST /api/v1/manager/orders/{orderId}/calculate`: Tính toán và cập nhật số tiền, **giữ nguyên trạng thái `PENDING`** (Quản lý có thể tính lại nhiều lần khi thỏa thuận giá thay đổi).
    - `POST /api/v1/manager/orders/{orderId}/confirm`: Hành động riêng biệt để chốt giá và chuyển sang `CONFIRMED`. Bắt buộc đơn phải có `totalAmount != null` mới được confirm. Khi confirm mới bắt đầu ghi nhận `confirmedAt` và kích hoạt cửa sổ hủy của khách hàng.
  - **Mở rộng Quyền Hủy Đơn & Quản lý Tiến trình Vòng đời**:
    - Cho phép Quản lý hủy đơn hàng ở tất cả các trạng thái trước khi hoàn tất: `PENDING`, `CONFIRMED`, `PROCESSING`, và **`SHIPPING`** (lý do hủy tùy chọn, ghi vết `OrderStatusHistory`).
    - Hỗ trợ cập nhật tiến trình đơn hàng tuần tự (`PATCH /api/v1/manager/orders/{orderId}/status`): `CONFIRMED -> PROCESSING -> SHIPPING -> COMPLETED`.
  - **Tra cứu, Tìm kiếm Tiếng Việt Không Dấu & Phân trang Chuẩn**:
    - Xây dựng [`OrderSpecification.forManager`](file:///d:/HMP/sales-backend/src/main/java/com/hmp/sales/feature/order/repository/OrderSpecification.java) hỗ trợ tìm kiếm tên khách hàng không dấu qua `VietnameseStringUtils`, tìm theo SĐT, email, lọc khoảng ngày, trạng thái.
    - Cung cấp danh sách phân trang chuẩn `PageResponse<ManagerOrderSummaryResponse>`.
  - **Dịch vụ & REST Controller Quản lý**:
    - Triển khai [`ManagerOrderService`](file:///d:/HMP/sales-backend/src/main/java/com/hmp/sales/feature/order/service/ManagerOrderService.java) và [`ManagerOrderServiceImpl`](file:///d:/HMP/sales-backend/src/main/java/com/hmp/sales/feature/order/service/impl/ManagerOrderServiceImpl.java).
    - Cung cấp 6 RESTful endpoints tại [`ManagerOrderController`](file:///d:/HMP/sales-backend/src/main/java/com/hmp/sales/feature/order/controller/ManagerOrderController.java) (`/api/v1/manager/orders/**`).
    - Cấu hình phân quyền động trong `DataInitializer`: `ORDER_VIEW_ALL`, `ORDER_CALCULATE`, `ORDER_CONFIRM`, `ORDER_CANCEL_ANY`, `ORDER_STATUS_UPDATE` cho `ROLE_MANAGER` và `ROLE_ADMIN`.
  - **Bổ sung Kiểm tra Toàn vẹn Tham số Tính giá & Tự động Tính giá theo Quyền hạn (`PRODUCT_PRICE_VIEW`)**:
    - Kiểm tra số lượng và danh sách `orderItemId` trong `CalculateOrderRequest.items`: nếu truyền `items`, bắt buộc số lượng item và ID phải khớp chính xác 100% với các dòng sản phẩm của đơn hàng, kiểm tra chống trùng lặp ID (tránh tính thiếu hoặc tính nhầm sang đơn hàng khác).
    - Tự động tính giá niêm yết khi tạo đơn (`createOrder`): Nếu khách hàng có quyền `PRODUCT_PRICE_VIEW`, hệ thống tự động tính giá niêm yết ngay khi đặt hàng, Quản lý không cần gọi API tính giá thủ công.
    - Kiểm soát hiển thị giá chặt chẽ (`getOwnOrderDetail`, `getOwnOrders`): Nếu khách hàng KHÔNG có quyền xem giá và đơn hàng đang ở trạng thái `PENDING`, hệ thống tự động mask/ẩn toàn bộ thông tin giá tiền (`null`), chỉ hiển thị giá khi đơn chuyển sang `CONFIRMED` trở đi. Nếu khách hàng có quyền xem giá, luôn xem được giá ở mọi trạng thái.
  - Toàn bộ **327/327 tests pass 100%** (bổ sung thêm 6 unit test cases mới kiểm thử toàn vẹn tham số tính giá và kiểm soát hiển thị giá tiền).
- ✅ **Bổ sung Tiện ích Xác thực Khoảng Thời gian (`DateUtils`)**:
  - Tạo lớp tiện ích [`DateUtils`](file:///d:/HMP/sales-backend/src/main/java/com/hmp/sales/common/util/DateUtils.java) trong package `com.hmp.sales.common.util` (tuân thủ quy chuẩn đặt tên `*Utils`).
  - Cung cấp các phương thức `isValidDateRange(fromDate, toDate)` và `validateDateRange(fromDate, toDate)` hỗ trợ đa dạng kiểu dữ liệu: `LocalDate`, `LocalDateTime`, `Instant`.
  - Kiểm tra điều kiện `fromDate <= toDate`. Nếu một trong hai giá trị là `null`, coi như không bị chặn biên (hợp lệ). Nếu `fromDate > toDate`, `validateDateRange` ném `BusinessRuleException` với thông điệp tiếng Việt thân thiện, rõ ràng.
  - Tích hợp vào [`CustomerOrderServiceImpl.getOwnOrders`](file:///d:/HMP/sales-backend/src/main/java/com/hmp/sales/feature/order/service/impl/CustomerOrderServiceImpl.java) để xác thực khoảng lọc ngày trước khi truy vấn cơ sở dữ liệu.
  - Viết bộ test đầy đủ tại [`DateUtilsTest`](file:///d:/HMP/sales-backend/src/test/java/com/hmp/sales/common/util/DateUtilsTest.java) và bổ sung test case tại [`CustomerOrderServiceImplTest`](file:///d:/HMP/sales-backend/src/test/java/com/hmp/sales/feature/order/service/CustomerOrderServiceImplTest.java).
  - Toàn bộ **295/295 tests pass 100%**.
- ✅ **Phase 4 (Step 2) — Quản lý Đơn hàng Khách hàng, Cấu hình Động Cửa sổ Hủy Đơn & Chuẩn hóa `colorId`, `unitId` completed**:
  - **Chuẩn hóa Định danh Màu sắc & Đơn vị tính trong `OrderItemRequest`**:
    - Thay thế trường văn bản thuần `color`, `unit` thành ID số nguyên dương [`colorId`, `unitId`](file:///d:/HMP/sales-backend/src/main/java/com/hmp/sales/feature/order/dto/OrderItemRequest.java) (`@Positive`).
    - Bổ sung truy vấn JPQL tối ưu: [`ProductColorRepository.existsByVariantIdAndColorId`](file:///d:/HMP/sales-backend/src/main/java/com/hmp/sales/feature/product/repository/ProductColorRepository.java) và [`ProductPriceRepository.existsByVariantIdAndUnitId`](file:///d:/HMP/sales-backend/src/main/java/com/hmp/sales/feature/product/repository/ProductPriceRepository.java).
    - Cập nhật logic `createOrder` trong [`CustomerOrderServiceImpl`](file:///d:/HMP/sales-backend/src/main/java/com/hmp/sales/feature/order/service/impl/CustomerOrderServiceImpl.java): kiểm tra chặt chẽ màu sắc và đơn vị tính tồn tại trong DB, ở trạng thái `ACTIVE`, và bắt buộc phải nằm trong danh mục màu sắc / bảng giá của biến thể đã chọn; snapshot tên và liên kết entity `Unit`. Ném `BusinessRuleException` nếu chọn màu/đơn vị tính khi chưa có biến thể hoặc không thuộc biến thể.
  - **Mô hình Dữ liệu & Thực thể JPA (`com.hmp.sales.feature.order.entity`)**:
    - [`Order`](file:///d:/HMP/sales-backend/src/main/java/com/hmp/sales/feature/order/entity/Order.java) (bảng `orders`): Lưu trữ trực tiếp trong PostgreSQL (Source of Truth). Chứa trạng thái (`OrderStatus`), tổng tiền `totalAmount`, snapshot bất biến người nhận (`receiverName`, `receiverPhone`, `shippingAddress`, `note`), các mốc thời gian (`confirmedAt`, `cancelledAt`, `completedAt`), và lý do hủy tùy chọn `cancellationReason`.
    - [`OrderItem`](file:///d:/HMP/sales-backend/src/main/java/com/hmp/sales/feature/order/entity/OrderItem.java) (bảng `order_items`): Lưu chi tiết dòng sản phẩm, số lượng `quantity`, chiều dài tôn sóng `size`, snapshot bất biến tên sản phẩm (`productNameSnapshot`), tên biến thể (`variantNameSnapshot`), đơn vị tính (`unitNameSnapshot`), màu sắc (`colorSnapshot`), đơn giá snapshot `unitPrice`, và thành tiền `totalPrice`. Phản hồi DTO [`OrderItemResponse`](file:///d:/HMP/sales-backend/src/main/java/com/hmp/sales/feature/order/dto/OrderItemResponse.java) chứa đầy đủ `variantId` và `variantName`.
    - [`OrderStatusHistory`](file:///d:/HMP/sales-backend/src/main/java/com/hmp/sales/feature/order/entity/OrderStatusHistory.java) (bảng `order_status_histories`): Lưu vết toàn bộ tiến trình chuyển trạng thái (`fromStatus`, `toStatus`, `changedBy`, `reason`, `createdAt`).
  - **Quản trị Động Cửa sổ Hủy Đơn (`SystemSetting`)**:
    - Triển khai thực thể [`SystemSetting`](file:///d:/HMP/sales-backend/src/main/java/com/hmp/sales/feature/setting/entity/SystemSetting.java), [`SystemSettingRepository`](file:///d:/HMP/sales-backend/src/main/java/com/hmp/sales/feature/setting/repository/SystemSettingRepository.java), [`SystemSettingService`](file:///d:/HMP/sales-backend/src/main/java/com/hmp/sales/feature/setting/service/SystemSettingService.java).
    - Tra cứu động tham số `ORDER_CUSTOMER_CANCELLATION_WINDOW_MINUTES` từ PostgreSQL, tự động fallback về `app.order.cancellation-window-minutes: 30` trong `application.yaml` nếu chưa có bản ghi DB.
  - **Tự động chuyển trạng thái nền ([`OrderAutoTransitionScheduler`](file:///d:/HMP/sales-backend/src/main/java/com/hmp/sales/feature/order/scheduler/OrderAutoTransitionScheduler.java))**:
    - Background task `@Scheduled(fixedDelay = 60000)` định kỳ quét các đơn hàng `CONFIRMED` có `confirmedAt + cancellationWindowMinutes <= now()`.
    - Tự động chuyển trạng thái sang `PROCESSING` (bắt đầu gia công/chuẩn bị giao) và ghi log vào `OrderStatusHistory` với `changedBy = null` (Hệ thống).
  - **Dịch vụ & Nghiệp vụ Khách hàng ([`CustomerOrderService`](file:///d:/HMP/sales-backend/src/main/java/com/hmp/sales/feature/order/service/CustomerOrderService.java))**:
    - `POST /api/v1/orders`: Đặt hàng mới trong `@Transactional` nguyên tử. Kiểm tra bảo chứng OTP số điện thoại nhận hàng (`otp:verified:{phone}` trên Redis hoặc trùng với SĐT đã lưu trong tài khoản User). Kiểm tra sản phẩm phải đang `ACTIVE`, biến thể hợp lệ, số lượng & chiều dài $> 0$. Khởi tạo trạng thái ban đầu `PENDING`.
    - `GET /api/v1/orders`: Danh sách đơn hàng phân trang chuẩn `PageResponse<OrderSummaryResponse>` hỗ trợ lọc theo khoảng ngày (`dateFrom`, `dateTo`), trạng thái (`status`).
    - `GET /api/v1/orders/{orderId}`: Chi tiết đơn hàng của chính mình (chặn xem đơn của người khác).
    - `POST /api/v1/orders/{orderId}/cancel`: Hủy đơn hàng trong hạn cho phép (lấy thời hạn động từ `SystemSettingService`), lý do hủy tùy chọn, chuyển sang `CANCELLED` và ghi log `OrderStatusHistory`.
  - **REST Controller ([`CustomerOrderController`](file:///d:/HMP/sales-backend/src/main/java/com/hmp/sales/feature/order/controller/CustomerOrderController.java)) & Phân quyền động**:
    - Cung cấp 4 endpoint RESTful theo chuẩn `api-spec.md` Section 8. Khách hàng chỉ cần vào xem giá chi tiết, không cần duyệt đơn thủ công.
    - Cấp quyền `ORDER_CREATE`, `ORDER_VIEW_OWN`, `ORDER_CANCEL_OWN` trong `DataInitializer`, nạp lại RAM cache `PermissionAuthorizationManager`.
    - Seed tham số mặc định `ORDER_CUSTOMER_CANCELLATION_WINDOW_MINUTES = "30"` vào `SystemSetting`.
    - Khôi phục cơ chế `resolveResourceName` trong `GlobalExceptionHandler` hỗ trợ `orderId` trả về 404 nhất quán khi ép kiểu sai.
    - Bổ sung `@ExceptionHandler(HttpMessageNotReadableException.class)` trong [`GlobalExceptionHandler`](file:///d:/HMP/sales-backend/src/main/java/com/hmp/sales/common/exception/GlobalExceptionHandler.java): Bắt lỗi parse Request Body/JSON, phân giải Jackson 3 `InvalidFormatException` để trả về lỗi 400 Bad Request rõ ràng (ví dụ: `items[0].colorId` truyền chuỗi thay vì số) thay vì rơi vào lỗi 500 chung.
    - Tạo và đồng bộ tệp tài liệu ngữ cảnh [`CONTEXT.md`](file:///d:/HMP/sales-backend/src/main/java/com/hmp/sales/feature/order/CONTEXT.md) và [`setting/CONTEXT.md`](file:///d:/HMP/sales-backend/src/main/java/com/hmp/sales/feature/setting/CONTEXT.md).
    - Toàn bộ **289/289 tests pass 100%** (bổ sung 10 test cases mới: kiểm thử xác thực biến thể, màu sắc, đơn vị tính và bắt lỗi định dạng Request Body).
- ✅ **Chuẩn hóa Phân trang Category Products (`products: { result, meta }`)**:
  - **DTO [`CategoryProductsResponse`](file:///d:/HMP/sales-backend/src/main/java/com/hmp/sales/feature/category/dto/CategoryProductsResponse.java)**:
    - Chuyển đổi thuộc tính `products` từ danh sách phẳng `List<ProductSummaryResponse>` sang đối tượng phân trang chuẩn `PageResponse<ProductSummaryResponse>` chứa `result` và `meta` (`page`, `size`, `totalElements`, `totalPages`).
  - **Data Access & Service Layer**:
    - [`ProductRepository`](file:///d:/HMP/sales-backend/src/main/java/com/hmp/sales/feature/product/repository/ProductRepository.java): Bổ sung truy vấn phân trang `findByCategoryIdAndStatusAndIsDeletedFalse(Long, ProductStatus, Pageable)` và `findByCategoryIdAndIsDeletedFalse(Long, Pageable)`.
    - [`CategoryService`](file:///d:/HMP/sales-backend/src/main/java/com/hmp/sales/feature/category/service/CategoryService.java) & [`CategoryServiceImpl`](file:///d:/HMP/sales-backend/src/main/java/com/hmp/sales/feature/category/service/impl/CategoryServiceImpl.java): Mở rộng `getProductsByCategory` và `getAllProductsByCategory` tiếp nhận `Pageable` và trả về `PageResponse.from(productDtos)`.
  - **REST Controllers**:
    - [`PublicCategoryController`](file:///d:/HMP/sales-backend/src/main/java/com/hmp/sales/feature/category/controller/PublicCategoryController.java) (`GET /api/v1/categories/{categoryId}/products`): Tiếp nhận `@RequestParam(required = false) Integer page, Integer size`, phân giải động qua `PaginationUtils` và trả về danh sách phân trang.
    - [`ManagerCategoryController`](file:///d:/HMP/sales-backend/src/main/java/com/hmp/sales/feature/category/controller/ManagerCategoryController.java) (`GET /api/v1/manager/categories/{categoryId}/products`): Tiếp nhận `page`, `size` và trả về toàn bộ sản phẩm (kể cả INACTIVE) dạng phân trang.
  - **Đồng bộ Tài liệu & Unit Tests**:
    - Đồng bộ `api-spec.md` Section 4.3 và Section 9.7.
    - Cập nhật test cases: `PublicCategoryControllerTest`, `ManagerCategoryControllerTest`, `CategoryServiceImplTest`. Khôi phục xử lý `resolveResourceName` trong `GlobalExceptionHandler`.
    - Toàn bộ **255/255 tests pass 100%**.
- ✅ **Phase 4 (Step 1) — Customer Profile & Address Book completed**:
  - **Thực thể & Quản lý Sổ địa chỉ [`CustomerAddress`](file:///d:/HMP/sales-backend/src/main/java/com/hmp/sales/feature/user/entity/CustomerAddress.java)**:
    - Bảng `customer_addresses` lưu địa chỉ giao nhận hàng của khách hàng với các trường `address_id`, `customer_id`, `address_line`, `is_default`, `status` (`AddressStatus.ACTIVE`/`INACTIVE`), audit timestamps và cờ xóa mềm `is_deleted`. Đã tinh giản loại bỏ `receiver_name` và `receiver_phone` khỏi sổ địa chỉ và đồng bộ 100% với `api-spec.md` (thông tin người nhận sẽ được snapshot trực tiếp vào `Orders`).
    - [`CustomerAddressRepository`](file:///d:/HMP/sales-backend/src/main/java/com/hmp/sales/feature/user/repository/CustomerAddressRepository.java): Truy vấn danh sách địa chỉ chưa xóa mềm, sắp xếp theo mặc định (`isDefault DESC`) và thời gian tạo giảm dần.
    - Cơ chế độc quyền địa chỉ mặc định: Khi tạo mới hoặc cập nhật địa chỉ với `isDefault = true`, truy vấn `@Modifying resetDefaultAddress` tự động xóa cờ mặc định của các địa chỉ cũ cùng khách hàng. Nếu là địa chỉ đầu tiên, hệ thống tự động gán `isDefault = true`.
    - Cơ chế tự động kế thừa mặc định khi xóa: Khi khách hàng xóa mềm địa chỉ mặc định, địa chỉ còn lại mới nhất sẽ được tự động kích hoạt làm mặc định thay thế.
  - **Hồ sơ Cá nhân & Ràng buộc OTP [`UserProfileService`](file:///d:/HMP/sales-backend/src/main/java/com/hmp/sales/feature/user/service/UserProfileService.java)**:
    - `GET /api/v1/me`: Xem thông tin tài khoản cá nhân (họ tên, email, phone, status).
    - `PATCH /api/v1/me`: Cập nhật họ tên và số điện thoại. Bắt buộc kiểm tra bảo chứng xác thực OTP (`phoneOtpService.isVerified(phone)` trên Redis) khi thay đổi hoặc nhập số điện thoại mới. Giữ nguyên số cũ hoặc chỉ đổi họ tên thì không yêu cầu OTP.
  - **REST Controller [`MeController`](file:///d:/HMP/sales-backend/src/main/java/com/hmp/sales/feature/user/controller/MeController.java)**:
    - Cung cấp đầy đủ các endpoint theo chuẩn `api-spec.md` Section 6: `GET /api/v1/me`, `PATCH /api/v1/me`, `GET /api/v1/me/addresses`, `POST /api/v1/me/addresses` (201 Created), `PATCH /api/v1/me/addresses/{addressId}`, `DELETE /api/v1/me/addresses/{addressId}`.
    - Tích hợp trích xuất `userId` từ `Authentication` qua `SecurityUtils.extractUserId()`.
  - **Phân quyền động trong `DataInitializer` & RAM Cache**:
    - Khởi tạo và cấp các quyền `ME_PROFILE_VIEW`, `ME_PROFILE_UPDATE`, `ME_ADDRESS_VIEW`, `ME_ADDRESS_CREATE`, `ME_ADDRESS_UPDATE`, `ME_ADDRESS_DELETE` cho `CUSTOMER`, `ADMIN` và `MANAGER`.
  - **Sửa triệt để lệch `+ 1` trong [`PageResponse.from()`](file:///d:/HMP/sales-backend/src/main/java/com/hmp/sales/common/response/PageResponse.java)**: Đồng bộ `page.getNumber()` chuẩn 0-indexed theo đúng REST API spec.
  - Toàn bộ **254/254 tests pass 100%** (bổ sung 23 test cases mới: `UserProfileServiceImplTest`, `CustomerAddressServiceImplTest`, `MeControllerTest`).
- ✅ **Chuẩn hóa Phân trang chuẩn & Triệt tiêu Hardcode tham số `page`, `size`**:
  - **Sửa lỗi chỉ số trang `meta.page` trong [`PageResponse`](file:///d:/HMP/sales-backend/src/main/java/com/hmp/sales/common/response/PageResponse.java)**:
    - Loại bỏ lệch `+ 1` trong `PageResponse.from(Page<T> page)` để đồng bộ 0-indexed (`"page": 0`) chuẩn xác theo đúng hợp đồng REST API và `api-spec.md`.
    - Đảm bảo envelope trả về luôn thống nhất chuẩn:
      ```json
      {
        "statusCode": 200,
        "data": {
          "result": [ ... ],
          "meta": {
            "page": 0,
            "size": 20,
            "totalElements": N,
            "totalPages": M
          }
        },
        "message": "Thành công",
        "timestamp": "..."
      }
      ```
  - **Cơ chế nạp cấu hình phân trang động từ `application.yaml` qua [`PaginationUtils`](file:///d:/HMP/sales-backend/src/main/java/com/hmp/sales/common/util/PaginationUtils.java)**:
    - Xây dựng tiện ích [`PaginationUtils`](file:///d:/HMP/sales-backend/src/main/java/com/hmp/sales/common/util/PaginationUtils.java) giải quyết giá trị mặc định cho `page` và `size` từ `AppProperties` (`app.pagination.default-page`, `app.pagination.default-page-size`, `app.pagination.max-page-size`).
    - Cập nhật toàn bộ các REST Controller có chức năng danh sách phân trang:
      - [`PublicProductController`](file:///d:/HMP/sales-backend/src/main/java/com/hmp/sales/feature/product/controller/PublicProductController.java) (`GET /api/v1/products`)
      - [`AdminBrandController`](file:///d:/HMP/sales-backend/src/main/java/com/hmp/sales/feature/product/controller/AdminBrandController.java) (`GET /api/v1/admin/brands`)
      - [`AdminUnitController`](file:///d:/HMP/sales-backend/src/main/java/com/hmp/sales/feature/product/controller/AdminUnitController.java) (`GET /api/v1/admin/units`)
      - [`AdminColorController`](file:///d:/HMP/sales-backend/src/main/java/com/hmp/sales/feature/product/controller/AdminColorController.java) (`GET /api/v1/admin/colors`)
      - [`UserController`](file:///d:/HMP/sales-backend/src/main/java/com/hmp/sales/feature/user/controller/UserController.java) (`GET /api/v1/admin/users`)
    - Loại bỏ hoàn toàn việc hardcode chuỗi `"0"` và `"20"` trong `@RequestParam`, thay bằng `@RequestParam(required = false) Integer page, @RequestParam(required = false) Integer size`.
    - Bổ sung `UserControllerTest`, `PaginationUtilsTest` và mở rộng `AdminMasterDataControllerTest`, `PublicProductControllerTest` xác minh format `meta` và `result`.
  - Toàn bộ **231/231 tests pass** (thêm 12 test cases mới).
- ✅ **Kiến trúc Tìm kiếm Đa Tầng (Two-Tier Hybrid Search Architecture) — Chuẩn hóa tiếng Việt không dấu & Engine Gợi ý sửa lỗi**:
  - **Tầng Database (`xxx_name_unaccent`)**:
    - Bổ sung tiện ích [`VietnameseStringUtils`](file:///d:/HMP/sales-backend/src/main/java/com/hmp/sales/common/util/VietnameseStringUtils.java) chuyển đổi tiếng Việt có dấu thành chữ thường không dấu chuẩn xác (xử lý ký tự đặc thù `đ/Đ`, dấu tổ hợp `NFD`, khoảng trắng thừa) và thuật toán tính khoảng cách Levenshtein ($O(\min(M, N))$ space).
    - Mở rộng toàn bộ các thực thể có tính năng tìm kiếm: `Product` (`product_name_unaccent`), `Category` (`category_name_unaccent`), `Brand` (`brand_name_unaccent`), `Unit` (`unit_name_unaccent`), `Color` (`color_name_unaccent`), `User` (`full_name_unaccent`).
    - Tự động đồng bộ hóa cột unaccent qua JPA `@PrePersist` và `@PreUpdate`.
    - Cập nhật các câu truy vấn Specification và JPA Repository tìm kiếm so khớp LIKE trên cột `xxx_name_unaccent`. Người dùng tìm kiếm có dấu (`"tôn"`), không dấu (`"ton"`), sai nguyên âm (`"lọp"` thay vì `"lợp"`), hay hoa/thường (`"TON"`) đều tìm ra kết quả chính xác 100%.
  - **Tầng RAM/Redis Search Suggestion Engine**:
    - Xây dựng [`SearchSuggestionService`](file:///d:/HMP/sales-backend/src/main/java/com/hmp/sales/feature/search/service/SearchSuggestionService.java) và [`SearchSuggestionServiceImpl`](file:///d:/HMP/sales-backend/src/main/java/com/hmp/sales/feature/search/service/impl/SearchSuggestionServiceImpl.java) phục vụ ô tìm kiếm nhanh (Type-ahead / Autocomplete) với tốc độ phản hồi cực nhanh dưới $2\text{ms}$ từ bộ nhớ RAM.
    - Áp dụng bộ lọc 2 pha: Pha 1 ưu tiên so khớp tiền tố (`startsWith`) và chuỗi con (`contains`); Pha 2 kích hoạt thuật toán Levenshtein với kỹ thuật **Sliding Window** sửa lỗi gõ trượt phím (như `"ton sog"` $\rightarrow$ `"Tôn sóng"`, `"hoa senn"` $\rightarrow$ `"Hoa Sen"`).
    - Áp dụng **Quy tắc ngưỡng bậc thang (Tiered Threshold)** triệt tiêu gợi ý ngớ ngẩn (từ khóa $< 3$ ký tự chỉ so khớp tiền tố; $3-5$ ký tự sai tối đa 1; $\ge 6$ ký tự sai tối đa 2).
    - Cơ chế **Event-driven Invalidation** qua Spring Application Event (`SearchIndexInvalidationEvent`) và Redis Pub/Sub, tự động đồng bộ cache tức thì mỗi khi Thêm, Sửa, Xóa, Khóa thực thể.
    - REST Controller công khai [`SearchController`](file:///d:/HMP/sales-backend/src/main/java/com/hmp/sales/feature/search/controller/SearchController.java) (`GET /api/v1/search/suggestions`) với validation độ dài tối đa 50 ký tự chặn spam.
  - Toàn bộ **219/219 tests pass** (bổ sung 38 test cases mới: `VietnameseStringUtilsTest`, `SearchSuggestionServiceImplTest`, `SearchControllerTest`).
- ✅ **Bugfix & Validation — Xử lý `MethodArgumentTypeMismatchException` tập trung**:
  - Bổ sung `@ExceptionHandler(MethodArgumentTypeMismatchException.class)` vào `GlobalExceptionHandler` xử lý khi client truyền ID dạng chuỗi (ví dụ `"abc"`) cho các tham số số (`Long`/`Integer`).
  - Tự động nhận diện loại tài nguyên từ tên tham số (`userId`, `categoryId`, `productId`, `brandId`, `unitId`, `colorId`, `variantId`, `roleId`,...) và URI để trả về mã 404 Not Found kèm thông điệp chuẩn tiếng Việt: `"Không tìm thấy [Tên tài nguyên] ID: [giá trị]"`, đồng bộ nhất quán với `ResourceNotFoundException`.
  - Đối với các tham số khác không phải là ID định danh (như phân trang `page`, `size`), trả về mã 400 Bad Request rõ ràng.
  - Thêm unit test và controller MockMvc tests xác minh, toàn bộ 181/181 tests pass.
- ✅ **Phase 3 — Catalog & Category Hierarchy completed**:
  - `Category` entity & `CategoryRepository`: Cấu trúc cây tự tham chiếu cha - con (`parent_category_id`), phát hiện và chặn quan hệ vòng (cycle detection), hỗ trợ mô hình quản lý giống hệ thống thư mục (tạo danh mục con hoặc sản phẩm trực tiếp dưới danh mục hiện tại).
  - Master data động do Admin quản trị (`Brand`, `Unit`, `Color`):
    - Đầy đủ Entity, Repository, Service (`BrandService`, `UnitService`, `ColorService`), Controller (`/api/v1/admin/brands/**`, `/api/v1/admin/units/**`, `/api/v1/admin/colors/**`).
    - Hỗ trợ CRUD, phân trang, tìm kiếm, khóa/mở khóa (`ACTIVE`/`LOCKED`), kiểm tra ràng buộc toàn vẹn chặn xóa (409 Conflict) nếu đã có sản phẩm/bảng giá/biến thể liên kết.
    - Cung cấp các endpoint Public GET phục vụ dropdown chọn thương hiệu, đơn vị tính, màu sắc (`/api/v1/brands`, `/api/v1/units`, `/api/v1/colors`).
  - `Product`, `ProductImage`, `ProductVariant`, `ProductColor`, `ProductPrice`:
    - `Product`: Quản lý thông tin chung, Brand, Category, trạng thái (`ACTIVE`, `INACTIVE`), người tạo.
    - `ProductImage`: Ràng buộc tối đa 1 ảnh đại diện (`isPrimary = true`), tối đa 10 ảnh phụ (tổng tối đa 11 ảnh cho 1 sản phẩm).
    - `ProductVariant`: Thông số kỹ thuật sản xuất (`typeValue`, `thickness`), mã biến thể duy nhất, màu sắc liên kết qua `ProductColor`.
    - `ProductPrice`: Ánh xạ giá bán linh hoạt theo đơn vị tính (`(variantId, unitId) -> price`), tính theo số nguyên VND (`INT`/`BIGINT`).
  - Bảo mật & Phân quyền hiển thị giá (Price Visibility):
    - Sử dụng quyền động `PRODUCT_PRICE_VIEW` kiểm tra qua `PermissionEvaluationService` (tự động lấy Authentication từ SecurityContext).
    - Khi caller thiếu quyền: ẩn giá (`prices = null`), chặn sắp xếp theo giá (`sortBy=price` ném ngoại lệ `BusinessRuleException` tránh rò rỉ thông tin giá gián tiếp).
  - Ràng buộc toàn vẹn & Cơ chế Xóa mềm (Soft Delete):
    - `Category`: Nếu rỗng -> xóa mềm ngay; nếu chứa con/sản phẩm và `force=false` -> trả về 409 Conflict kèm payload số lượng con (`ConflictWithPayloadException`) để frontend hiển thị confirm popup; nếu `force=true` -> xóa mềm cascade nếu không có sản phẩm nào nằm trong đơn hàng dở dang.
    - `Product`: Cho phép xóa mềm ngay cả khi có đơn hàng lịch sử (`COMPLETED`/`CANCELLED`); chỉ chặn xóa (409 Conflict) khi sản phẩm đang nằm trong đơn hàng chưa hoàn thành (`PENDING`, `CONFIRMED`, `PROCESSING`, `SHIPPING`).
  - Dữ liệu mẫu thực tế trong `DataInitializer`:
    - Seed đầy đủ quyền catalog cho `ADMIN` và `MANAGER`.
    - Khởi tạo thương hiệu tôn (Hoa Sen, Đông Á, Nam Kim), đơn vị tính (m, tấm, cuộn, cái), màu sắc (Xanh ngọc, Đỏ đậm, Xanh rêu,...), cây danh mục tôn lợp mái, phụ kiện và sản phẩm mẫu có biến thể, bảng giá.
  - Toàn bộ 157/157 tests pass (thêm 57 test cases mới cho Phase 3 bao gồm Service tests và Controller MockMvc tests).
- ✅ **Phase 2 — Authentication, Tokens & OTP completed**:
  - `GoogleAuthService` & `GoogleAuthServiceImpl`: Xác thực Google ID Token qua endpoint Google TokenInfo (`https://oauth2.googleapis.com/tokeninfo`) sử dụng `RestClient`.
  - `JwtTokenService` & `JwtTokenServiceImpl`: Cấp phát JWT Access Token (HMAC-SHA256) chứa claims `sub`, `email`, `roles`, `jti`, `exp` (bỏ qua `permissions` để hỗ trợ phân quyền động in-memory), tự động đăng ký `auth:access:{jti}` vào Redis với TTL.
  - `RefreshToken` entity & `RefreshTokenRepository`: Lưu trữ mã băm SHA-256 của Refresh Token, quản lý `tokenFamilyId`, `jti`, thời hạn hết hạn `expiresAt` và cờ thu hồi `isRevoked`.
  - `RefreshTokenService` & `RefreshTokenServiceImpl`: Triển khai xoay vòng Refresh Token (Token Rotation) nguyên tử, phát hiện tấn công tái sử dụng (Replay Attack) và thu hồi toàn bộ Token Family.
  - `AuthService` & `AuthServiceImpl`: Điều phối toàn bộ luồng đăng nhập Google (idempotent user creation cho CUSTOMER, chặn tài khoản LOCKED), làm mới token và đăng xuất (xóa cookie HttpOnly, xóa Redis `auth:access:{jti}`, đánh dấu thu hồi trong DB).
  - `AuthController`: Các endpoint RESTful `/api/v1/auth/google`, `/api/v1/auth/refresh`, `/api/v1/auth/logout` với transport cookie `HttpOnly; Secure; SameSite=Lax`.
  - `PhoneOtpService` & `PhoneOtpServiceImpl`: Sinh mã OTP 6 chữ số ngẫu nhiên lưu Redis với TTL 5 phút, giới hạn tần suất gửi lại (cooldown 60s) và giới hạn số lần nhập sai tối đa (5 lần), lưu bảo chứng xác thực `otp:verified:{phone}` (TTL 10 phút) dùng khi đặt đơn hàng.
  - `OtpController`: Endpoint RESTful `/api/v1/orders/otp/request`, `/api/v1/orders/otp/verify` và `/api/v1/me/otp/**`.
  - Bổ sung quyền `ORDER_OTP_REQUEST`, `ORDER_OTP_VERIFY`, `ME_OTP_REQUEST`, `ME_OTP_VERIFY` vào `DataInitializer` cho vai trò `CUSTOMER`.
  - Toàn bộ 100/100 tests pass (thêm 35 test cases mới cho Phase 2: `JwtTokenServiceImplTest`, `RefreshTokenServiceImplTest`, `AuthServiceImplTest`, `PhoneOtpServiceImplTest`, `AuthControllerTest`, `OtpControllerTest`).
- ✅ **Phase 1 — Identity, Roles & Permissions (Task 1 & Task 2 completed)**:
  - JPA Entities configured in `com.hmp.sales.feature.user.entity`:
    - `Role` (bảng `roles`, khóa chính `role_id`, ràng buộc duy nhất trên `role_code`, cờ `is_deleted`, audit timestamps).
    - `Permission` (bảng `permissions`, khóa chính `permission_id`, `permission_name` duy nhất, ràng buộc `(api_path, method)`, phân hệ `module`, cờ `is_deleted`, audit timestamps).
    - `RolePermission` (bảng liên kết `role_permissions`, composite key `RolePermissionId`, hỗ trợ phân quyền động `PRODUCT_PRICE_VIEW`).
    - `User` (bảng `users`, khóa chính `user_id`, hỗ trợ `GOOGLE` và `LOCAL`, `ACTIVE`/`LOCKED`, ràng buộc duy nhất `email` và `(auth_provider, provider_subject)`).
  - Enums: `RoleCode` (`ADMIN`, `MANAGER`, `CUSTOMER`), `AuthProvider` (`GOOGLE`, `LOCAL`), `UserStatus` (`ACTIVE`, `LOCKED`).
  - Spring Data JPA Repositories in `com.hmp.sales.feature.user.repository`:
    - `RoleRepository`: tìm theo `roleCode`, lọc `isDeleted = false`.
    - `PermissionRepository`: tìm theo `permissionName`, `apiPath` + `method`, lọc theo module.
    - `RolePermissionRepository`: tìm quyền theo role (`findPermissionsByRoleId`), kiểm tra quyền nhanh (`hasPermission`).
    - `UserRepository`: tìm theo `email`, `authProvider` + `providerSubject`, eager fetch role (`findByIdWithRole`, `findByEmailWithRole`).
  - Toàn bộ 35/35 unit và integration tests pass (thêm 12 repository test cases mới: `RoleRepositoryTest`, `PermissionRepositoryTest`, `RolePermissionRepositoryTest`, `UserRepositoryTest`).
- ✅ **Phase 0 — Foundation & Infrastructure completed**:
  - Spring Boot 4.1.1 project configured with Java 21, Spring Data JPA, Spring Security 7, OAuth2 Resource Server, Redis, Validation, Lombok, and H2 test DB.
  - Multi-profile YAML configuration (`application.yaml`, `application-dev.yaml`, `application-test.yaml`) with `AppProperties` for JWT, OTP, CORS, and order properties.
  - Standard API response wrappers: `ApiSuccessResponse<T>`, `ApiErrorResponse<T>`, `ValidationErrorResponse` matching `api-spec.md`.
  - Base custom exceptions: `ResourceNotFoundException`, `BusinessRuleException`, `UnauthorizedOperationException`, `InvalidOrderStateException`, `OtpVerificationException`.
  - Centralized `@RestControllerAdvice` (`GlobalExceptionHandler`) for Vietnamese standardized error responses and validation messages.
  - `RedisConfig` with `RedisTemplate<String, Object>` and `StringRedisTemplate`.
  - `SecurityConfig` with stateless OAuth2 Resource Server, HMAC-SHA256 `JwtDecoder`, custom `RestAuthenticationEntryPoint`, and `RestAccessDeniedHandler`.
  - `RedisTokenValidationFilter` enforcing active token verification against `auth:access:{jti}` in Redis.
  - 23 unit and integration tests passing (`ApiResponseTest`, `GlobalExceptionHandlerTest`, `RedisTokenValidationFilterTest`, `SalesBackendApplicationTests`).
- ✅ **Requirements baseline confirmed**:
  - Website purpose: direct online sales + product introduction/contact.
  - Initial scope: Vietnamese language, mobile responsive, individual customers, one province/city.
  - Guest browsing without authentication; login required for ordering.
  - Customer authentication via Google; forgot-password/change-password excluded for Google accounts.
  - OTP required when phone number is entered during ordering (saved on first entry, updateable later).
  - Business roles: Admin, Manager, Customer.
  - Product price visibility is permission-driven (`PRODUCT_PRICE_VIEW`), configurable by Admin.
  - Customer can see order total during confirmation step after Manager calculation.
  - Order cancellation reason is optional for every role; order status history tracked.
  - Category behaves as a self-referencing parent-child tree; Manager can create child category or product under selected category.
  - Product active/locking status supported (`ACTIVE`/`INACTIVE`).
  - Manufacturing focus for V1: Tôn sóng lợp mái (inputs: type, color, length/size, quantity).
  - Size-based manufacturing formula confirmed: `TotalPrice = UnitPrice × Size × Quantity`.
  - Storage roles: PostgreSQL is durable source of truth; Redis is used for active access tokens (`jti`) and OTP with TTL.
  - Refresh tokens stored as cryptographic hashes in PostgreSQL and rotated on every refresh.
  - **Out-of-scope for V1 confirmed**: Online payment, inventory management, promotions/coupons, product reviews, quotation management, laser cutting, warranty workflow, news/blog module.
- ✅ **Architecture documentation completed** (`architecture.md`):
  - High-level layered architecture (`Controller -> Service -> Repository`).
  - Guest, Customer, Manager, and Admin business flows.
  - Category tree navigation and management flow.
  - Permission-based price visibility flow.
  - Order calculation, confirmation, and cancellation flows.
  - Product active/inactive locking flow.
  - Redis OTP flow and notification dispatch flow.
- ✅ **Database schema design completed** (`database.md`):
  - PostgreSQL schema for 21 tables with explicit PK/FK relationships, constraints, and audit fields.
  - Identity/Security: `Role`, `User`, `Permission`, `RolePermission`, `RefreshToken`.
  - Catalog: `Category`, `Brand`, `Unit`, `Product`, `ProductImage`, `ProductVariant`, `Color`, `ProductColor`, `ProductPrice`.
  - Ordering: `CustomerAddress`, `Order`, `OrderItem`, `OrderStatusHistory`.
  - Platform: `Notification`, `SystemLog`, `SystemSetting`.
  - Redis key design: `auth:access:{jti}`, `otp:phone:<phone>`, and rate-limiting keys.
- ✅ **REST API specification completed** (`api-spec.md`):
  - Standard response format (`ApiSuccessResponse<T>`) and error envelope (`ApiErrorResponse<T>`).
  - Public catalog APIs, Google auth & token APIs, Customer profile/address/order APIs, Manager catalog/order processing APIs, Admin user/role/setting/audit APIs.
- ✅ **Phase 1 — Identity, Roles & Permissions completed**:
  - `Role`, `Permission`, `RolePermission`, `User` entities & JPA repositories with integration tests.
  - `DataInitializer` startup component: idempotent seed for default Roles, Permissions, and Role-Permission mappings.
  - `PermissionEvaluationService` (`permEval` bean) for dynamic SpEL permission checks from JWT authorities without DB round-trips.
  - `SystemLogService` stub logging audit operations.
  - `AdminUserService` & `AdminUserController`: user pagination/search, account locking/unlocking, soft-delete with token revocation, and Google-based Manager provisioning.
  - `AdminRoleService` & `AdminRoleController`: view role permissions, replace-all permission updates, and forced token revocation flow (Section 18).
  - Refactor: Triển khai kiến trúc **Dynamic In-Memory Permission Authorization**:
    - `PermissionAuthorizationManager` implements `AuthorizationManager<RequestAuthorizationContext>`: nạp cache singleton `rolePermissionsCache` vào RAM khi khởi động.
    - So khớp quyền động qua `AntPathMatcher` dựa trên `(apiPath, method)` của quyền thuộc vai trò trong cache.
    - Tích hợp `anyRequest().access(permissionAuthorizationManager)` vào `SecurityConfig`.
    - Tự động làm mới cache (cache invalidation) qua `loadCache()` trong `RoleServiceImpl` và `DataInitializer`.
    - Cập nhật `PermissionEvaluationService` tra cứu quyền động từ cache RAM cho các logic nghiệp vụ lọc dữ liệu (như `PRODUCT_PRICE_VIEW`).
  - Toàn bộ 65/65 unit và integration tests pass (11 test cases mới: `PermissionAuthorizationManagerTest`, `RolePermissionRepositoryTest`, `PermissionEvaluationServiceTest`).

## In Progress
_Nothing._

## Deferred Issues
- **[P2]** Google OAuth exchange architecture: finalize frontend callback vs backend authorization code verification contract.
- **[P2]** Production OTP & notification provider: select SMS / Zalo ZNS provider and configure API credentials.
- **[P2]** Exact application JWT issuer & asymmetric/symmetric signing key management strategy.
- **[P2]** Exact order status cancellation matrix: determine which statuses remain cancellable after Manager confirms order.
- **[P2]** Confirm whether all calculated orders require explicit Customer confirmation before moving to `PROCESSING`.
- **[P3]** Cloud/object storage provider (S3 / Cloudinary) for product images deferred to file module implementation.
- **[P3]** Concrete numeric validation constraints for sheet dimensions (length, thickness, min/max limits).
- **[P3]** Exact enumerated `Type` values for Tôn sóng variants and accessories (sườn, xối, nóc, máng).
- **[P3]** Production Redis reliability pattern (transactional outbox or Redis Stream consumer group with dead-letter retry).
- **[P3]** Add ERD visual diagram and Google OAuth sequence diagram to documentation.

## Warnings
- ⚠️ **Spring Boot 4 / Spring Security 7**: OAuth2 Resource Server validates JWT signature/claims, but application must also check active token existence in Redis via `jti` on every request.
- ⚠️ **PostgreSQL Source of Truth**: Redis is strictly for ephemeral data (OTP, active `jti`); never store durable business entities or refresh tokens in Redis.
- ⚠️ **Money Calculation (VND)**: All monetary values must use `BIGINT` or `INT` (never floating-point types). Manufacturing calculation formula: `TotalPrice = UnitPrice × Size × Quantity`.
- ⚠️ **Permission-Driven Price Visibility**: `PRODUCT_PRICE_VIEW` is a configurable permission controlled by Admin; never hard-code price visibility logic based on role.
- ⚠️ **Refresh Token Rotation**: Refresh tokens must be stored as cryptographic hashes in PostgreSQL; old tokens must be revoked atomically when issuing replacements, with token family reuse detection.
- ⚠️ **Data Integrity & Snapshots**: Historical order data (prices, addresses, receiver info) must remain immutable snapshots. Do not recalculate historical orders when current product prices change, and do not physically delete master data referenced by past orders.
- ⚠️ **Scope Protection**: Do NOT implement V1 excluded modules (payment gateways, promotions, reviews, inventory, quotation, warranty, laser cutting, blogs).

## Next Tasks
1. **[P2]** Phase 6: Triển khai kiểm thử tích hợp chuyên sâu (Integration Tests) với MockMvc và Spring Security cho toàn bộ phân hệ quản trị Admin (Users, Roles, Settings, SystemLogs, Master Data).
2. **[P2]** Phase 6: Rà soát kiểm thử tự động cho toàn bộ các kịch bản biên giới hạn kích thước tôn sóng và ma trận trạng thái đơn hàng.

## Milestones

### Phase 0 — Foundation & Infrastructure
- [x] Create Spring Boot 4.x project skeleton (Maven, application.yml, multi-profile config)
- [x] PostgreSQL datasource & JPA/Hibernate setup
- [x] Redis connection & RedisTemplate / Lettuce configuration
- [x] Base exception classes (`ResourceNotFoundException`, `BusinessRuleException`, `UnauthorizedOperationException`, `InvalidOrderStateException`, `OtpVerificationException`)
- [x] Centralized exception handling (`@RestControllerAdvice`, `GlobalExceptionHandler`)
- [x] Standard API response wrapper (`ApiSuccessResponse<T>`, `ApiErrorResponse<T>`)
- [x] Jakarta Bean Validation configuration
- [x] Spring Security 7 OAuth2 Resource Server baseline configuration
- [x] Redis-backed access token validation filter / bearer token interceptor (`jti`)

### Phase 1 — Identity, Roles & Permissions
- [x] Role entity & repository (`ADMIN`, `MANAGER`, `CUSTOMER`)
- [x] Permission entity & repository (including `PRODUCT_PRICE_VIEW`, `ORDER_*`, `PRODUCT_*`)
- [x] RolePermission join entity & repository
- [x] User entity & repository (supporting `GOOGLE` and `LOCAL` auth providers, `ACTIVE`/`LOCKED` status)
- [x] User unique constraints (`Email`, `(AuthProvider, ProviderSubject)`)
- [x] Role & Permission initial seed data / migration (`DataInitializer` idempotent startup runner)
- [x] Permission evaluation service / custom authorization manager for dynamic permission checks (`PermissionEvaluationService`)
- [x] Admin User management service & APIs (search, lock/unlock accounts, soft-delete, create Manager)
- [x] Admin Role & Permission management APIs (view/update role permissions with forced token revocation flow)

### Phase 2 — Authentication, Tokens & OTP
- [x] Google OAuth token verification service & user account resolution (idempotent mapping)
- [x] Application JWT token generation service (with unique `jti`, roles claim, dynamic permissions)
- [x] Redis active access token registry (`auth:access:{jti}` with TTL)
- [x] RefreshToken entity & repository with cryptographic hash storage, family tracking, and rotation
- [x] Refresh token rotation logic with token reuse detection and revocation
- [x] Auth endpoints:
  - [x] `POST /api/v1/auth/google` (login / register with Google)
  - [x] `POST /api/v1/auth/refresh` (rotate refresh token, issue new JWT)
  - [x] `POST /api/v1/auth/logout` (revoke Redis access token and PostgreSQL refresh token)
- [x] Redis phone OTP service:
  - [x] 6-digit random OTP generation with TTL (`otp:phone:<phone>`)
  - [x] Rate limiting for OTP generation and verification attempts
  - [x] OTP verification and single-use invalidation
- [x] Auth & OTP unit and integration tests (100/100 tests pass)

### Phase 3 — Catalog & Category Hierarchy
- [x] Category entity (self-referencing parent-child tree) & repository
- [x] Category tree validation (prevent circular hierarchy references)
- [x] Master data entities & CRUD repositories:
  - [x] Brand entity & repository (`ACTIVE`/`LOCKED`, soft-delete)
  - [x] Unit entity & repository (`ACTIVE`/`LOCKED`, soft-delete)
  - [x] Color entity & repository (`ACTIVE`/`LOCKED`, soft-delete)
- [x] Product entity (`ACTIVE`/`INACTIVE` status, Brand, Category references)
- [x] ProductImage entity (1 primary image constraint, max 10 secondary images)
- [x] ProductVariant entity (spec, thickness, variantCode, active state)
- [x] ProductColor entity & composite repository
- [x] ProductPrice entity & composite repository (by variant and unit, VND integers)
- [x] Product status management (active/locking logic: prevent ordering inactive products)
- [x] Public catalog & master data APIs:
  - [x] `GET /api/v1/categories/tree` (hierarchical category tree)
  - [x] `GET /api/v1/categories/{categoryId}/products` (products in category)
  - [x] `GET /api/v1/brands`, `GET /api/v1/units`, `GET /api/v1/colors` (active master data for browsing/filters)
  - [x] `GET /api/v1/products` (filter, sort, search, conditional price visibility based on `PRODUCT_PRICE_VIEW`)
  - [x] `GET /api/v1/products/{productId}` (detail, variants, images, conditional price visibility)
- [x] Manager catalog management APIs:
  - [x] Create child category or product under current category
  - [x] Category CRUD & display order updates
  - [x] Product CRUD, image management, variant management
  - [x] Product lock/unlock endpoint
- [x] Admin master data management APIs (Dynamic CRUD for Brand, Unit, Color):
  - [x] Brand CRUD, search/pagination, lock/unlock (`/api/v1/admin/brands/**`)
  - [x] Unit CRUD, search/pagination, lock/unlock (`/api/v1/admin/units/**`)
  - [x] Color CRUD, search/pagination, lock/unlock (`/api/v1/admin/colors/**`)
- [x] Catalog unit and integration tests (157/157 tests pass)

### Phase 4 — Ordering & Manufacturing Price Calculation
- [x] CustomerAddress entity, repository, and CRUD APIs (`GET/POST/PATCH/DELETE /api/v1/me/addresses`)
- [x] Customer profile APIs (`GET /api/v1/me`, `PATCH /api/v1/me`)
- [x] Order & OrderItem entities with immutable price snapshots (`UnitPrice`, `TotalPrice`, `TotalAmount`)
- [x] Receiver snapshot fields on Order (`ReceiverName`, `ReceiverPhone`, address fields)
- [x] OrderStatusHistory entity & repository
- [x] Order creation flow:
  - [x] Customer order submission with phone OTP verification
  - [x] Validation for active products, valid variants, positive quantity and size
  - [x] Initial state assignment: `PENDING`
  - [x] Direct PostgreSQL order creation with atomic transaction (`PENDING` state)
- [x] SystemSetting dynamic cancellation window configuration (`ORDER_CUSTOMER_CANCELLATION_WINDOW_MINUTES`)
- [x] Background scheduler for auto-transitioning confirmed orders to `PROCESSING` (`OrderAutoTransitionScheduler`)
- [x] Manager order processing & calculation:
  - [x] Order listing with filters, pagination, and detail view (`GET /api/v1/manager/orders`, `GET /api/v1/manager/orders/{orderId}`)
  - [x] Manager price & discount calculation: by unit price, item discount amount, or whole-order discount (`POST /api/v1/manager/orders/{orderId}/calculate`)
  - [x] Decoupled order confirmation (`POST /api/v1/manager/orders/{orderId}/confirm`) transitioning to `CONFIRMED`
  - [x] Lifecycle status updates (`CONFIRMED -> PROCESSING -> SHIPPING -> COMPLETED`) and cancellations (including `SHIPPING` state)
- [x] Customer order viewing & cancellation flow:
  - [x] Customer views calculated order details
  - [x] Customer cancellation validation against dynamic cancellation window from DB
  - [x] Optional cancellation reason for all roles
  - [x] Order undo cancellation flow for Customer (within 60m window, self-cancelled only) and Manager/Admin (any prior state, manager-cancelled only) (`POST /api/v1/orders/{id}/undo-cancel`, `POST /api/v1/manager/orders/{id}/undo-cancel`)
  - [x] Immutable history logging in `OrderStatusHistory` for every valid transition
- [x] Order unit and integration tests (380/380 tests pass)

### Phase 5 — Platform, Audit & Notifications
- [x] Notification entity & repository
- [x] Notification service (dispatch in-app notifications on order status changes)
- [x] Customer notification APIs (`GET /api/v1/notifications`, `GET /api/v1/notifications/unread-count`, mark as read)
- [x] External notification integration (Zalo ZNS with RestClient, SendGrid Email with Thymeleaf HTML templates, and NoOp stub)
- [x] SystemSetting entity, repository, and Admin configuration APIs (`GET /api/v1/admin/settings`, `PUT /api/v1/admin/settings/{key}`)
- [x] SystemLog entity, repository, and business audit logging service (persisted to PostgreSQL `system_logs`)
- [x] Admin system audit log query APIs (`GET /api/v1/admin/system-logs`, `GET /api/v1/admin/system-logs/{logId}`)

### Phase 6 — Comprehensive Testing & Hardening
- [ ] **JUnit 5 Unit Tests**:
  - [ ] Manufacturing price calculation formula (`UnitPrice × Size × Quantity`)
  - [ ] Invalid size, negative quantity, and inactive product order rejections
  - [ ] Dynamic permission evaluation (`PRODUCT_PRICE_VIEW`) for price visibility
  - [ ] Order status transition matrix validation
  - [ ] Customer cancellation window boundary tests
  - [ ] Optional cancellation reason validation
  - [ ] Category self-referencing cycle prevention
  - [ ] Google account idempotent mapping
  - [ ] OTP generation, TTL expiry, attempt rate limiting, and replay prevention
- [ ] **Integration Tests**:
  - [ ] Spring Security 7 OAuth2 Resource Server bearer token authentication
  - [ ] Redis active access token (`jti`) revocation & logout integration
  - [ ] Refresh token rotation, cryptographic hashing, and family reuse detection
  - [ ] PostgreSQL atomic transactions and rollback behavior
  - [ ] Redis OTP TTL and access token revocation validation
  - [ ] REST API controller request validation and error responses
- [ ] **End-to-End / Manual Verification Pass**:
  - [ ] Guest browse -> Google login -> order flow
  - [ ] Phone OTP challenge -> verify -> order submission
  - [ ] Manager receives order -> calculates price -> status update
  - [ ] Customer views calculated total -> confirms order
  - [ ] Customer cancellation within allowed window
  - [ ] Product lock prevents subsequent ordering
  - [ ] Admin enables `PRODUCT_PRICE_VIEW` for Customer -> prices become visible in product catalog

## Definition of Done

A feature / task is complete only when:

```text
Business rule documented
        +
Database/API updated if needed
        +
Implementation complete
        +
Authorization verified
        +
Validation + error handling added
        +
Relevant JUnit 5 tests pass
        +
Manual/integration verification complete where required
        =
DONE
```
