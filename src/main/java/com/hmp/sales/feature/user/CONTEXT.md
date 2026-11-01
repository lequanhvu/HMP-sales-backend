# User & Identity Feature Context

## 1. Overview
Phân hệ quản lý định danh, người dùng, vai trò và phân quyền (Identity, Users, Roles & Permissions) trong hệ thống bán hàng HMP.

## 2. Entities & Schema Mapping
Các thực thể JPA đã triển khai tại `com.hmp.sales.feature.user.entity`:
- `Role`: Đại diện bảng `roles`. Định danh các vai trò chính: `ADMIN`, `MANAGER`, `CUSTOMER`.
- `Permission`: Đại diện bảng `permissions`. Quản lý quyền hạn động dựa trên `api_path`, `method` và đặc thù như `PRODUCT_PRICE_VIEW`.
- `RolePermission`: Đại diện bảng liên kết `role_permissions` với composite key `RolePermissionId`.
- `User`: Đại diện bảng `users`. Quản lý tài khoản hỗ trợ cả xác thực Google OAuth (`GOOGLE`) và xác thực nội bộ (`LOCAL`), trạng thái `ACTIVE` / `LOCKED`.

## 3. Enums
- `RoleCode`: `ADMIN`, `MANAGER`, `CUSTOMER`.
- `AuthProvider`: `GOOGLE`, `LOCAL`.
- `UserStatus`: `ACTIVE`, `LOCKED`.

## 4. Repositories
Các repository tại `com.hmp.sales.feature.user.repository`:
- `RoleRepository`: Truy vấn vai trò theo mã định danh `RoleCode`, lọc xóa mềm `isDeleted`.
- `PermissionRepository`: Truy vấn quyền hạn theo mã tên quyền, `apiPath` + `method`, và lọc theo module.
- `RolePermissionRepository`: Truy vấn quyền hạn gán cho vai trò (`findPermissionsByRoleId`), kiểm tra nhanh quyền hạn của vai trò (`hasPermission`), và lấy toàn bộ liên kết quyền hợp lệ eager load để nạp cache RAM (`findAllActiveRolePermissions`).
- `UserRepository`: Truy vấn người dùng theo `email`, `authProvider` + `providerSubject` (Google sub), eager fetch vai trò (`findByIdWithRole`, `findByEmailWithRole`).

## 5. Security & Authorization Integration
- `PermissionAuthorizationManager`: Singleton quản lý cache `rolePermissionsCache` trên RAM, kiểm tra quyền truy cập động bằng `AntPathMatcher` trên `(apiPath, method)`.
- `PermissionEvaluationServiceImpl`: Tích hợp tra cứu quyền từ cache RAM qua `PermissionAuthorizationManager`.
- `RoleServiceImpl`: Tự động gọi `loadCache()` khi Admin cập nhật quyền hạn cho vai trò.

## 6. Testing
Tất cả các repository và service được kiểm thử tự động với `@DataJpaTest` và Mockito:
- `RoleRepositoryTest` (3 tests)
- `PermissionRepositoryTest` (3 tests)
- `RolePermissionRepositoryTest` (3 tests)
- `UserRepositoryTest` (4 tests)
- `AdminUserServiceTest` (11 tests)
- `AdminRoleServiceTest` (3 tests)
- `PermissionAuthorizationManagerTest` (9 tests)
- `PermissionEvaluationServiceTest` (7 tests)
Toàn bộ 102/102 tests trong dự án đều PASS 100%.

## 7. Soft Delete Policy & Unique Constraint Protection
- Khi xóa mềm người dùng (`isDeleted = true`), hệ thống sinh một mã UUID ngẫu nhiên và gắn hậu tố `_<UUID>` vào các trường có ràng buộc duy nhất hoặc thông tin liên lạc nhạy cảm: `email`, `phone` (nếu có), và `providerSubject` (nếu có).
- Lưu lại vào cơ sở dữ liệu trước khi chuyển cờ `isDeleted = true` và `status = LOCKED`.
- Việc này giúp giải phóng các ràng buộc `uk_users_email` và `uk_users_auth_provider_subject`, đảm bảo khách hàng có thể đăng ký hoặc đăng nhập lại bằng email, số điện thoại hoặc tài khoản Google cũ mà không bị lỗi trùng khóa (duplicate key exception).

## 8. Customer Profile & Address Book (Phase 4 — Step 1)

