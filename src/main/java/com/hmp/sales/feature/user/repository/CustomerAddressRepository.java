package com.hmp.sales.feature.user.repository;

import com.hmp.sales.feature.user.entity.CustomerAddress;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository truy xuất dữ liệu sổ địa chỉ của khách hàng (CustomerAddress).
 */
@Repository
public interface CustomerAddressRepository extends JpaRepository<CustomerAddress, Long> {

    /**
     * Lấy toàn bộ danh sách địa chỉ đang hoạt động của khách hàng, sắp xếp theo mặc định trước rồi đến ngày tạo giảm dần.
     *
     * @param customerId ID khách hàng sở hữu
     * @return danh sách các địa chỉ chưa bị xóa mềm
     */
    List<CustomerAddress> findByCustomerIdAndIsDeletedFalseOrderByIsDefaultDescCreatedAtDesc(Long customerId);

    /**
     * Tìm một địa chỉ cụ thể theo ID và ID khách hàng sở hữu (đảm bảo tính cô lập dữ liệu).
     *
     * @param id         ID địa chỉ
     * @param customerId ID khách hàng sở hữu
     * @return {@link Optional} chứa địa chỉ nếu tìm thấy và chưa bị xóa mềm
     */
    Optional<CustomerAddress> findByIdAndCustomerIdAndIsDeletedFalse(Long id, Long customerId);

    /**
     * Tìm địa chỉ mới nhất còn hoạt động của khách hàng (dùng để gán mặc định thay thế khi xóa địa chỉ mặc định).
     *
     * @param customerId ID khách hàng sở hữu
     * @return {@link Optional} chứa địa chỉ mới nhất nếu có
     */
    Optional<CustomerAddress> findFirstByCustomerIdAndIsDeletedFalseOrderByCreatedAtDesc(Long customerId);

    /**
     * Đếm số lượng địa chỉ còn hoạt động của khách hàng.
     *
     * @param customerId ID khách hàng sở hữu
     * @return số lượng địa chỉ chưa bị xóa mềm
     */
    long countByCustomerIdAndIsDeletedFalse(Long customerId);

    /**
     * Bỏ đánh dấu mặc định ({@code isDefault = false}) của toàn bộ địa chỉ thuộc về khách hàng.
     *
     * @param customerId ID khách hàng sở hữu
     */
    @Modifying
    @Query("UPDATE CustomerAddress a SET a.isDefault = false WHERE a.customer.id = :customerId AND a.isDefault = true AND a.isDeleted = false")
    void resetDefaultAddress(@Param("customerId") Long customerId);
}

