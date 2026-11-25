package com.hmp.sales.feature.product.service;

import com.hmp.sales.feature.product.dto.ProductTypeResponse;
import com.hmp.sales.feature.product.entity.ProductType;

import java.util.List;

/**
 * Interface dịch vụ quản lý loại sản phẩm kỹ thuật {@link ProductType}.
 */
public interface ProductTypeService {

    /**
     * Lấy danh sách tất cả các loại sản phẩm đang được hỗ trợ trong hệ thống.
     *
     * @return danh sách DTO ProductTypeResponse
     */
    List<ProductTypeResponse> getAllProductTypes();

    /**
     * Lấy thực thể ProductType theo mã code.
     *
     * @param code mã code (ví dụ: ROOFING_SHEET, GUTTER, RETAIL)
     * @return thực thể ProductType
     */
    ProductType getByCode(String code);

    /**
     * Lấy thực thể ProductType theo ID.
     *
     * @param id ID loại sản phẩm
     * @return thực thể ProductType
     */
    ProductType getById(Long id);
}
