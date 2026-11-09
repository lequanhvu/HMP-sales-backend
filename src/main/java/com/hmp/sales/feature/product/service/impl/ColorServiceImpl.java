package com.hmp.sales.feature.product.service.impl;

import com.hmp.sales.common.exception.BusinessRuleException;
import com.hmp.sales.common.exception.ResourceNotFoundException;
import com.hmp.sales.common.util.AuditValueUtils;
import com.hmp.sales.common.util.SecurityUtils;
import com.hmp.sales.common.util.VietnameseStringUtils;
import com.hmp.sales.feature.product.dto.ColorRequest;
import com.hmp.sales.feature.product.dto.ColorResponse;
import com.hmp.sales.feature.product.entity.Color;
import com.hmp.sales.feature.product.entity.ColorStatus;
import com.hmp.sales.feature.product.repository.ColorRepository;
import com.hmp.sales.feature.product.service.ColorService;
import com.hmp.sales.feature.systemlog.service.SystemLogService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Triển khai nghiệp vụ quản lý màu sắc tôn sản xuất (Color) cho Admin.
 */
@Slf4j
@Service
public class ColorServiceImpl implements ColorService {

    private final ColorRepository colorRepository;
    private final SystemLogService systemLogService;

    /**
     * Khởi tạo ColorServiceImpl với repository màu sắc và service ghi log hệ thống.
     *
     * @param colorRepository  repository quản lý màu sắc
     * @param systemLogService service ghi nhật ký hệ thống
     */
    public ColorServiceImpl(ColorRepository colorRepository, SystemLogService systemLogService) {
        this.colorRepository = colorRepository;
        this.systemLogService = systemLogService;
    }

    /** {@inheritDoc} */
    @Override
    @Transactional(readOnly = true)
    public Page<ColorResponse> searchColors(String keyword, String status, int page, int size) {
        ColorStatus colorStatus = parseStatus(status);
        var pageable = PageRequest.of(page, size, Sort.by("colorName").ascending());
        String normalizedKeyword = VietnameseStringUtils.toUnaccentLowerCase(keyword);
        return colorRepository.searchColors(normalizedKeyword, colorStatus, pageable)
                .map(ColorResponse::fromEntity);
    }

    /** {@inheritDoc} */
    @Override
    @Transactional(readOnly = true)
    public ColorResponse getColorById(Long colorId) {
        return ColorResponse.fromEntity(findActiveColor(colorId));
    }

    /** {@inheritDoc} */
    @Override
    @Transactional
    public ColorResponse createColor(ColorRequest request) {
        if (colorRepository.existsByColorNameAndIsDeletedFalse(request.colorName())) {
            throw new BusinessRuleException("Tên màu sắc '" + request.colorName() + "' đã tồn tại.");
        }
        Color color = Color.builder()
                .colorName(request.colorName())
                .imageUrl(request.imageUrl())
                .build();
        color = colorRepository.save(color);
        log.info("[ColorService] Tạo màu sắc: {} (ID={})", color.getColorName(), color.getId());
        Long actorId = SecurityUtils.extractCurrentUserId();
        systemLogService.log(actorId, "COLOR_CREATED", "COLOR", color.getId(),
                "Tạo màu sắc: " + color.getColorName());
        return ColorResponse.fromEntity(color);
    }

    /** {@inheritDoc} */
    @Override
    @Transactional
    public ColorResponse updateColor(Long colorId, ColorRequest request) {
        Color color = findActiveColor(colorId);
        String oldName = color.getColorName();
        if (colorRepository.existsByColorNameAndIsDeletedFalseAndIdNot(request.colorName(), colorId)) {
            throw new BusinessRuleException("Tên màu sắc '" + request.colorName() + "' đã tồn tại.");
        }
        color.setColorName(request.colorName());
        color.setImageUrl(request.imageUrl());
        Color saved = colorRepository.save(color);
        Long actorId = SecurityUtils.extractCurrentUserId();
        systemLogService.logWithChange(actorId, "COLOR_UPDATED", "COLOR", colorId,
                "Cập nhật màu sắc: " + saved.getColorName(),
                AuditValueUtils.toJson(Map.of("colorName", oldName)),
                AuditValueUtils.toJson(Map.of("colorName", saved.getColorName())));
        return ColorResponse.fromEntity(saved);
    }

    /** {@inheritDoc} */
    @Override
    @Transactional
    public ColorResponse lockColor(Long colorId) {
        Color color = findActiveColor(colorId);
        color.setStatus(ColorStatus.LOCKED);
        Color saved = colorRepository.save(color);
        Long actorId = SecurityUtils.extractCurrentUserId();
        systemLogService.logWithChange(actorId, "COLOR_LOCKED", "COLOR", colorId,
                "Khóa màu sắc: " + saved.getColorName(),
                AuditValueUtils.toJson(Map.of("status", "ACTIVE")),
                AuditValueUtils.toJson(Map.of("status", "LOCKED")));
        return ColorResponse.fromEntity(saved);
    }

    /** {@inheritDoc} */
    @Override
    @Transactional
    public ColorResponse unlockColor(Long colorId) {
        Color color = findActiveColor(colorId);
        color.setStatus(ColorStatus.ACTIVE);
        Color saved = colorRepository.save(color);
        Long actorId = SecurityUtils.extractCurrentUserId();
        systemLogService.logWithChange(actorId, "COLOR_UNLOCKED", "COLOR", colorId,
                "Mở khóa màu sắc: " + saved.getColorName(),
                AuditValueUtils.toJson(Map.of("status", "LOCKED")),
                AuditValueUtils.toJson(Map.of("status", "ACTIVE")));
        return ColorResponse.fromEntity(saved);
    }

    /** {@inheritDoc} */
    @Override
    @Transactional
    public void deleteColor(Long colorId) {
        Color color = findActiveColor(colorId);
        if (colorRepository.hasActiveVariants(colorId)) {
            throw new BusinessRuleException("Không thể xóa màu sắc vì còn biến thể sản phẩm đang liên kết.");
        }
        color.setIsDeleted(true);
        colorRepository.save(color);
        Long actorId = SecurityUtils.extractCurrentUserId();
        systemLogService.log(actorId, "COLOR_DELETED", "COLOR", colorId,
                "Xóa mềm màu sắc: " + color.getColorName());
        log.info("[ColorService] Xóa mềm màu sắc ID={}", colorId);
    }

    /** {@inheritDoc} */
    @Override
    @Transactional(readOnly = true)
    public List<ColorResponse> getActiveColors() {
        return colorRepository.findByStatusAndIsDeletedFalseOrderByColorNameAsc(ColorStatus.ACTIVE)
                .stream().map(ColorResponse::fromEntity).collect(Collectors.toList());
    }

    private Color findActiveColor(Long colorId) {
        return colorRepository.findByIdAndIsDeletedFalse(colorId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy màu sắc ID: " + colorId));
    }

    private ColorStatus parseStatus(String status) {
        if (status == null || status.isBlank()) return null;
        try { return ColorStatus.valueOf(status.toUpperCase()); }
        catch (IllegalArgumentException e) { return null; }
    }
}
