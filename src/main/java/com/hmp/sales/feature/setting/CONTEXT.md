# System Setting Feature Context

## 1. Overview
Phân hệ quản lý tham số cấu hình động hệ thống (`SystemSetting`) tại `com.hmp.sales.feature.setting`.
Cho phép Quản trị viên (Admin) cấu hình các tham số vận hành nghiệp vụ mà không cần sửa code hay khởi động lại ứng dụng.

## 2. Entities & Schema Mapping
- `SystemSetting`: Đại diện bảng `system_settings`.
  - Khóa chính `setting_id` (`Long`).
  - Khóa cấu hình duy nhất `setting_key` (`VARCHAR(100)`).
  - Giá trị cấu hình `setting_value` (`VARCHAR(1000)`).
  - Kiểu dữ liệu `data_type` (`VARCHAR(30)`).
  - Audit: `updated_by`, `updated_at`, `is_deleted`.

## 3. Core Settings
- `ORDER_CUSTOMER_CANCELLATION_WINDOW_MINUTES`: Thời gian tối đa (phút) khách hàng được phép hủy đơn hàng sau khi tạo đơn hoặc sau khi Quản lý chốt giá.
  - Giá trị mặc định trong `DataInitializer`: `"30"`.
  - Cơ chế fallback: Nếu chưa có trong DB hoặc parse lỗi, tự động fallback về `app.order.cancellation-window-minutes` trong `application.yaml`.

## 4. Technical Decisions
- Tuân thủ nghiêm ngặt `Interface + Impl` (`SystemSettingService` và `SystemSettingServiceImpl`).
- Repository hỗ trợ tra cứu `findBySettingKeyAndIsDeletedFalse`.

