package com.hmp.sales.feature.user.service;

import com.hmp.sales.common.exception.ResourceNotFoundException;
import com.hmp.sales.feature.user.dto.AddressRequest;
import com.hmp.sales.feature.user.dto.AddressResponse;
import com.hmp.sales.feature.user.entity.AddressStatus;
import com.hmp.sales.feature.user.entity.AuthProvider;
import com.hmp.sales.feature.user.entity.CustomerAddress;
import com.hmp.sales.feature.user.entity.User;
import com.hmp.sales.feature.user.entity.UserStatus;
import com.hmp.sales.feature.user.repository.CustomerAddressRepository;
import com.hmp.sales.feature.user.repository.UserRepository;
import com.hmp.sales.feature.user.service.impl.CustomerAddressServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Unit test cho {@link CustomerAddressServiceImpl}.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("CustomerAddressServiceImpl Unit Tests")
class CustomerAddressServiceImplTest {

    @Mock
    private CustomerAddressRepository customerAddressRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private CustomerAddressServiceImpl customerAddressService;

    private User sampleUser;
    private CustomerAddress sampleAddress;

    @BeforeEach
    void setUp() {
        sampleUser = User.builder()
                .id(1L)
                .fullName("Nguyễn Văn A")
                .email("user@example.com")
                .phone("0987654321")
                .authProvider(AuthProvider.GOOGLE)
                .status(UserStatus.ACTIVE)
                .isDeleted(false)
                .build();

        sampleAddress = CustomerAddress.builder()
                .id(10L)
                .customer(sampleUser)
                .addressLine("123 Đường Số 1, Phường 2, TP. Quy Nhơn")
                .isDefault(true)
                .status(AddressStatus.ACTIVE)
                .isDeleted(false)
                .build();
    }

