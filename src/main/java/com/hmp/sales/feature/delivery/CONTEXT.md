# Delivery Feature Context

## Mục đích và Phạm vi
Module `delivery` cung cấp các dịch vụ kiểm tra vị trí địa lý, tính toán khoảng cách và xác thực bán kính giao hàng cho xưởng gia công tôn thép HMP.
- **Quy mô kinh doanh:** Phục vụ giao hàng tận nơi (`SHIPPING`) trong phạm vi bán kính giới hạn quanh xưởng (cấu hình động qua DB, mặc định 25.0 km thuộc địa bàn huyện Lập Thạch, Vĩnh Phúc).
- **Hỗ trợ khách ngoài bán kính:** Cho phép chọn phương thức "Khách tự bốc hàng tại xưởng" (`PICKUP`), không giới hạn khoảng cách và cung cấp thông tin hotline liên hệ điều phối.

## Kiến trúc và Thành phần
1. **Utility:**
   - `com.hmp.sales.common.util.GeoUtils`:
     - `calculateDistanceKm(lat1, lon1, lat2, lon2)`: Công thức Haversine thuần túy, tính chính xác khoảng cách trắc địa trên mặt cong Trái Đất (bán kính R = 6371 km), làm tròn 2 chữ số thập phân.
     - `isWithinRadius(lat1, lon1, lat2, lon2, maxRadiusKm)`: So sánh trực tiếp khoảng cách tính được với bán kính cho phép.

2. **DTOs:**
   - `StoreInfoResponse`: Tên xưởng, địa chỉ, tọa độ tâm (latitude, longitude), bán kính phục vụ tối đa (km), số điện thoại hotline.
   - `DeliveryDistanceResponse`: Khoảng cách tính toán (km), bán kính tối đa (km), trạng thái trong bán kính (`withinRadius`), thông tin liên hệ xưởng.

3. **Services:**
   - `DeliveryService` / `DeliveryServiceImpl`:
     - Đọc cấu hình động từ `system_settings`: `STORE_NAME`, `STORE_ADDRESS`, `STORE_LATITUDE`, `STORE_LONGITUDE`, `STORE_MAX_DELIVERY_RADIUS_KM`, `STORE_HOTLINE`.
     - `getStoreInfo()`: Trả về thông tin xưởng công khai.
     - `calculateDistanceToStore(customerLat, customerLng)`: Tính khoảng cách và trả về kết quả kiểm tra.
     - `validateDeliveryLocation(customerLat, customerLng)`: Quăng ngoại lệ `BadRequestException` với thông điệp rõ ràng kèm hotline nếu vị trí nằm ngoài bán kính phục vụ.

4. **Controllers:**
   - `PublicDeliveryController`: Public endpoint không yêu cầu xác thực (`/api/v1/delivery/store-info`, `/api/v1/delivery/check-distance`).

5. **Tích hợp với Đơn hàng (Order Feature):**
   - `CustomerOrderServiceImpl`:
     - Với `deliveryMethod == SHIPPING`: Bắt buộc địa chỉ phải có tọa độ `latitude` và `longitude`, gọi `deliveryService.validateDeliveryLocation(...)`. Lưu `deliveryMethod`, `deliveryLatitude`, `deliveryLongitude`, `deliveryDistanceKm` vào entity `Order`.
     - Với `deliveryMethod == PICKUP`: Bỏ qua kiểm tra bán kính, khách hàng tự vận chuyển tôn.
