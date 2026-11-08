# Notification Feature Context

## 1. Overview
Phân hệ `notification` đảm nhiệm việc tiếp nhận, quản lý và phân phát thông báo in-app (lưu trữ bền vững trong PostgreSQL) tới người dùng trong hệ thống (Khách hàng, Quản lý, Admin).
Đồng thời, phân hệ xây dựng sẵn kiến trúc Event-Driven nội bộ qua Spring Application Event (`OrderStatusChangedEvent`) và tầng trừu tượng `ExternalNotificationProvider` (No-Op ở V1) sẵn sàng mở rộng tích hợp Zalo ZNS, Email, SMS ở V2 mà không cần thay đổi nghiệp vụ đơn hàng.

## 2. Architecture & Design Patterns
- **Database Persistence**: Bảng `notifications` (PostgreSQL - Source of Truth), quản lý trạng thái đọc (`isRead`, `readAt`), khóa ngoại liên kết tới `User`, phân loại theo `NotificationType`, hỗ trợ soft-delete (`isDeleted`).
- **Event-Driven Decoupling**: Các service đơn hàng (`ManagerOrderServiceImpl`, `CustomerOrderServiceImpl`, `OrderAutoTransitionScheduler`) chỉ phát sinh sự kiện `OrderStatusChangedEvent`.
- **Transactional Consistency**: Listener `NotificationServiceImpl.onOrderStatusChanged` lắng nghe với `@TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)` và `@Transactional(propagation = Propagation.REQUIRES_NEW)`, đảm bảo đơn hàng đã commit thành công mới lưu thông báo, lỗi thông báo không làm ảnh hưởng giao dịch đơn hàng.
- **Provider Abstraction**: Giao tiếp ngoại vi thông qua `ExternalNotificationProvider` với triển khai mặc định `NoOpExternalNotificationProvider`.
- **Layered Pattern**: `NotificationController` -> `NotificationService` (Interface) / `NotificationServiceImpl` -> `NotificationRepository` -> PostgreSQL.

## 3. Endpoints (Base Path: `/api/v1/notifications`)
- `GET /api/v1/notifications`: Lấy danh sách thông báo phân trang của người dùng hiện tại (trả về envelope `PageResponse<NotificationResponse>`).
- `GET /api/v1/notifications/unread-count`: Lấy số lượng thông báo chưa đọc của người dùng hiện tại (`UnreadCountResponse`).
- `POST /api/v1/notifications/{notificationId}/read`: Đánh dấu một thông báo cụ thể là đã đọc.
- `POST /api/v1/notifications/read-all`: Đánh dấu toàn bộ thông báo chưa đọc của người dùng là đã đọc (`MarkAllReadResponse`).

## 4. Key Classes & Interfaces
- `Notification`: Thực thể JPA ánh xạ bảng `notifications`.
- `NotificationType`: Enum gồm `ORDER_CREATED`, `ORDER_CONFIRMED`, `ORDER_PROCESSING`, `ORDER_SHIPPING`, `ORDER_COMPLETED`, `ORDER_CANCELLED`, `ORDER_RESTORED`.
- `NotificationRepository`: Spring Data JPA Repository, cung cấp các truy vấn theo `user.id`, đếm chưa đọc và `@Modifying` bulk update `markAllAsRead`.
- `NotificationService` / `NotificationServiceImpl`: Interface và Service xử lý nghiệp vụ.
- `OrderStatusChangedEvent`: Record chứa dữ liệu sự kiện thay đổi trạng thái đơn hàng.
- `ExternalNotificationProvider` / `NoOpExternalNotificationProvider`: Trừu tượng hóa cổng thông báo bên ngoài.
- `ZaloNotificationProvider`: Triển khai gửi ZNS qua Zalo OpenAPI (`RestClient`), kích hoạt bởi `@ConditionalOnProperty(name = "app.notification.zalo.enabled", havingValue = "true")`.
- `SendGridEmailNotificationProvider`: Triển khai gửi email qua SendGrid API với template Thymeleaf HTML và nội dung plain text fallback, kích hoạt bởi `@ConditionalOnProperty(name = "app.notification.email.enabled", havingValue = "true")`.
- `NotificationController`: REST Controller tiếp nhận request từ client.

