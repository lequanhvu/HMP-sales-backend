package com.hmp.sales.feature.product.repository;

import com.hmp.sales.feature.product.entity.ProductType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Repository truy xuất dữ liệu loại sản phẩm {@link ProductType}.
 */
@Repository
public interface ProductTypeRepository extends JpaRepository<ProductType, Long> {

    /**
     * Tìm loại sản phẩm theo mã code.
     *
     * @param code mã định danh loại sản phẩm
     * @return Optional chứa ProductType nếu tìm thấy
     */
    Optional<ProductType> findByCode(String code);

    /**
     * Kiểm tra tồn tại loại sản phẩm theo mã code.
     *
     * @param code mã định danh loại sản phẩm
     * @return true nếu đã tồn tại
     */
    boolean existsByCode(String code);
}