### Business Context
Khách hàng cần quản lý thông tin cá nhân (họ tên, số điện thoại) và lưu danh sách sổ địa chỉ giao hàng (`CustomerAddress`) phục vụ việc chọn nhanh địa chỉ khi đặt hàng, đồng thời đảm bảo thông tin đơn hàng lịch sử là snapshot bất biến.

### Technical Decisions
- **`CustomerAddress` Entity**: Lưu tại bảng `customer_addresses`, liên kết `@ManyToOne` với `User(customer_id)`, chứa `addressLine`, `isDefault`, `status` (`AddressStatus`), `isDeleted`. Loại bỏ `receiverName` và `receiverPhone` khỏi sổ địa chỉ để tinh gọn mô hình (thông tin người nhận sẽ được nhập/snapshot trực tiếp trên `Order`).
- **Độc quyền địa chỉ mặc định (`isDefault`)**: Khi thêm mới hoặc cập nhật một địa chỉ với `isDefault = true`, câu truy vấn `@Modifying resetDefaultAddress` tự động xóa cờ mặc định của các địa chỉ cũ cùng khách hàng. Nếu khách hàng tạo địa chỉ đầu tiên, hệ thống tự động gán `isDefault = true`.
- **Kế thừa mặc định khi xóa địa chỉ**: Khi khách hàng xóa mềm địa chỉ đang là mặc định, hệ thống tự động chọn địa chỉ còn lại mới nhất (`findFirstByCustomerIdAndIsDeletedFalseOrderByCreatedAtDesc`) làm mặc định thay thế.
- **Ràng buộc OTP khi đổi số điện thoại (`PATCH /api/v1/me`)**: Nếu khách hàng nhập số điện thoại mới khác số hiện tại, bắt buộc phải có bảo chứng xác thực OTP (`phoneOtpService.isVerified(phone)` trên Redis); nếu giữ nguyên số điện thoại cũ hoặc chỉ đổi họ tên thì không yêu cầu OTP.
- **Tách biệt Service**: Tách `UserProfileService` (quản lý profile) và `CustomerAddressService` (quản lý sổ địa chỉ) theo Interface + Impl pattern, controller mỏng `MeController` tại `/api/v1/me`.
- **Dynamic Permission Integration**: Đăng ký các quyền `ME_PROFILE_VIEW`, `ME_PROFILE_UPDATE`, `ME_ADDRESS_VIEW`, `ME_ADDRESS_CREATE`, `ME_ADDRESS_UPDATE`, `ME_ADDRESS_DELETE` vào `DataInitializer` và nạp vào bộ nhớ RAM của `PermissionAuthorizationManager`.

### Considered and Rejected
- **Lưu địa chỉ trực tiếp trên Order**: Bị loại bỏ vì khách hàng cần lưu sổ địa chỉ để tái sử dụng nhiều lần; thay vào đó, địa chỉ được lưu tại `CustomerAddress` và copy snapshot sang `Order` khi đặt hàng.
- **Hard delete địa chỉ**: Bị loại bỏ để bảo toàn tính toàn vẹn và khả năng kiểm toán nếu có tham chiếu.

### Dependencies
- Phụ thuộc: `User` entity, `UserRepository`, `PhoneOtpService`, `SecurityUtils`, `GlobalExceptionHandler`.
- Được phụ thuộc bởi: Module Order (`feature/order`) khi tạo đơn hàng sẽ đọc sổ địa chỉ của khách hàng.

### Refactor Log

#### 2026-11-02 | @antigravity
- Triển khai Step 1 Phase 4: Customer Profile APIs (`GET/PATCH /api/v1/me`) và Customer Address Book CRUD (`GET/POST/PATCH/DELETE /api/v1/me/addresses`).
- Thêm `CustomerAddress`, `CustomerAddressRepository`, `UserProfileService`, `CustomerAddressService`, `MeController`.
- Bổ sung 23 unit & integration test cases (`UserProfileServiceImplTest`, `CustomerAddressServiceImplTest`, `MeControllerTest`), toàn bộ 254/254 tests pass.
- Refactor tinh giản `CustomerAddress`: Xóa bỏ 2 trường `receiverName` và `receiverPhone` khỏi `CustomerAddress`, DTOs (`AddressRequest`, `AddressResponse`) và đồng bộ lại `api-spec.md` Section 6 (6.4, 6.5, 6.6) cùng `database.md` Table 16.

