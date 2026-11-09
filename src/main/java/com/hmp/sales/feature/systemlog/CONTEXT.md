# System Audit Log — Implementation Context
> Written: 2026-10-25 | Last Updated: 2026-11-08 | Author: @antigravity

## Business Context
Provides business and security audit trail recording for sensitive administrative actions (role permission updates, user locking/unlocking, manager provisioning, system settings modifications, catalog modifications, staff order processing, and security events) to satisfy accountability and traceability requirements.

## Audited Actions Matrix (Danh sách hành động được ghi log)

Dưới đây là toàn bộ danh mục mã hành động (`action`), loại thực thể (`entityType`), service thực thi và ngữ cảnh ghi log được bảo vệ trong hệ thống:

| Nhóm nghiệp vụ | Mã hành động (`action`) | Loại thực thể (`entityType`) | Service triển khai | Mô tả chi tiết & Dữ liệu ghi vết (`oldValue` / `newValue`) |
|---|---|---|---|---|
| **Người dùng & Vai trò** (`user`) | `USER_LOCKED` | `USER` | `UserServiceImpl.lockUser` | Khóa tài khoản người dùng bởi Admin |
| | `USER_UNLOCKED` | `USER` | `UserServiceImpl.unlockUser` | Mở khóa tài khoản người dùng bởi Admin |
| | `USER_SOFT_DELETED` | `USER` | `UserServiceImpl.deleteUser` | Xóa mềm tài khoản người dùng bởi Admin |
| | `MANAGER_CREATED` | `USER` | `UserServiceImpl.createManager` | Admin tạo tài khoản Quản lý (Manager) mới |
| | `ROLE_PERMISSIONS_UPDATED` | `ROLE` | `RoleServiceImpl.updateRolePermissions` | Admin thay đổi danh mục quyền hạn gán cho vai trò |
| | `USER_PHONE_CHANGED` | `USER` | `UserProfileServiceImpl.updateProfile` | Khách hàng cập nhật SĐT mới sau khi xác thực OTP thành công (SĐT cũ và mới được che dạng `09*****123`) |
| **Cấu hình hệ thống** (`setting`) | `SYSTEM_SETTING_UPDATED` | `SYSTEM_SETTING` | `SystemSettingServiceImpl.updateSetting` | Admin cập nhật giá trị tham số cấu hình động (`oldValue`/`newValue` giá trị cấu hình) |
| **Danh mục** (`category`) | `CATEGORY_CREATED` | `CATEGORY` | `CategoryServiceImpl.createRootCategory`<br>`CategoryServiceImpl.createChildCategory` | Tạo mới danh mục gốc hoặc danh mục con |
| | `CATEGORY_UPDATED` | `CATEGORY` | `CategoryServiceImpl.updateCategory` | Đổi tên danh mục (`oldValue`/`newValue` tên danh mục) |
| | `CATEGORY_LOCKED` | `CATEGORY` | `CategoryServiceImpl.lockCategory` | Khóa danh mục (`status`: `ACTIVE` → `LOCKED`) |
| | `CATEGORY_UNLOCKED` | `CATEGORY` | `CategoryServiceImpl.unlockCategory` | Mở khóa danh mục (`status`: `LOCKED` → `ACTIVE`) |
| | `CATEGORY_DELETED` | `CATEGORY` | `CategoryServiceImpl.deleteCategory` | Xóa mềm danh mục (ghi vết số lượng danh mục con và sản phẩm bị xóa cascade nếu `force=true`) |
| **Sản phẩm & Biến thể & Ảnh** (`product`) | `PRODUCT_CREATED` | `PRODUCT` | `ProductServiceImpl.createProduct` | Quản lý tạo sản phẩm mới |
| | `PRODUCT_UPDATED` | `PRODUCT` | `ProductServiceImpl.updateProduct` | Cập nhật tên/thông tin sản phẩm (`oldValue`/`newValue` tên) |
| | `PRODUCT_LOCKED` | `PRODUCT` | `ProductServiceImpl.lockProduct` | Khóa sản phẩm (`status`: `ACTIVE` → `INACTIVE`) |
| | `PRODUCT_UNLOCKED` | `PRODUCT` | `ProductServiceImpl.unlockProduct` | Mở khóa sản phẩm (`status`: `INACTIVE` → `ACTIVE`) |
| | `PRODUCT_DELETED` | `PRODUCT` | `ProductServiceImpl.deleteProduct` | Xóa mềm sản phẩm và cascade ảnh, biến thể |
| | `VARIANT_ADDED` | `PRODUCT_VARIANT` | `ProductServiceImpl.addVariant` | Thêm biến thể mới cho sản phẩm |
| | `VARIANT_UPDATED` | `PRODUCT_VARIANT` | `ProductServiceImpl.updateVariant` | Cập nhật thông số kỹ thuật, giá, màu sắc của biến thể |
| | `VARIANT_LOCKED` | `PRODUCT_VARIANT` | `ProductServiceImpl.lockVariant` | Khóa biến thể sản phẩm |
| | `VARIANT_UNLOCKED` | `PRODUCT_VARIANT` | `ProductServiceImpl.unlockVariant` | Mở khóa biến thể sản phẩm |
| | `VARIANT_DELETED` | `PRODUCT_VARIANT` | `ProductServiceImpl.deleteVariant` | Xóa mềm biến thể và các liên kết giá/màu |
| | `PRODUCT_IMAGES_UPLOADED` | `PRODUCT` | `ProductServiceImpl.uploadProductImages` | Tải lên danh sách ảnh sản phẩm (ghi nhận số lượng ảnh) |
| | `PRODUCT_IMAGES_DELETED` | `PRODUCT` | `ProductServiceImpl.deleteProductImages` | Xóa ảnh sản phẩm (ghi nhận số lượng ảnh) |
| **Dữ liệu chủ** (`product/masterdata`) | `BRAND_CREATED`, `BRAND_UPDATED`,<br>`BRAND_LOCKED`, `BRAND_UNLOCKED`,<br>`BRAND_DELETED` | `BRAND` | `BrandServiceImpl` | Toàn bộ vòng đời thương hiệu sản phẩm do Admin quản lý |
| | `UNIT_CREATED`, `UNIT_UPDATED`,<br>`UNIT_LOCKED`, `UNIT_UNLOCKED`,<br>`UNIT_DELETED` | `UNIT` | `UnitServiceImpl` | Toàn bộ vòng đời đơn vị tính bán hàng do Admin quản lý |
| | `COLOR_CREATED`, `COLOR_UPDATED`,<br>`COLOR_LOCKED`, `COLOR_UNLOCKED`,<br>`COLOR_DELETED` | `COLOR` | `ColorServiceImpl` | Toàn bộ vòng đời màu sắc tôn do Admin quản lý |
| **Đơn hàng Quản lý / Staff** (`order`) | `ORDER_PRICE_CALCULATED` | `ORDER` | `ManagerOrderServiceImpl.calculateOrderPrice` | Quản lý tính đơn giá, chiết khấu và tổng thanh toán (`oldValue`/`newValue` tổng tiền và giảm giá) |
| | `ORDER_CONFIRMED` | `ORDER` | `ManagerOrderServiceImpl.confirmOrder` | Quản lý xác nhận đơn hàng sau khi tính giá |
| | `ORDER_STATUS_UPDATED` | `ORDER` | `ManagerOrderServiceImpl.updateOrderStatus` | Quản lý cập nhật tiến trình đơn hàng (`oldValue`/`newValue` trạng thái) |
| | `ORDER_CANCELLED_BY_STAFF` | `ORDER` | `ManagerOrderServiceImpl.cancelOrder` | Quản lý/Admin chủ động hủy đơn hàng |
| | `ORDER_CANCEL_UNDONE_BY_STAFF` | `ORDER` | `ManagerOrderServiceImpl.undoCancelOrder` | Quản lý/Admin hoàn tác hủy đơn hàng, khôi phục về trạng thái trước đó |
| **Xác thực & Bảo mật** (`auth`) | `CUSTOMER_REGISTERED` | `USER` | `AuthServiceImpl.loginWithGoogle` | Tài khoản khách hàng mới được tự động khởi tạo qua Google OAuth (email được che dạng `c***@example.com`) |
| | `LOGIN_BLOCKED_LOCKED_ACCOUNT` | `USER` | `AuthServiceImpl.loginWithGoogle` | Chặn đăng nhập đối với tài khoản đang bị khóa (*ghi qua `logSecurityEvent`*) |
| | `REFRESH_TOKEN_REUSE_DETECTED` | `REFRESH_TOKEN_FAMILY` | `RefreshTokenServiceImpl.rotateRefreshToken` | Phát hiện tấn công tái sử dụng token (Replay Attack), thu hồi toàn bộ token family (*ghi qua `logSecurityEvent`*) |

