package com.hmp.sales.common.util;

import com.hmp.sales.common.exception.BusinessRuleException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("DateUtils Unit Tests")
class DateUtilsTest {

    @Test
    @DisplayName("isValidDateRange với LocalDate trả về true khi fromDate <= toDate hoặc có giá trị null")
    void isValidDateRange_localDate_returnsTrue() {
        LocalDate today = LocalDate.of(2026, 11, 4);
        LocalDate tomorrow = LocalDate.of(2026, 11, 5);

        assertThat(DateUtils.isValidDateRange(today, tomorrow)).isTrue();
        assertThat(DateUtils.isValidDateRange(today, today)).isTrue();
        assertThat(DateUtils.isValidDateRange(null, tomorrow)).isTrue();
        assertThat(DateUtils.isValidDateRange(today, null)).isTrue();
        assertThat(DateUtils.isValidDateRange((LocalDate) null, (LocalDate) null)).isTrue();
    }

    @Test
    @DisplayName("isValidDateRange với LocalDate trả về false khi fromDate > toDate")
    void isValidDateRange_localDate_returnsFalse() {
        LocalDate today = LocalDate.of(2026, 11, 4);
        LocalDate yesterday = LocalDate.of(2026, 11, 3);

        assertThat(DateUtils.isValidDateRange(today, yesterday)).isFalse();
    }

    @Test
    @DisplayName("validateDateRange với LocalDate ném BusinessRuleException khi fromDate > toDate")
    void validateDateRange_localDate_throwsBusinessRuleException() {
        LocalDate from = LocalDate.of(2026, 11, 10);
        LocalDate to = LocalDate.of(2026, 11, 1);

        assertThatThrownBy(() -> DateUtils.validateDateRange(from, to))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("Ngày bắt đầu (2026-11-10) không được lớn hơn ngày kết thúc (2026-11-01).");
    }

    @Test
    @DisplayName("isValidDateRange với LocalDateTime trả về kết quả chính xác")
    void isValidDateRange_localDateTime() {
        LocalDateTime now = LocalDateTime.of(2026, 11, 4, 10, 0, 0);
        LocalDateTime later = LocalDateTime.of(2026, 11, 4, 11, 0, 0);

        assertThat(DateUtils.isValidDateRange(now, later)).isTrue();
        assertThat(DateUtils.isValidDateRange(later, now)).isFalse();

        assertThatThrownBy(() -> DateUtils.validateDateRange(later, now))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("Thời điểm bắt đầu");
    }

    @Test
    @DisplayName("isValidDateRange với Instant trả về kết quả chính xác")
    void isValidDateRange_instant() {
        Instant now = Instant.now();
        Instant later = now.plusSeconds(3600);

        assertThat(DateUtils.isValidDateRange(now, later)).isTrue();
        assertThat(DateUtils.isValidDateRange(later, now)).isFalse();

        assertThatThrownBy(() -> DateUtils.validateDateRange(later, now))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("Thời điểm bắt đầu");
    }
}
