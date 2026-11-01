package com.hmp.sales.feature.user.service;

import com.hmp.sales.feature.user.dto.AddressRequest;
import com.hmp.sales.feature.user.dto.AddressResponse;

import java.util.List;

/**
 * Service quản lý sổ địa chỉ giao nhận hàng của khách hàng (Customer Address).
 */
public interface CustomerAddressService {

    /**
     * Lấy toàn bộ danh sách địa chỉ đang hoạt động của khách hàng.
     *
     * @param userId ID khách hàng đang đăng nhập
     * @return danh sách các địa chỉ {@link AddressResponse}
     */
    List<AddressResponse> getAddresses(Long userId);

    /**
     * Thêm mới một địa chỉ giao nhận hàng cho khách hàng.
     * <p>
     * Nếu đặt cờ {@code isDefault = true}, hệ thống sẽ tự động bỏ cờ mặc định của các địa chỉ cũ.
     * Nếu đây là địa chỉ đầu tiên của khách hàng, nó sẽ được tự động thiết lập làm địa chỉ mặc định.
     *
     * @param userId  ID khách hàng
     * @param request dữ liệu địa chỉ mới
     * @return thông tin địa chỉ vừa tạo {@link AddressResponse}
     * @throws com.hmp.sales.common.exception.ResourceNotFoundException nếu không tìm thấy khách hàng
     */
    AddressResponse createAddress(Long userId, AddressRequest request);

    /**
     * Cập nhật thông tin một địa chỉ đã có của khách hàng.
     *
     * @param userId    ID khách hàng
     * @param addressId ID địa chỉ cần cập nhật
     * @param request   dữ liệu cập nhật
     * @return thông tin địa chỉ sau khi cập nhật {@link AddressResponse}
     * @throws com.hmp.sales.common.exception.ResourceNotFoundException nếu không tìm thấy địa chỉ hoặc không thuộc về khách hàng
     */
    AddressResponse updateAddress(Long userId, Long addressId, AddressRequest request);

    /**
     * Xóa mềm một địa chỉ khỏi sổ địa chỉ của khách hàng.
     * <p>
     * Nếu địa chỉ bị xóa là địa chỉ mặc định và khách hàng còn các địa chỉ khác, địa chỉ gần nhất
     * còn lại sẽ được tự động đặt làm mặc định thay thế.
     *
     * @param userId    ID khách hàng
     * @param addressId ID địa chỉ cần xóa
     * @throws com.hmp.sales.common.exception.ResourceNotFoundException nếu không tìm thấy địa chỉ hoặc không thuộc về khách hàng
     */
    void deleteAddress(Long userId, Long addressId);
}

