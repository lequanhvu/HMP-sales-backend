package com.hmp.sales.feature.user.service.impl;

import com.hmp.sales.common.exception.ResourceNotFoundException;
import com.hmp.sales.feature.user.dto.AddressRequest;
import com.hmp.sales.feature.user.dto.AddressResponse;
import com.hmp.sales.feature.user.entity.AddressStatus;
import com.hmp.sales.feature.user.entity.CustomerAddress;
import com.hmp.sales.feature.user.entity.User;
import com.hmp.sales.feature.user.repository.CustomerAddressRepository;
import com.hmp.sales.feature.user.repository.UserRepository;
import com.hmp.sales.feature.user.service.CustomerAddressService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Triển khai dịch vụ quản lý sổ địa chỉ giao nhận hàng của khách hàng (CustomerAddressService).
 */
@Slf4j
@Service
public class CustomerAddressServiceImpl implements CustomerAddressService {

    private final CustomerAddressRepository customerAddressRepository;
    private final UserRepository userRepository;

    /**
     * Khởi tạo CustomerAddressServiceImpl với các repository cần thiết.
     *
     * @param customerAddressRepository repository địa chỉ khách hàng
     * @param userRepository            repository người dùng
     */
    public CustomerAddressServiceImpl(CustomerAddressRepository customerAddressRepository,
                                      UserRepository userRepository) {
        this.customerAddressRepository = customerAddressRepository;
        this.userRepository = userRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public List<AddressResponse> getAddresses(Long userId) {
        return customerAddressRepository
                .findByCustomerIdAndIsDeletedFalseOrderByIsDefaultDescCreatedAtDesc(userId)
                .stream()
                .map(AddressResponse::fromEntity)
                .toList();
    }

    @Override
    @Transactional
    public AddressResponse createAddress(Long userId, AddressRequest request) {
        User user = findActiveUser(userId);

        long activeCount = customerAddressRepository.countByCustomerIdAndIsDeletedFalse(userId);
        boolean shouldBeDefault = Boolean.TRUE.equals(request.isDefault()) || activeCount == 0;

        if (shouldBeDefault) {
            customerAddressRepository.resetDefaultAddress(userId);
        }

        CustomerAddress address = CustomerAddress.builder()
                .customer(user)
                .addressLine(request.address().trim())
                .isDefault(shouldBeDefault)
                .status(AddressStatus.ACTIVE)
                .isDeleted(false)
                .build();

        CustomerAddress saved = customerAddressRepository.save(address);
        log.info("[CustomerAddressService] Đã thêm địa chỉ ID: {} cho khách hàng ID: {}", saved.getId(), userId);
        return AddressResponse.fromEntity(saved);
    }

    @Override
    @Transactional
    public AddressResponse updateAddress(Long userId, Long addressId, AddressRequest request) {
        CustomerAddress address = findAddress(addressId, userId);

        if (Boolean.TRUE.equals(request.isDefault())) {
            customerAddressRepository.resetDefaultAddress(userId);
            address.setIsDefault(true);
        } else if (Boolean.FALSE.equals(request.isDefault()) && Boolean.TRUE.equals(address.getIsDefault())) {
            address.setIsDefault(false);
        }

        if (request.address() != null && !request.address().isBlank()) {
            address.setAddressLine(request.address().trim());
        }

        CustomerAddress saved = customerAddressRepository.save(address);
        log.info("[CustomerAddressService] Đã cập nhật địa chỉ ID: {} cho khách hàng ID: {}", saved.getId(), userId);
        return AddressResponse.fromEntity(saved);
    }

    @Override
    @Transactional
    public void deleteAddress(Long userId, Long addressId) {
        CustomerAddress address = findAddress(addressId, userId);

        boolean wasDefault = Boolean.TRUE.equals(address.getIsDefault());

        address.setIsDeleted(true);
        address.setStatus(AddressStatus.INACTIVE);
        address.setIsDefault(false);
        customerAddressRepository.save(address);

        if (wasDefault) {
            customerAddressRepository.findFirstByCustomerIdAndIsDeletedFalseOrderByCreatedAtDesc(userId)
                    .ifPresent(nextDefault -> {
                        nextDefault.setIsDefault(true);
                        customerAddressRepository.save(nextDefault);
                        log.info("[CustomerAddressService] Đã tự động chọn địa chỉ ID: {} làm mặc định mới cho khách hàng ID: {}",
                                nextDefault.getId(), userId);
                    });
        }

        log.info("[CustomerAddressService] Đã xóa mềm địa chỉ ID: {} của khách hàng ID: {}", addressId, userId);
    }

    private User findActiveUser(Long userId) {
        if (userId == null) {
            throw new ResourceNotFoundException("Không tìm thấy người dùng");
        }
        return userRepository.findById(userId)
                .filter(u -> !Boolean.TRUE.equals(u.getIsDeleted()))
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy người dùng ID: " + userId));
    }

    private CustomerAddress findAddress(Long addressId, Long userId) {
        if (addressId == null || userId == null) {
            throw new ResourceNotFoundException("Không tìm thấy địa chỉ");
        }
        return customerAddressRepository.findByIdAndCustomerIdAndIsDeletedFalse(addressId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy địa chỉ ID: " + addressId));
    }
}

