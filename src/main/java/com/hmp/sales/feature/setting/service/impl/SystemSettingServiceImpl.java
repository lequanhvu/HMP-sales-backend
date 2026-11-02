package com.hmp.sales.feature.setting.service.impl;

import com.hmp.sales.config.AppProperties;
import com.hmp.sales.feature.setting.entity.SystemSetting;
import com.hmp.sales.feature.setting.repository.SystemSettingRepository;
import com.hmp.sales.feature.setting.service.SystemSettingService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

/**
 * Lớp triển khai dịch vụ quản lý tham số cấu hình hệ thống (SystemSettingService).
 */
@Slf4j
@Service
public class SystemSettingServiceImpl implements SystemSettingService {

    private final SystemSettingRepository systemSettingRepository;
    private final AppProperties appProperties;

    /**
     * Khởi tạo service với các phụ thuộc cần thiết.
     *
     * @param systemSettingRepository repository tham số cấu hình
     * @param appProperties          cấu hình ứng dụng chung
     */
    public SystemSettingServiceImpl(SystemSettingRepository systemSettingRepository,
                                    AppProperties appProperties) {
        this.systemSettingRepository = systemSettingRepository;
        this.appProperties = appProperties;
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
    public Optional<String> getSettingValue(String settingKey) {
        return systemSettingRepository.findBySettingKeyAndIsDeletedFalse(settingKey)
                .map(SystemSetting::getSettingValue);
    }
}

