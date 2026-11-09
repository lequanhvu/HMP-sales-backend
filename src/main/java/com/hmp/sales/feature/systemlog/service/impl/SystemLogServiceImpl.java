package com.hmp.sales.feature.systemlog.service.impl;

import com.hmp.sales.common.exception.ResourceNotFoundException;
import com.hmp.sales.common.util.AuditValueUtils;
import com.hmp.sales.common.util.DateUtils;
import com.hmp.sales.feature.systemlog.dto.SystemLogDetailResponse;
import com.hmp.sales.feature.systemlog.dto.SystemLogSearchRequest;
import com.hmp.sales.feature.systemlog.dto.SystemLogSummaryResponse;
import com.hmp.sales.feature.systemlog.entity.SystemLog;
import com.hmp.sales.feature.systemlog.repository.SystemLogRepository;
import com.hmp.sales.feature.systemlog.repository.SystemLogSpecification;
import com.hmp.sales.feature.systemlog.service.SystemLogService;
import com.hmp.sales.feature.user.entity.User;
import com.hmp.sales.feature.user.repository.UserRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * Triển khai {@link SystemLogService}: lưu nhật ký vào PostgreSQL (bảng {@code system_logs}).
 * <p>
 * {@link #log} và {@link #logWithChange} tham gia giao dịch của thao tác nghiệp vụ gọi chúng
 * (propagation {@code REQUIRED}); nếu ghi nhật ký lỗi thì thao tác nghiệp vụ cũng rollback để
 * không bao giờ tồn tại thay đổi quản trị nào thiếu dấu vết. {@link #logSecurityEvent} dùng giao
 * dịch riêng và chỉ cảnh báo khi lỗi.
 */
@Slf4j
@Service
public class SystemLogServiceImpl implements SystemLogService {

    /** Độ dài tối đa của mô tả được lưu. */
    static final int MAX_DESCRIPTION_LENGTH = 2000;
    /** Độ dài tối đa của {@code oldValue}/{@code newValue} được lưu. */
    static final int MAX_VALUE_LENGTH = 4000;

    private final SystemLogRepository systemLogRepository;
    private final UserRepository userRepository;
    private final TransactionTemplate requiresNewTemplate;

    /**
     * Khởi tạo service với các phụ thuộc cần thiết.
     *
     * @param systemLogRepository repository nhật ký hệ thống
     * @param userRepository      repository người dùng, dùng để tham chiếu người thực hiện
     * @param transactionManager  transaction manager để mở giao dịch riêng cho sự kiện bảo mật
     */
    public SystemLogServiceImpl(SystemLogRepository systemLogRepository,
                                UserRepository userRepository,
                                PlatformTransactionManager transactionManager) {
        this.systemLogRepository = systemLogRepository;
        this.userRepository = userRepository;
        this.requiresNewTemplate = new TransactionTemplate(transactionManager);
        this.requiresNewTemplate.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @Transactional
    public void log(Long actorId, String action, String entityType, Long entityId, String description) {
        persist(actorId, action, entityType, entityId, description, null, null);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @Transactional
    public void logWithChange(Long actorId, String action, String entityType, Long entityId,
                              String description, String oldValue, String newValue) {
        persist(actorId, action, entityType, entityId, description, oldValue, newValue);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void logSecurityEvent(Long actorId, String action, String entityType, Long entityId, String description) {
        try {
            requiresNewTemplate.executeWithoutResult(status ->
                    persist(actorId, action, entityType, entityId, description, null, null));
        } catch (RuntimeException e) {
            log.warn("[SystemLog] Không ghi được sự kiện bảo mật action={} entity={}:{}: {}",
                    action, entityType, entityId, e.getMessage());
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @Transactional(readOnly = true)
    public Page<SystemLogSummaryResponse> search(SystemLogSearchRequest filter, int page, int size) {
        if (filter != null) {
            DateUtils.validateDateRange(filter.fromDate(), filter.toDate());
        }
        PageRequest pageable = PageRequest.of(page, size,
                Sort.by(Sort.Direction.DESC, "createdAt").and(Sort.by(Sort.Direction.DESC, "id")));
        return systemLogRepository.findAll(SystemLogSpecification.from(filter), pageable)
                .map(this::toSummary);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @Transactional(readOnly = true)
    public SystemLogDetailResponse getById(Long logId) {
        SystemLog entry = systemLogRepository.findByIdAndIsDeletedFalse(logId)
                .orElseThrow(() -> new ResourceNotFoundException("Nhật ký hệ thống", logId));
        return new SystemLogDetailResponse(
                entry.getId(), actorIdOf(entry), entry.getAction(), entry.getEntityType(), entry.getEntityId(),
                entry.getDescription(), entry.getOldValue(), entry.getNewValue(), entry.getCreatedAt());
    }

    private void persist(Long actorId, String action, String entityType, Long entityId,
                         String description, String oldValue, String newValue) {
        User actor = actorId != null ? userRepository.getReferenceById(actorId) : null;
        SystemLog entry = SystemLog.builder()
                .user(actor)
                .action(AuditValueUtils.truncate(action, 100))
                .entityType(AuditValueUtils.truncate(entityType, 50))
                .entityId(entityId)
                .description(AuditValueUtils.truncate(description, MAX_DESCRIPTION_LENGTH))
                .oldValue(AuditValueUtils.truncate(oldValue, MAX_VALUE_LENGTH))
                .newValue(AuditValueUtils.truncate(newValue, MAX_VALUE_LENGTH))
                .build();
        systemLogRepository.save(entry);
    }

    private SystemLogSummaryResponse toSummary(SystemLog entry) {
        return new SystemLogSummaryResponse(
                entry.getId(), actorIdOf(entry), entry.getAction(), entry.getDescription(), entry.getCreatedAt());
    }

    private Long actorIdOf(SystemLog entry) {
        return entry.getUser() != null ? entry.getUser().getId() : null;
    }
}
