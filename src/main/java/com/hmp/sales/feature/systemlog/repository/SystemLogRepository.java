package com.hmp.sales.feature.systemlog.repository;

import com.hmp.sales.feature.systemlog.entity.SystemLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Repository truy vấn nhật ký hệ thống. Chỉ phục vụ ghi thêm và tra cứu, không dùng để xóa.
 */
@Repository
public interface SystemLogRepository extends JpaRepository<SystemLog, Long>, JpaSpecificationExecutor<SystemLog> {

    /**
     * Tra cứu một bản ghi nhật ký chưa bị xóa mềm theo ID.
     *
     * @param id ID bản ghi nhật ký
     * @return bản ghi nếu tồn tại
     */
    Optional<SystemLog> findByIdAndIsDeletedFalse(Long id);
}