    @Test
    @DisplayName("getAddresses_returnsList - Lấy danh sách địa chỉ thành công")
    void getAddresses_returnsList() {
        when(customerAddressRepository.findByCustomerIdAndIsDeletedFalseOrderByIsDefaultDescCreatedAtDesc(1L))
                .thenReturn(List.of(sampleAddress));

        List<AddressResponse> result = customerAddressService.getAddresses(1L);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).addressId()).isEqualTo(10L);
        assertThat(result.get(0).address()).isEqualTo("123 Đường Số 1, Phường 2, TP. Quy Nhơn");
        assertThat(result.get(0).isDefault()).isTrue();
    }

    @Test
    @DisplayName("getAddresses_returnsEmptyList - Khách hàng chưa có địa chỉ trả về danh sách rỗng")
    void getAddresses_returnsEmptyList() {
        when(customerAddressRepository.findByCustomerIdAndIsDeletedFalseOrderByIsDefaultDescCreatedAtDesc(1L))
                .thenReturn(Collections.emptyList());

        List<AddressResponse> result = customerAddressService.getAddresses(1L);

        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("createAddress_firstAddress_autoDefault - Địa chỉ đầu tiên tự động đặt làm mặc định")
    void createAddress_firstAddress_autoDefault() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(sampleUser));
        when(customerAddressRepository.countByCustomerIdAndIsDeletedFalse(1L)).thenReturn(0L);
        when(customerAddressRepository.save(any(CustomerAddress.class))).thenAnswer(invocation -> {
            CustomerAddress a = invocation.getArgument(0);
            a.setId(10L);
            return a;
        });

        AddressRequest request = new AddressRequest("456 Đường ABC, TP.HCM", false);

        AddressResponse response = customerAddressService.createAddress(1L, request);

        assertThat(response.isDefault()).isTrue();
        assertThat(response.address()).isEqualTo("456 Đường ABC, TP.HCM");
        verify(customerAddressRepository).resetDefaultAddress(1L);
    }

    @Test
    @DisplayName("createAddress_subsequentAddress_withoutDefault - Địa chỉ tiếp theo không chọn mặc định")
    void createAddress_subsequentAddress_withoutDefault() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(sampleUser));
        when(customerAddressRepository.countByCustomerIdAndIsDeletedFalse(1L)).thenReturn(1L);
        when(customerAddressRepository.save(any(CustomerAddress.class))).thenAnswer(invocation -> {
            CustomerAddress a = invocation.getArgument(0);
            a.setId(11L);
            return a;
        });

        AddressRequest request = new AddressRequest("789 Đường XYZ", false);

        AddressResponse response = customerAddressService.createAddress(1L, request);

        assertThat(response.address()).isEqualTo("789 Đường XYZ");
        assertThat(response.isDefault()).isFalse();
        verify(customerAddressRepository, never()).resetDefaultAddress(1L);
    }

    @Test
    @DisplayName("createAddress_userNotFound - Không tìm thấy user ném ResourceNotFoundException")
    void createAddress_userNotFound() {
        when(userRepository.findById(99L)).thenReturn(Optional.empty());

        AddressRequest request = new AddressRequest("123 Đường A", true);

        assertThatThrownBy(() -> customerAddressService.createAddress(99L, request))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Không tìm thấy người dùng ID: 99");
    }

    @Test
    @DisplayName("updateAddress_success - Cập nhật thông tin địa chỉ thành công")
    void updateAddress_success() {
        when(customerAddressRepository.findByIdAndCustomerIdAndIsDeletedFalse(10L, 1L))
                .thenReturn(Optional.of(sampleAddress));
        when(customerAddressRepository.save(any(CustomerAddress.class))).thenAnswer(invocation -> invocation.getArgument(0));

        AddressRequest request = new AddressRequest("Địa chỉ mới", true);

        AddressResponse response = customerAddressService.updateAddress(1L, 10L, request);

        assertThat(response.address()).isEqualTo("Địa chỉ mới");
        assertThat(response.isDefault()).isTrue();
        verify(customerAddressRepository).resetDefaultAddress(1L);
    }

    @Test
    @DisplayName("updateAddress_notFound_throwsResourceNotFoundException - Không tìm thấy địa chỉ ném ResourceNotFoundException")
    void updateAddress_notFound_throwsResourceNotFoundException() {
        when(customerAddressRepository.findByIdAndCustomerIdAndIsDeletedFalse(999L, 1L))
                .thenReturn(Optional.empty());

        AddressRequest request = new AddressRequest("Addr", false);

        assertThatThrownBy(() -> customerAddressService.updateAddress(1L, 999L, request))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Không tìm thấy địa chỉ ID: 999");
    }

    @Test
    @DisplayName("deleteAddress_wasDefault_promotesNextAddress - Xóa địa chỉ mặc định tự động chọn địa chỉ còn lại làm mặc định mới")
    void deleteAddress_wasDefault_promotesNextAddress() {
        when(customerAddressRepository.findByIdAndCustomerIdAndIsDeletedFalse(10L, 1L))
                .thenReturn(Optional.of(sampleAddress));

        CustomerAddress nextAddress = CustomerAddress.builder()
                .id(11L)
                .customer(sampleUser)
                .addressLine("Địa chỉ còn lại")
                .isDefault(false)
                .status(AddressStatus.ACTIVE)
                .isDeleted(false)
                .build();

        when(customerAddressRepository.findFirstByCustomerIdAndIsDeletedFalseOrderByCreatedAtDesc(1L))
                .thenReturn(Optional.of(nextAddress));

        customerAddressService.deleteAddress(1L, 10L);

        assertThat(sampleAddress.getIsDeleted()).isTrue();
        assertThat(sampleAddress.getStatus()).isEqualTo(AddressStatus.INACTIVE);
        assertThat(sampleAddress.getIsDefault()).isFalse();

        assertThat(nextAddress.getIsDefault()).isTrue();
        verify(customerAddressRepository).save(sampleAddress);
        verify(customerAddressRepository).save(nextAddress);
    }
}
