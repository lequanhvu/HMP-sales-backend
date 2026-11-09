package com.hmp.sales.feature.setting.service.impl;

import com.hmp.sales.common.exception.BusinessRuleException;
import com.hmp.sales.common.exception.ResourceNotFoundException;
import com.hmp.sales.common.util.AuditValueUtils;
import com.hmp.sales.config.AppProperties;
import com.hmp.sales.feature.setting.dto.SettingResponse;
import com.hmp.sales.feature.setting.entity.SystemSetting;
import com.hmp.sales.feature.setting.repository.SystemSettingRepository;
import com.hmp.sales.feature.setting.service.SystemSettingService;
import com.hmp.sales.feature.systemlog.service.SystemLogService;
import com.hmp.sales.feature.user.repository.UserRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

/**
 * Lớp triển khai dịch vụ quản lý tham số cấu hình hệ thống (SystemSettingService).
 */
@Slf4j
@Service
public class SystemSettingServiceImpl implements SystemSettingService {

    private final SystemSettingRepository systemSettingRepository;
    private final AppProperties appProperties;
    private final SystemLogService systemLogService;
    private final UserRepository userRepository;

    /**
     * Khởi tạo service với các phụ thuộc cần thiết.
     *
     * @param systemSettingRepository repository tham số cấu hình
     * @param appProperties          cấu hình ứng dụng chung
     * @param systemLogService       service ghi nhật ký hệ thống
     * @param userRepository         repository người dùng, dùng để ghi nhận người cập nhật
     */
    public SystemSettingServiceImpl(SystemSettingRepository systemSettingRepository,
                                    AppProperties appProperties,
                                    SystemLogService systemLogService,
                                    UserRepository userRepository) {
        this.systemSettingRepository = systemSettingRepository;
        this.appProperties = appProperties;
        this.systemLogService = systemLogService;
        this.userRepository = userRepository;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @Transactional(readOnly = true)
    public List<SettingResponse> listSettings() {
        return systemSettingRepository.findAllByIsDeletedFalseOrderBySettingKeyAsc().stream()
                .map(this::toResponse)
                .toList();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @Transactional
    public SettingResponse updateSetting(Long actorId, String settingKey, String value) {
        SystemSetting setting = systemSettingRepository.findBySettingKeyAndIsDeletedFalse(settingKey)
                .orElseThrow(() -> new ResourceNotFoundException("Cấu hình hệ thống", settingKey));

        String newValue = value == null ? "" : value.trim();
        validateValue(setting, newValue);

        String oldValue = setting.getSettingValue();
        setting.setSettingValue(newValue);
        if (actorId != null) {
            setting.setUpdatedBy(userRepository.getReferenceById(actorId));
        }
        SystemSetting saved = systemSettingRepository.save(setting);

        systemLogService.logWithChange(actorId, "SYSTEM_SETTING_UPDATED", "SYSTEM_SETTING", saved.getId(),
                "Cập nhật cấu hình hệ thống " + settingKey,
                AuditValueUtils.toJson(Map.of("key", settingKey, "value", oldValue)),
                AuditValueUtils.toJson(Map.of("key", settingKey, "value", newValue)));
        return toResponse(saved);
    }

    /**
     * Kiểm tra giá trị mới phù hợp với kiểu dữ liệu của tham số.
     * Các khóa cửa sổ thời gian bắt buộc là số nguyên dương.
     *
     * @param setting tham số đang cập nhật
     * @param value   giá trị mới đã trim
     * @throws BusinessRuleException nếu giá trị không hợp lệ
     */
    private void validateValue(SystemSetting setting, String value) {
        if (value.isEmpty()) {
            throw new BusinessRuleException("Giá trị cấu hình không được để trống");
        }
        String dataType = setting.getDataType() == null ? "STRING" : setting.getDataType().toUpperCase(Locale.ROOT);
        switch (dataType) {
            case "INT", "INTEGER" -> {
                int number;
                try {
                    number = Integer.parseInt(value);
                } catch (NumberFormatException e) {
                    throw new BusinessRuleException("Giá trị cấu hình phải là số nguyên");
                }
                boolean isWindowKey = KEY_CANCELLATION_WINDOW_MINUTES.equals(setting.getSettingKey())
                        || KEY_UNDO_CANCEL_WINDOW_MINUTES.equals(setting.getSettingKey());
                if (number < 0 || (isWindowKey && number == 0)) {
                    throw new BusinessRuleException(isWindowKey
                            ? "Thời gian cửa sổ (phút) phải là số nguyên dương"
                            : "Giá trị cấu hình không được là số âm");
                }
            }
            case "BOOLEAN" -> {
                if (!"true".equalsIgnoreCase(value) && !"false".equalsIgnoreCase(value)) {
                    throw new BusinessRuleException("Giá trị cấu hình phải là true hoặc false");
                }
            }
            case "DECIMAL" -> {
                try {
                    new BigDecimal(value);
                } catch (NumberFormatException e) {
                    throw new BusinessRuleException("Giá trị cấu hình phải là số thập phân");
                }
            }
            default -> {
                // STRING: chỉ cần không rỗng (đã kiểm tra ở trên)
            }
        }
    }

    private SettingResponse toResponse(SystemSetting s) {
        return new SettingResponse(s.getSettingKey(), s.getSettingValue(), s.getDataType(),
                s.getDescription(), s.getUpdatedAt());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @Transactional(readOnly = true)
    public int getCancellationWindowMinutes() {
        Optional<SystemSetting> settingOpt = systemSettingRepository
                .findBySettingKeyAndIsDeletedFalse(KEY_CANCELLATION_WINDOW_MINUTES);

        if (settingOpt.isPresent()) {
            try {
                return Integer.parseInt(settingOpt.get().getSettingValue().trim());
            } catch (NumberFormatException e) {
                log.warn("[SystemSetting] Giá trị cấu hình '{}' không phải số nguyên: '{}'. Sử dụng giá trị fallback mặc định.",
                        KEY_CANCELLATION_WINDOW_MINUTES, settingOpt.get().getSettingValue());
            }
        }

        return appProperties.order().cancellationWindowMinutes();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @Transactional(readOnly = true)
    public int getUndoCancelWindowMinutes() {
        Optional<SystemSetting> settingOpt = systemSettingRepository
                .findBySettingKeyAndIsDeletedFalse(KEY_UNDO_CANCEL_WINDOW_MINUTES);

        if (settingOpt.isPresent()) {
            try {
                return Integer.parseInt(settingOpt.get().getSettingValue().trim());
            } catch (NumberFormatException e) {
                log.warn("[SystemSetting] Giá trị cấu hình '{}' không phải số nguyên: '{}'. Sử dụng giá trị fallback mặc định.",
                        KEY_UNDO_CANCEL_WINDOW_MINUTES, settingOpt.get().getSettingValue());
            }
        }

        return appProperties.order().undoCancelWindowMinutes();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @Transactional(readOnly = true)
    public Optional<String> getSettingValue(String settingKey) {
        return systemSettingRepository.findBySettingKeyAndIsDeletedFalse(settingKey)
                .map(SystemSetting::getSettingValue);
    }
}

