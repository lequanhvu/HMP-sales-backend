package com.hmp.sales.common.util;

import com.hmp.sales.common.exception.BusinessRuleException;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Lớp tiện ích xử lý và kiểm tra tính hợp lệ của ngày tháng và khoảng thời gian (Date & Time Utils).
 * <p>
 * Cung cấp các hàm kiểm tra điều kiện {@code fromDate <= toDate} cho các kiểu dữ liệu
 * {@link LocalDate}, {@link LocalDateTime}, và {@link Instant}.
 * </p>
 */
public final class DateUtils {

    private DateUtils() {
        throw new UnsupportedOperationException("Lớp tiện ích không thể khởi tạo đối tượng.");
    }

    /**
     * Kiểm tra khoảng ngày {@link LocalDate} có hợp lệ hay không (tức {@code fromDate <= toDate}).
     * <p>
     * Nếu một trong hai giá trị là {@code null}, hàm coi như hợp lệ (không bị chặn giới hạn một đầu).
     * </p>
     *
     * @param fromDate ngày bắt đầu
     * @param toDate   ngày kết thúc
     * @return {@code true} nếu khoảng ngày hợp lệ hoặc có giá trị null; {@code false} nếu {@code fromDate > toDate}
     */
    public static boolean isValidDateRange(LocalDate fromDate, LocalDate toDate) {
        if (fromDate == null || toDate == null) {
            return true;
        }
        return !fromDate.isAfter(toDate);
    }

    /**
     * Xác thực khoảng ngày {@link LocalDate}. Nếu {@code fromDate > toDate}, ném ra {@link BusinessRuleException}.
     *
     * @param fromDate ngày bắt đầu
     * @param toDate   ngày kết thúc
     * @throws BusinessRuleException nếu ngày bắt đầu lớn hơn ngày kết thúc
     */
    public static void validateDateRange(LocalDate fromDate, LocalDate toDate) {
        if (!isValidDateRange(fromDate, toDate)) {
            throw new BusinessRuleException(
                    String.format("Ngày bắt đầu (%s) không được lớn hơn ngày kết thúc (%s).", fromDate, toDate));
        }
    }

    /**
     * Kiểm tra khoảng thời gian {@link LocalDateTime} có hợp lệ hay không (tức {@code fromDate <= toDate}).
     *
     * @param fromDate thời điểm bắt đầu
     * @param toDate   thời điểm kết thúc
     * @return {@code true} nếu khoảng thời gian hợp lệ hoặc có giá trị null; {@code false} nếu {@code fromDate > toDate}
     */
    public static boolean isValidDateRange(LocalDateTime fromDate, LocalDateTime toDate) {
        if (fromDate == null || toDate == null) {
            return true;
        }
        return !fromDate.isAfter(toDate);
    }

    /**
     * Xác thực khoảng thời gian {@link LocalDateTime}. Nếu {@code fromDate > toDate}, ném ra {@link BusinessRuleException}.
     *
     * @param fromDate thời điểm bắt đầu
     * @param toDate   thời điểm kết thúc
     * @throws BusinessRuleException nếu thời điểm bắt đầu lớn hơn thời điểm kết thúc
     */
    public static void validateDateRange(LocalDateTime fromDate, LocalDateTime toDate) {
        if (!isValidDateRange(fromDate, toDate)) {
            throw new BusinessRuleException(
                    String.format("Thời điểm bắt đầu (%s) không được lớn hơn thời điểm kết thúc (%s).", fromDate, toDate));
        }
    }

    /**
     * Kiểm tra khoảng mốc thời gian {@link Instant} có hợp lệ hay không (tức {@code fromDate <= toDate}).
     *
     * @param fromDate mốc thời gian bắt đầu
     * @param toDate   mốc thời gian kết thúc
     * @return {@code true} nếu khoảng thời gian hợp lệ hoặc có giá trị null; {@code false} nếu {@code fromDate > toDate}
     */
    public static boolean isValidDateRange(Instant fromDate, Instant toDate) {
        if (fromDate == null || toDate == null) {
            return true;
        }
        return !fromDate.isAfter(toDate);
    }

    /**
     * Xác thực khoảng mốc thời gian {@link Instant}. Nếu {@code fromDate > toDate}, ném ra {@link BusinessRuleException}.
     *
     * @param fromDate mốc thời gian bắt đầu
     * @param toDate   mốc thời gian kết thúc
     * @throws BusinessRuleException nếu mốc thời gian bắt đầu lớn hơn mốc thời gian kết thúc
     */
    public static void validateDateRange(Instant fromDate, Instant toDate) {
        if (!isValidDateRange(fromDate, toDate)) {
            throw new BusinessRuleException(
                    String.format("Thời điểm bắt đầu (%s) không được lớn hơn thời điểm kết thúc (%s).", fromDate, toDate));
        }
    }
}

