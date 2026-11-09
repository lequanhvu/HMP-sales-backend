package com.hmp.sales.feature.systemlog.controller;

import com.hmp.sales.common.response.ApiSuccessResponse;
import com.hmp.sales.common.response.PageResponse;
import com.hmp.sales.common.util.PaginationUtils;
import com.hmp.sales.config.AppProperties;
import com.hmp.sales.feature.systemlog.dto.SystemLogDetailResponse;
import com.hmp.sales.feature.systemlog.dto.SystemLogSearchRequest;
import com.hmp.sales.feature.systemlog.dto.SystemLogSummaryResponse;
import com.hmp.sales.feature.systemlog.service.SystemLogService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;

/**
 * REST Controller cho Admin tra cứu nhật ký hệ thống (audit log).
 * <p>
 * Nhật ký là chỉ-ghi-thêm nên controller này không cung cấp API sửa hoặc xóa.
 */
@RestController
@RequestMapping("/api/v1/admin/system-logs")
public class AdminSystemLogController {

    private final SystemLogService systemLogService;
    private final AppProperties appProperties;

    /**
     * Khởi tạo controller với service nhật ký và cấu hình ứng dụng.
     *
     * @param systemLogService service tra cứu nhật ký
     * @param appProperties    cấu hình phân trang từ {@code application.yaml}
     */
    @Autowired
    public AdminSystemLogController(SystemLogService systemLogService,
                                    @Autowired(required = false) AppProperties appProperties) {
        this.systemLogService = systemLogService;
        this.appProperties = appProperties;
    }

    /**
     * Khởi tạo controller đơn giản (dành cho unit test).
     *
     * @param systemLogService service tra cứu nhật ký
     */
    public AdminSystemLogController(SystemLogService systemLogService) {
        this(systemLogService, null);
    }

    /**
     * Tìm kiếm nhật ký hệ thống có phân trang, mới nhất lên đầu.
     *
     * @param role     mã vai trò của người thực hiện (tùy chọn)
     * @param activity mã hành động (tùy chọn)
     * @param fromDate ngày bắt đầu, định dạng {@code yyyy-MM-dd} (tùy chọn)
     * @param toDate   ngày kết thúc, định dạng {@code yyyy-MM-dd} (tùy chọn)
     * @param name     tên người thực hiện, hỗ trợ không dấu (tùy chọn)
     * @param phone    số điện thoại người thực hiện (tùy chọn)
     * @param email    email người thực hiện (tùy chọn)
     * @param page     chỉ số trang (tùy chọn)
     * @param size     số bản ghi mỗi trang (tùy chọn)
     * @return danh sách nhật ký phân trang
     */
    @GetMapping
    public ResponseEntity<ApiSuccessResponse<PageResponse<SystemLogSummaryResponse>>> searchLogs(
            @RequestParam(required = false) String role,
            @RequestParam(required = false) String activity,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fromDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate toDate,
            @RequestParam(required = false) String name,
            @RequestParam(required = false) String phone,
            @RequestParam(required = false) String email,
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size) {
        int pageNumber = PaginationUtils.resolvePage(page, appProperties);
        int pageSize = PaginationUtils.resolveSize(size, appProperties);
        SystemLogSearchRequest filter = new SystemLogSearchRequest(role, activity, fromDate, toDate, name, phone, email);
        return ResponseEntity.ok(ApiSuccessResponse.success(
                PageResponse.from(systemLogService.search(filter, pageNumber, pageSize))));
    }

    /**
     * Lấy chi tiết một bản ghi nhật ký.
     *
     * @param logId ID bản ghi nhật ký
     * @return chi tiết bản ghi, kèm trạng thái trước/sau thay đổi
     */
    @GetMapping("/{logId}")
    public ResponseEntity<ApiSuccessResponse<SystemLogDetailResponse>> getLog(@PathVariable Long logId) {
        return ResponseEntity.ok(ApiSuccessResponse.success(systemLogService.getById(logId)));
    }
}

