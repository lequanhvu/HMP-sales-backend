package com.hmp.sales.feature.systemlog.service;

import com.hmp.sales.common.exception.BusinessRuleException;
import com.hmp.sales.common.exception.ResourceNotFoundException;
import com.hmp.sales.feature.systemlog.dto.SystemLogDetailResponse;
import com.hmp.sales.feature.systemlog.dto.SystemLogSearchRequest;
import com.hmp.sales.feature.systemlog.dto.SystemLogSummaryResponse;
import com.hmp.sales.feature.systemlog.entity.SystemLog;
import com.hmp.sales.feature.systemlog.repository.SystemLogRepository;
import com.hmp.sales.feature.systemlog.service.impl.SystemLogServiceImpl;
import com.hmp.sales.feature.user.entity.User;
import com.hmp.sales.feature.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.TransactionStatus;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("SystemLogServiceImpl Unit Tests")
class SystemLogServiceImplTest {

    @Mock
    private SystemLogRepository systemLogRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private PlatformTransactionManager transactionManager;

    @Mock
    private TransactionStatus transactionStatus;

    private SystemLogServiceImpl systemLogService;

    @BeforeEach
    void setUp() {
        lenient().when(transactionManager.getTransaction(any(TransactionDefinition.class)))
                .thenReturn(transactionStatus);
        systemLogService = new SystemLogServiceImpl(systemLogRepository, userRepository, transactionManager);
    }

    @Test
    @DisplayName("log - Lưu bản ghi đơn giản thành công")
    void log_success() {
        User user = User.builder().id(10L).fullName("Admin").build();
        when(userRepository.getReferenceById(10L)).thenReturn(user);

        systemLogService.log(10L, "PRODUCT_CREATED", "PRODUCT", 100L, "Tạo sản phẩm mới");

        ArgumentCaptor<SystemLog> captor = ArgumentCaptor.forClass(SystemLog.class);
        verify(systemLogRepository).save(captor.capture());
        SystemLog saved = captor.getValue();
        assertThat(saved.getAction()).isEqualTo("PRODUCT_CREATED");
        assertThat(saved.getEntityType()).isEqualTo("PRODUCT");
        assertThat(saved.getEntityId()).isEqualTo(100L);
        assertThat(saved.getDescription()).isEqualTo("Tạo sản phẩm mới");
        assertThat(saved.getUser()).isEqualTo(user);
    }

    @Test
    @DisplayName("logWithChange - Lưu bản ghi kèm trước/sau state")
    void logWithChange_success() {
        systemLogService.logWithChange(null, "SETTING_UPDATED", "SETTING", 1L,
                "Đổi cấu hình", "{\"value\":30}", "{\"value\":60}");

        ArgumentCaptor<SystemLog> captor = ArgumentCaptor.forClass(SystemLog.class);
        verify(systemLogRepository).save(captor.capture());
        SystemLog saved = captor.getValue();
        assertThat(saved.getUser()).isNull();
        assertThat(saved.getOldValue()).isEqualTo("{\"value\":30}");
        assertThat(saved.getNewValue()).isEqualTo("{\"value\":60}");
    }

    @Test
    @DisplayName("logSecurityEvent - Ghi qua transaction riêng, lỗi DB không văng ra ngoài")
    void logSecurityEvent_dbFails_catchesAndDoesNotThrow() {
        doThrow(new RuntimeException("DB down"))
                .when(systemLogRepository).save(any(SystemLog.class));

        // Không ném ngoại lệ
        systemLogService.logSecurityEvent(1L, "LOGIN_FAILED", "USER", 1L, "Lỗi đăng nhập");
        verify(systemLogRepository).save(any(SystemLog.class));
    }

    @Test
    @DisplayName("search - Trả về phân trang tóm tắt")
    void search_returnsSummaryPage() {
        SystemLog entry = SystemLog.builder()
                .id(9001L)
                .action("PRODUCT_UPDATED")
                .description("Cập nhật sản phẩm")
                .createdAt(Instant.now())
                .build();
        Page<SystemLog> page = new PageImpl<>(List.of(entry));
        when(systemLogRepository.findAll(any(Specification.class), any(Pageable.class)))
                .thenReturn(page);

        SystemLogSearchRequest req = new SystemLogSearchRequest(null, "PRODUCT_UPDATED", null, null, null, null, null);
        Page<SystemLogSummaryResponse> res = systemLogService.search(req, 0, 10);

        assertThat(res.getContent()).hasSize(1);
        assertThat(res.getContent().get(0).logId()).isEqualTo(9001L);
        assertThat(res.getContent().get(0).activity()).isEqualTo("PRODUCT_UPDATED");
    }

    @Test
    @DisplayName("search - Ném BusinessRuleException khi khoảng ngày không hợp lệ")
    void search_invalidDateRange_throwsBusinessRuleException() {
        SystemLogSearchRequest req = new SystemLogSearchRequest(
                null, null, LocalDate.of(2026, 10, 5), LocalDate.of(2026, 10, 1), null, null, null);
        assertThatThrownBy(() -> systemLogService.search(req, 0, 10))
                .isInstanceOf(BusinessRuleException.class);
    }

    @Test
    @DisplayName("getById - Lấy chi tiết thành công")
    void getById_found_returnsDetail() {
        SystemLog entry = SystemLog.builder()
                .id(9001L)
                .action("PRODUCT_UPDATED")
                .entityType("PRODUCT")
                .entityId(100L)
                .description("Cập nhật")
                .oldValue("{\"name\":\"A\"}")
                .newValue("{\"name\":\"B\"}")
                .createdAt(Instant.now())
                .build();
        when(systemLogRepository.findByIdAndIsDeletedFalse(9001L)).thenReturn(Optional.of(entry));

        SystemLogDetailResponse res = systemLogService.getById(9001L);
        assertThat(res.logId()).isEqualTo(9001L);
        assertThat(res.oldValue()).isEqualTo("{\"name\":\"A\"}");
        assertThat(res.newValue()).isEqualTo("{\"name\":\"B\"}");
    }

    @Test
    @DisplayName("getById - Ném ResourceNotFoundException khi không tìm thấy")
    void getById_notFound_throwsException() {
        when(systemLogRepository.findByIdAndIsDeletedFalse(9999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> systemLogService.getById(9999L))
                .isInstanceOf(ResourceNotFoundException.class);
    }
}

