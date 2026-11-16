package com.hmp.sales.feature.product.service.impl;

import com.hmp.sales.common.exception.BusinessRuleException;
import com.hmp.sales.common.exception.ResourceNotFoundException;
import com.hmp.sales.feature.product.dto.FeaturedProductCreateRequest;
import com.hmp.sales.feature.product.dto.FeaturedProductResponse;
import com.hmp.sales.feature.product.dto.FeaturedProductUpdateRequest;
import com.hmp.sales.feature.product.entity.FeaturedProduct;
import com.hmp.sales.feature.product.entity.FeaturedProductStatus;
import com.hmp.sales.feature.product.entity.Product;
import com.hmp.sales.feature.product.entity.ProductStatus;
import com.hmp.sales.feature.product.repository.FeaturedProductRepository;
import com.hmp.sales.feature.product.repository.ProductRepository;
import com.hmp.sales.feature.product.service.FeaturedProductService;
import com.hmp.sales.feature.systemlog.service.SystemLogService;
import com.hmp.sales.feature.user.entity.User;
import com.hmp.sales.feature.user.repository.UserRepository;
import com.hmp.sales.security.PermissionEvaluationService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Cài đặt service xử lý nghiệp vụ cho Sản phẩm nổi bật (Featured Products).
 * <p>
 * Đảm bảo các ràng buộc:
 * <ul>
 *   <li>Chỉ sản phẩm {@code ACTIVE} và chưa bị xóa mềm mới được phép đưa vào danh sách nổi bật.</li>
 *   <li>Mỗi sản phẩm chỉ tồn tại một bản ghi nổi bật có hiệu lực (chưa bị xóa mềm).</li>
 *   <li>Hỗ trợ phân quyền xem giá động qua {@link PermissionEvaluationService}.</li>
 *   <li>Ghi vết nhật ký hệ thống qua {@link SystemLogService}.</li>
 * </ul>
 */
@Slf4j
@Service
public class FeaturedProductServiceImpl implements FeaturedProductService {

    private final FeaturedProductRepository featuredProductRepository;
    private final ProductRepository productRepository;
    private final UserRepository userRepository;
    private final PermissionEvaluationService permissionEvaluationService;
    private final SystemLogService systemLogService;

    /**
     * Khởi tạo FeaturedProductServiceImpl với đầy đủ các phụ thuộc nghiệp vụ.
     *
     * @param featuredProductRepository  repository sản phẩm nổi bật
     * @param productRepository          repository sản phẩm
     * @param userRepository             repository người dùng
     * @param permissionEvaluationService service đánh giá quyền xem giá động
     * @param systemLogService           service ghi nhật ký hệ thống
     */
    @Autowired
    public FeaturedProductServiceImpl(
            FeaturedProductRepository featuredProductRepository,
            ProductRepository productRepository,
            UserRepository userRepository,
            @Autowired(required = false) PermissionEvaluationService permissionEvaluationService,
            @Autowired(required = false) SystemLogService systemLogService) {
        this.featuredProductRepository = featuredProductRepository;
        this.productRepository = productRepository;
        this.userRepository = userRepository;
        this.permissionEvaluationService = permissionEvaluationService;
        this.systemLogService = systemLogService;
    }

    /**
     * Khởi tạo FeaturedProductServiceImpl đơn giản (dành cho unit test).
     *
     * @param featuredProductRepository repository sản phẩm nổi bật
     * @param productRepository         repository sản phẩm
     */
    public FeaturedProductServiceImpl(
            FeaturedProductRepository featuredProductRepository,
            ProductRepository productRepository) {
        this(featuredProductRepository, productRepository, null, null, null);
    }

    /** {@inheritDoc} */
    @Override
    @Transactional(readOnly = true)
    public List<FeaturedProductResponse> getPublicFeaturedProducts() {
        boolean priceVisible = isPriceVisible();
        return featuredProductRepository
                .findByStatusAndIsDeletedFalseOrderByDisplayOrderAscCreatedAtDesc(FeaturedProductStatus.ACTIVE)
                .stream()
                .filter(fp -> fp.getProduct() != null && !Boolean.TRUE.equals(fp.getProduct().getIsDeleted()) && fp.getProduct().getStatus() == ProductStatus.ACTIVE)
                .map(fp -> FeaturedProductResponse.fromEntity(fp, priceVisible))
                .toList();
    }