### Các hành động KHÔNG ghi vào `system_logs` (Exclusions)
- **Thao tác đọc (GET)**: Không ghi nhật ký cho các request tra cứu, tìm kiếm dữ liệu.
- **Đăng nhập thành công (`LOGIN`) & Đăng xuất (`LOGOUT`)**: Không ghi log nhằm tránh quá tải cơ sở dữ liệu.
- **Hành động thường ngày của khách hàng**: Đặt hàng, xem đơn hàng, hủy đơn hàng trong thời hạn, thêm địa chỉ giao nhận (tiến trình vòng đời đơn hàng đã được theo dõi bất biến chuyên biệt tại bảng `order_status_histories`).

## Technical Decisions
- **Interface + PostgreSQL Implementation (Phase 5)**: `SystemLog` entity (bảng `system_logs`) được lưu trực tiếp vào PostgreSQL dưới dạng audit trail chỉ-ghi-thêm (append-only), không cung cấp API sửa/xóa.
- **Transactional Consistency (Decision Confirmed)**:
  - `log` và `logWithChange` tham gia cùng transaction nghiệp vụ với caller (`REQUIRED`), cam kết tính toàn vẹn 100% (thao tác và nhật ký cùng commit hoặc cùng rollback).
  - `logSecurityEvent` mở transaction độc lập (`REQUIRES_NEW`) và tự bắt lỗi để ghi nhận các sự cố an ninh ngoại luồng mà không làm gián đoạn luồng nghiệp vụ.
