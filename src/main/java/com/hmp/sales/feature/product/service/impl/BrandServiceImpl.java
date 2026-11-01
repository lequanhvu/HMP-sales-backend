package com.hmp.sales.feature.product.service.impl;

import com.hmp.sales.common.exception.BusinessRuleException;
import com.hmp.sales.common.exception.ResourceNotFoundException;
import com.hmp.sales.common.util.VietnameseStringUtils;
import com.hmp.sales.feature.product.dto.BrandRequest;
import com.hmp.sales.feature.product.dto.BrandResponse;
import com.hmp.sales.feature.product.entity.Brand;
import com.hmp.sales.feature.product.entity.BrandStatus;
import com.hmp.sales.feature.product.repository.BrandRepository;
import com.hmp.sales.feature.product.service.BrandService;
import com.hmp.sales.feature.search.dto.SearchResourceType;
import com.hmp.sales.feature.search.event.SearchIndexInvalidationEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Triển khai nghiệp vụ quản lý thương hiệu sản phẩm (Brand) cho Admin.
 * Bao gồm CRUD đầy đủ, tìm kiếm/phân trang, khóa/mở khóa và dropdown công khai.
 */
@Slf4j
@Service
public class BrandServiceImpl implements BrandService {

    private final BrandRepository brandRepository;
    private ApplicationEventPublisher eventPublisher;

    @Autowired(required = false)
    public void setEventPublisher(ApplicationEventPublisher eventPublisher) {
        this.eventPublisher = eventPublisher;
    }

    private void publishSearchInvalidation(Long targetId, SearchIndexInvalidationEvent.Operation op) {
        if (eventPublisher != null) {
            try {
                eventPublisher.publishEvent(new SearchIndexInvalidationEvent(SearchResourceType.BRAND, targetId, op));
            } catch (Exception e) {
                log.debug("[BrandService] Không thể bắn event search index sync: {}", e.getMessage());
            }
        }
    }

    /**
     * Khởi tạo BrandServiceImpl với repository thương hiệu.
     *
     * @param brandRepository repository quản lý thương hiệu
     */
    public BrandServiceImpl(BrandRepository brandRepository) {
        this.brandRepository = brandRepository;
    }

    /** {@inheritDoc} */
    @Override
    @Transactional(readOnly = true)
    public Page<BrandResponse> searchBrands(String keyword, String status, int page, int size) {
        BrandStatus brandStatus = parseStatus(status, BrandStatus.class);
        var pageable = PageRequest.of(page, size, Sort.by("brandName").ascending());
        String normalizedKeyword = VietnameseStringUtils.toUnaccentLowerCase(keyword);
        return brandRepository.searchBrands(normalizedKeyword, brandStatus, pageable)
                .map(BrandResponse::fromEntity);
    }

    /** {@inheritDoc} */
    @Override
    @Transactional(readOnly = true)
    public BrandResponse getBrandById(Long brandId) {
        return BrandResponse.fromEntity(findActiveBrand(brandId));
    }

    /** {@inheritDoc} */
    @Override
    @Transactional
    public BrandResponse createBrand(BrandRequest request) {
        if (brandRepository.existsByBrandNameAndIsDeletedFalse(request.brandName())) {
            throw new BusinessRuleException("Tên thương hiệu '" + request.brandName() + "' đã tồn tại.");
        }
        Brand brand = Brand.builder().brandName(request.brandName()).build();
        brand = brandRepository.save(brand);
        log.info("[BrandService] Tạo thương hiệu: {} (ID={})", brand.getBrandName(), brand.getId());
        publishSearchInvalidation(brand.getId(), SearchIndexInvalidationEvent.Operation.UPSERT);
        return BrandResponse.fromEntity(brand);
    }

    /** {@inheritDoc} */
    @Override
    @Transactional
    public BrandResponse updateBrand(Long brandId, BrandRequest request) {
        Brand brand = findActiveBrand(brandId);
        if (brandRepository.existsByBrandNameAndIsDeletedFalseAndIdNot(request.brandName(), brandId)) {
            throw new BusinessRuleException("Tên thương hiệu '" + request.brandName() + "' đã tồn tại.");
        }
        brand.setBrandName(request.brandName());
        brand = brandRepository.save(brand);
        publishSearchInvalidation(brand.getId(), SearchIndexInvalidationEvent.Operation.UPSERT);
        return BrandResponse.fromEntity(brand);
    }

    /** {@inheritDoc} */
    @Override
    @Transactional
    public BrandResponse lockBrand(Long brandId) {
        Brand brand = findActiveBrand(brandId);
        brand.setStatus(BrandStatus.LOCKED);
        brand = brandRepository.save(brand);
        publishSearchInvalidation(brandId, SearchIndexInvalidationEvent.Operation.DELETE);
        return BrandResponse.fromEntity(brand);
    }

    /** {@inheritDoc} */
    @Override
    @Transactional
    public BrandResponse unlockBrand(Long brandId) {
        Brand brand = findActiveBrand(brandId);
        brand.setStatus(BrandStatus.ACTIVE);
        brand = brandRepository.save(brand);
        publishSearchInvalidation(brandId, SearchIndexInvalidationEvent.Operation.UPSERT);
        return BrandResponse.fromEntity(brand);
    }

    /** {@inheritDoc} */
    @Override
    @Transactional
    public void deleteBrand(Long brandId) {
        Brand brand = findActiveBrand(brandId);
        if (brandRepository.hasActiveProducts(brandId)) {
            throw new BusinessRuleException("Không thể xóa thương hiệu vì còn sản phẩm đang liên kết.");
        }
        brand.setIsDeleted(true);
        brandRepository.save(brand);
        publishSearchInvalidation(brandId, SearchIndexInvalidationEvent.Operation.DELETE);
        log.info("[BrandService] Xóa mềm thương hiệu ID={}", brandId);
    }

    /** {@inheritDoc} */
    @Override
    @Transactional(readOnly = true)
    public List<BrandResponse> getActiveBrands() {
        return brandRepository.findByStatusAndIsDeletedFalseOrderByBrandNameAsc(BrandStatus.ACTIVE)
                .stream().map(BrandResponse::fromEntity).collect(Collectors.toList());
    }

    // -------------------------------------------------------------------------
    // Helper methods
    // -------------------------------------------------------------------------

    private Brand findActiveBrand(Long brandId) {
        return brandRepository.findByIdAndIsDeletedFalse(brandId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy thương hiệu ID: " + brandId));
    }

    private <E extends Enum<E>> E parseStatus(String status, Class<E> enumClass) {
        if (status == null || status.isBlank()) return null;
        try {
            return Enum.valueOf(enumClass, status.toUpperCase());
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
}
