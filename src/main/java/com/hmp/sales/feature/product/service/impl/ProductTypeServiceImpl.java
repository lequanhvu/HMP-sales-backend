package com.hmp.sales.feature.product.service.impl;

import com.hmp.sales.common.exception.ResourceNotFoundException;
import com.hmp.sales.feature.product.dto.ProductTypeResponse;
import com.hmp.sales.feature.product.entity.ProductType;
import com.hmp.sales.feature.product.repository.ProductTypeRepository;
import com.hmp.sales.feature.product.service.ProductTypeService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Triển khai dịch vụ truy xuất loại sản phẩm kỹ thuật.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ProductTypeServiceImpl implements ProductTypeService {

    private final ProductTypeRepository productTypeRepository;

    @Override
    public List<ProductTypeResponse> getAllProductTypes() {
        return productTypeRepository.findAll().stream()
                .map(ProductTypeResponse::fromEntity)
                .collect(Collectors.toList());
    }

    @Override
    public ProductType getByCode(String code) {
        return productTypeRepository.findByCode(code)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy loại sản phẩm với mã: " + code));
    }

    @Override
    public ProductType getById(Long id) {
        return productTypeRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy loại sản phẩm với ID: " + id));
    }
}