- **Masking & Sanitization**: `AuditValueUtils` tự động lọc bỏ các khóa nhạy cảm (`password`, `token`, `otp`, `secret`, `authorization`, `credential`), che số điện thoại và email trước khi đóng gói JSON `oldValue`/`newValue`.
- **Query & Specifications**: `SystemLogSpecification` hỗ trợ lọc động theo role actor, activity, khoảng ngày, tìm kiếm tên không dấu (`VietnameseStringUtils`), SĐT, email với phân trang `PageResponse`.

## Endpoints
- `GET /api/v1/admin/system-logs`: Tìm kiếm danh sách audit logs có phân trang và bộ lọc (yêu cầu quyền `SYSTEM_LOG_VIEW`).
- `GET /api/v1/admin/system-logs/{logId}`: Xem chi tiết một bản ghi audit log kèm trạng thái trước/sau thay đổi.

## Dependencies
- Bảng PostgreSQL `system_logs`.
- Injected vào các service: `user`, `setting`, `category`, `product`, `order`, `auth`.

## Refactor Log
- **2026-11-08**: Phase 5 completed. Triển khai toàn diện `SystemLog` entity, `SystemLogRepository`, `SystemLogSpecification`, `SystemLogServiceImpl`, `AdminSystemLogController`, và gắn audit trail đầy đủ vào toàn bộ các service quản trị và bảo mật.
