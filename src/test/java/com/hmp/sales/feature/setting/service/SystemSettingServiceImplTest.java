package com.hmp.sales.feature.setting.service;

import com.hmp.sales.config.AppProperties;
import com.hmp.sales.feature.setting.entity.SystemSetting;
import com.hmp.sales.feature.setting.repository.SystemSettingRepository;
import com.hmp.sales.feature.setting.service.impl.SystemSettingServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

/**
 * Unit test cho {@link SystemSettingServiceImpl}.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("SystemSettingServiceImpl Unit Tests")
class SystemSettingServiceImplTest {

    @Mock
    private SystemSettingRepository systemSettingRepository;

    @Mock
    private AppProperties appProperties;

    @Mock
    private AppProperties.OrderProperties orderProperties;

    @InjectMocks
    private SystemSettingServiceImpl systemSettingService;

    @Test
    @DisplayName("Lấy cancellationWindowMinutes từ DB thành công khi có cấu hình hợp lệ")
    void getCancellationWindowMinutes_fromDb_returnsDbValue() {
        SystemSetting setting = SystemSetting.builder()
                .settingKey(SystemSettingService.KEY_CANCELLATION_WINDOW_MINUTES)
                .settingValue("45")
                .build();

        when(systemSettingRepository.findBySettingKeyAndIsDeletedFalse(SystemSettingService.KEY_CANCELLATION_WINDOW_MINUTES))
                .thenReturn(Optional.of(setting));

        int minutes = systemSettingService.getCancellationWindowMinutes();
        assertThat(minutes).isEqualTo(45);
    }

    @Test
    @DisplayName("Fallback về AppProperties khi cấu hình trong DB không phải số nguyên")
    void getCancellationWindowMinutes_invalidDbValue_fallbackToAppProperties() {
        SystemSetting setting = SystemSetting.builder()
                .settingKey(SystemSettingService.KEY_CANCELLATION_WINDOW_MINUTES)
                .settingValue("abc_invalid")
                .build();

        when(systemSettingRepository.findBySettingKeyAndIsDeletedFalse(SystemSettingService.KEY_CANCELLATION_WINDOW_MINUTES))
                .thenReturn(Optional.of(setting));
        when(appProperties.order()).thenReturn(orderProperties);
        when(orderProperties.cancellationWindowMinutes()).thenReturn(30);

        int minutes = systemSettingService.getCancellationWindowMinutes();
        assertThat(minutes).isEqualTo(30);
    }

    @Test
    @DisplayName("Fallback về AppProperties khi không tìm thấy cấu hình trong DB")
    void getCancellationWindowMinutes_noDbRecord_fallbackToAppProperties() {
        when(systemSettingRepository.findBySettingKeyAndIsDeletedFalse(SystemSettingService.KEY_CANCELLATION_WINDOW_MINUTES))
                .thenReturn(Optional.empty());
        when(appProperties.order()).thenReturn(orderProperties);
        when(orderProperties.cancellationWindowMinutes()).thenReturn(30);

        int minutes = systemSettingService.getCancellationWindowMinutes();
        assertThat(minutes).isEqualTo(30);
    }

    @Test
    @DisplayName("Lấy undoCancelWindowMinutes từ DB thành công khi có cấu hình hợp lệ")
    void getUndoCancelWindowMinutes_fromDb_returnsDbValue() {
        SystemSetting setting = SystemSetting.builder()
                .settingKey(SystemSettingService.KEY_UNDO_CANCEL_WINDOW_MINUTES)
                .settingValue("90")
                .build();

        when(systemSettingRepository.findBySettingKeyAndIsDeletedFalse(SystemSettingService.KEY_UNDO_CANCEL_WINDOW_MINUTES))
                .thenReturn(Optional.of(setting));

        int minutes = systemSettingService.getUndoCancelWindowMinutes();
        assertThat(minutes).isEqualTo(90);
    }

    @Test
    @DisplayName("Fallback về AppProperties cho undoCancelWindowMinutes khi cấu hình trong DB không phải số nguyên")
    void getUndoCancelWindowMinutes_invalidDbValue_fallbackToAppProperties() {
        SystemSetting setting = SystemSetting.builder()
                .settingKey(SystemSettingService.KEY_UNDO_CANCEL_WINDOW_MINUTES)
                .settingValue("invalid_num")
                .build();

        when(systemSettingRepository.findBySettingKeyAndIsDeletedFalse(SystemSettingService.KEY_UNDO_CANCEL_WINDOW_MINUTES))
                .thenReturn(Optional.of(setting));
        when(appProperties.order()).thenReturn(orderProperties);
        when(orderProperties.undoCancelWindowMinutes()).thenReturn(60);

        int minutes = systemSettingService.getUndoCancelWindowMinutes();
        assertThat(minutes).isEqualTo(60);
    }

    @Test
    @DisplayName("Fallback về AppProperties cho undoCancelWindowMinutes khi không tìm thấy cấu hình trong DB")
    void getUndoCancelWindowMinutes_noDbRecord_fallbackToAppProperties() {
        when(systemSettingRepository.findBySettingKeyAndIsDeletedFalse(SystemSettingService.KEY_UNDO_CANCEL_WINDOW_MINUTES))
                .thenReturn(Optional.empty());
        when(appProperties.order()).thenReturn(orderProperties);
        when(orderProperties.undoCancelWindowMinutes()).thenReturn(60);

        int minutes = systemSettingService.getUndoCancelWindowMinutes();
        assertThat(minutes).isEqualTo(60);
    }

    @Test
    @DisplayName("Lấy giá trị chuỗi cấu hình thành công")
    void getSettingValue_found_returnsValue() {
        SystemSetting setting = SystemSetting.builder()
                .settingKey("TEST_KEY")
                .settingValue("test_value")
                .build();

        when(systemSettingRepository.findBySettingKeyAndIsDeletedFalse("TEST_KEY"))
                .thenReturn(Optional.of(setting));

        Optional<String> val = systemSettingService.getSettingValue("TEST_KEY");
        assertThat(val).isPresent().contains("test_value");
    }

    @Test
    @DisplayName("Lấy giá trị chuỗi cấu hình trả về empty khi không tìm thấy")
    void getSettingValue_notFound_returnsEmpty() {
        when(systemSettingRepository.findBySettingKeyAndIsDeletedFalse("NOT_FOUND"))
                .thenReturn(Optional.empty());

        Optional<String> val = systemSettingService.getSettingValue("NOT_FOUND");
        assertThat(val).isEmpty();
    }
}