    /** {@inheritDoc} */
    @Override
    @Transactional(readOnly = true)
    public List<FeaturedProductResponse> getAllFeaturedProducts() {
        boolean priceVisible = isPriceVisible();
        return featuredProductRepository
                .findByIsDeletedFalseOrderByDisplayOrderAscCreatedAtDesc()
                .stream()
                .map(fp -> FeaturedProductResponse.fromEntity(fp, priceVisible))
                .toList();
    }

    /** {@inheritDoc} */
    @Override
    @Transactional(readOnly = true)
    public FeaturedProductResponse getFeaturedProductById(Long id) {
        FeaturedProduct fp = featuredProductRepository.findByIdAndIsDeletedFalse(id)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy sản phẩm nổi bật với ID: " + id));
        return FeaturedProductResponse.fromEntity(fp, isPriceVisible());
    }

    /** {@inheritDoc} */
    @Override
    @Transactional
    public FeaturedProductResponse addFeaturedProduct(FeaturedProductCreateRequest request, Long actorId) {
        Product product = productRepository.findByIdAndIsDeletedFalse(request.productId())
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy sản phẩm với ID: " + request.productId()));

        if (product.getStatus() != ProductStatus.ACTIVE) {
            throw new BusinessRuleException("Chỉ có thể thêm sản phẩm đang ở trạng thái ACTIVE vào danh sách sản phẩm nổi bật.");
        }

        if (featuredProductRepository.existsByProductIdAndIsDeletedFalse(request.productId())) {
            throw new BusinessRuleException("Sản phẩm '" + product.getProductName() + "' đã tồn tại trong danh sách nổi bật.");
        }

        User creator = null;
        if (actorId != null && userRepository != null) {
            creator = userRepository.findById(actorId).orElse(null);
        }

        FeaturedProduct featuredProduct = FeaturedProduct.builder()
                .product(product)
                .displayOrder(request.displayOrder() != null ? request.displayOrder() : 0)
                .status(request.status() != null ? request.status() : FeaturedProductStatus.ACTIVE)
                .createdBy(creator)
                .isDeleted(false)
                .build();

        FeaturedProduct saved = featuredProductRepository.save(featuredProduct);

        if (systemLogService != null && actorId != null) {
            systemLogService.log(actorId, "FEATURED_PRODUCT_ADDED", "FEATURED_PRODUCT", saved.getId(),
                    "Thêm sản phẩm ID=" + product.getId() + " (" + product.getProductName() + ") vào danh sách nổi bật");
        }

        log.info("[FeaturedProductService] Đã thêm sản phẩm ID={} vào danh sách nổi bật (featured_id={})", product.getId(), saved.getId());
        return FeaturedProductResponse.fromEntity(saved, isPriceVisible());
    }

    /** {@inheritDoc} */
    @Override
    @Transactional
    public FeaturedProductResponse updateFeaturedProduct(Long id, FeaturedProductUpdateRequest request, Long actorId) {
        FeaturedProduct fp = featuredProductRepository.findByIdAndIsDeletedFalse(id)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy sản phẩm nổi bật với ID: " + id));

        if (request.displayOrder() != null) {
            fp.setDisplayOrder(request.displayOrder());
        }
        if (request.status() != null) {
            fp.setStatus(request.status());
        }

        FeaturedProduct updated = featuredProductRepository.save(fp);

        if (systemLogService != null && actorId != null) {
            systemLogService.log(actorId, "FEATURED_PRODUCT_UPDATED", "FEATURED_PRODUCT", updated.getId(),
                    "Cập nhật sản phẩm nổi bật ID=" + id);
        }

        log.info("[FeaturedProductService] Đã cập nhật sản phẩm nổi bật ID={}", id);
        return FeaturedProductResponse.fromEntity(updated, isPriceVisible());
    }

    /** {@inheritDoc} */
    @Override
    @Transactional
    public void deleteFeaturedProduct(Long id, Long actorId) {
        FeaturedProduct fp = featuredProductRepository.findByIdAndIsDeletedFalse(id)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy sản phẩm nổi bật với ID: " + id));

        fp.setIsDeleted(true);
        featuredProductRepository.save(fp);

        if (systemLogService != null && actorId != null) {
            systemLogService.log(actorId, "FEATURED_PRODUCT_DELETED", "FEATURED_PRODUCT", id,
                    "Xóa sản phẩm nổi bật ID=" + id);
        }

        log.info("[FeaturedProductService] Đã xóa mềm sản phẩm nổi bật ID={}", id);
    }

    private boolean isPriceVisible() {
        return permissionEvaluationService != null && permissionEvaluationService.canViewProductPrice();
    }
}
