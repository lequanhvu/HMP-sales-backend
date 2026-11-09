package com.hmp.sales.feature.setting.repository;

import com.hmp.sales.feature.setting.entity.SystemSetting;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository truy xuất dữ liệu tham số cấu hình hệ thống (SystemSetting).
 */
@Repository
public interface SystemSettingRepository extends JpaRepository<SystemSetting, Long> {

    /**
     * Tìm tham số cấu hình theo khóa và cờ chưa xóa mềm.
     *
     * @param settingKey khóa định danh của cấu hình
     * @return Optional chứa {@link SystemSetting} nếu tìm thấy
     */
    Optional<SystemSetting> findBySettingKeyAndIsDeletedFalse(String settingKey);

    /**
     * Lấy toàn bộ tham số cấu hình chưa bị xóa mềm, sắp xếp tăng dần theo khóa.
     *
     * @return danh sách tham số cấu hình
     */
    List<SystemSetting> findAllByIsDeletedFalseOrderBySettingKeyAsc();
}

