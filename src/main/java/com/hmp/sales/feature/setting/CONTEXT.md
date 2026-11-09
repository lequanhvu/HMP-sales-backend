# System Setting Feature Context

## 1. Overview
Phân hệ quản lý tham số cấu hình động hệ thống (`SystemSetting`) tại `com.hmp.sales.feature.setting`.
Cho phép Quản trị viên (Admin) cấu hình các tham số vận hành nghiệp vụ mà không cần sửa code hay khởi động lại ứng dụng.

## 2. Entities & Schema Mapping
- `SystemSetting`: Đại diện bảng `system_settings`.
  - Khóa chính `setting_id` (`Long`).
  - Khóa cấu hình duy nhất `setting_key` (`VARCHAR(100)`).
  - Giá trị cấu hình `setting_value` (`VARCHAR(1000)`).
  - Kiểu dữ liệu `data_type` (`VARCHAR(30)`): `INT`, `BOOLEAN`, `DECIMAL`, `STRING`.
  - Audit: `updated_by` (FK `users`), `updated_at`, `is_deleted`.

## 3. Core Settings
- `ORDER_CUSTOMER_CANCELLATION_WINDOW_MINUTES`: Thời gian tối đa (phút) khách hàng được phép hủy đơn hàng sau khi tạo đơn hoặc sau khi Quản lý chốt giá.
  - Mặc định: `"30"`. Fallback: `app.order.cancellation-window-minutes`.
- `ORDER_UNDO_CANCEL_WINDOW_MINUTES`: Thời gian tối đa (phút) khách hàng được phép hoàn tác hủy đơn hàng.
  - Mặc định: `"60"`. Fallback: `app.order.undo-cancel-window-minutes`.

## 4. Endpoints & Security
- `GET /api/v1/admin/settings`: Xem danh sách toàn bộ cấu hình hệ thống (yêu cầu quyền `SYSTEM_SETTING_VIEW`, dành cho `ROLE_ADMIN`).
- `PUT /api/v1/admin/settings/{key}`: Cập nhật giá trị cấu hình (yêu cầu quyền `SYSTEM_SETTING_UPDATE`, dành cho `ROLE_ADMIN`).
  - Validation chặt chẽ theo `dataType`. Các khóa cửa sổ thời gian bắt buộc là số nguyên dương.
  - Mọi thay đổi đều được ghi audit trail vào `system_logs` (`SYSTEM_SETTING_UPDATED`) cùng giao dịch.

## 5. Technical Decisions
- Tuân thủ nghiêm ngặt `Interface + Impl` (`SystemSettingService` và `SystemSettingServiceImpl`).
- Repository hỗ trợ tra cứu `findBySettingKeyAndIsDeletedFalse` và `findAllByIsDeletedFalseOrderBySettingKeyAsc`.
