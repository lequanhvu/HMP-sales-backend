package com.hmp.sales.common.util;

import com.hmp.sales.config.AppProperties;

/**
 * Tiện ích hỗ trợ chuẩn hóa và giải quyết các tham số phân trang (page, size)
 * từ cấu hình {@code application.yaml} thông qua {@link AppProperties}.
 * <p>
 * Giúp các Controller tránh việc hardcode giá trị phân trang mặc định và
 * tự động áp dụng giới hạn kích thước trang tối đa (max-page-size).
 */
public final class PaginationUtils {

    /**
     * Hàm khởi tạo riêng tư để ngăn chặn khởi tạo đối tượng tiện ích.
     *
     * @throws UnsupportedOperationException luôn ném ngoại lệ khi được gọi
     */
    private PaginationUtils() {
        throw new UnsupportedOperationException("Lớp tiện ích không thể khởi tạo instance.");
    }

    /**
     * Xác định chỉ số trang hiện tại. Nếu tham số {@code page} được truyền vào, trả về giá trị đó (tối thiểu 0).
     * Ngược lại, lấy giá trị mặc định từ cấu hình {@code app.pagination.default-page}.
     *
     * @param page          tham số chỉ số trang do client truyền vào (có thể null)
     * @param appProperties cấu hình ứng dụng chứa thông tin phân trang (có thể null khi chạy test đơn lập)
     * @return chỉ số trang hợp lệ (0-indexed)
     */
    public static int resolvePage(Integer page, AppProperties appProperties) {
        if (page != null) {
            return Math.max(0, page);
        }
        if (appProperties != null && appProperties.pagination() != null) {
            return Math.max(0, appProperties.pagination().defaultPage());
        }
        return 0;
    }

    /**
     * Xác định kích thước trang. Nếu tham số {@code size} được truyền vào, đảm bảo giá trị nằm trong khoảng [1, maxPageSize].
     * Ngược lại, lấy giá trị mặc định từ cấu hình {@code app.pagination.default-page-size}.
     *
     * @param size          tham số kích thước trang do client truyền vào (có thể null)
     * @param appProperties cấu hình ứng dụng chứa thông tin phân trang (có thể null khi chạy test đơn lập)
     * @return kích thước trang hợp lệ
     */
    public static int resolveSize(Integer size, AppProperties appProperties) {
        int max = (appProperties != null && appProperties.pagination() != null)
                ? appProperties.pagination().maxPageSize()
                : 100;

        if (size != null && size > 0) {
            return Math.min(size, max);
        }
        if (appProperties != null && appProperties.pagination() != null) {
            return Math.min(appProperties.pagination().defaultPageSize(), max);
        }
        return 20;
    }
}

