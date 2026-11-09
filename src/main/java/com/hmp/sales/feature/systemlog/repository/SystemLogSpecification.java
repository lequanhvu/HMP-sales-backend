package com.hmp.sales.feature.systemlog.repository;

import com.hmp.sales.common.exception.BusinessRuleException;
import com.hmp.sales.common.util.VietnameseStringUtils;
import com.hmp.sales.feature.systemlog.dto.SystemLogSearchRequest;
import com.hmp.sales.feature.systemlog.entity.SystemLog;
import com.hmp.sales.feature.user.entity.RoleCode;
import com.hmp.sales.feature.user.entity.User;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;

import java.time.Instant;
import java.time.LocalTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Xây dựng JPA {@link Specification} để tra cứu nhật ký hệ thống theo nhiều tiêu chí.
 */
public final class SystemLogSpecification {

    private SystemLogSpecification() {
        // Lớp tiện ích
    }

    /**
     * Tạo Specification từ bộ lọc tìm kiếm.
     *
     * @param filter bộ lọc tìm kiếm (có thể {@code null})
     * @return Specification cho {@link SystemLog}
     * @throws BusinessRuleException nếu {@code role} không phải mã vai trò hợp lệ
     */
    public static Specification<SystemLog> from(SystemLogSearchRequest filter) {
        RoleCode roleCode = filter != null ? parseRole(filter.role()) : null;
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            predicates.add(cb.isFalse(root.get("isDeleted")));

            if (filter == null) {
                return cb.and(predicates.toArray(new Predicate[0]));
            }

            if (hasText(filter.activity())) {
                predicates.add(cb.equal(root.get("action"), filter.activity().trim()));
            }
            if (filter.fromDate() != null) {
                Instant from = filter.fromDate().atStartOfDay().toInstant(ZoneOffset.UTC);
                predicates.add(cb.greaterThanOrEqualTo(root.get("createdAt"), from));
            }
            if (filter.toDate() != null) {
                Instant to = filter.toDate().atTime(LocalTime.MAX).toInstant(ZoneOffset.UTC);
                predicates.add(cb.lessThanOrEqualTo(root.get("createdAt"), to));
            }

            boolean needUser = roleCode != null || hasText(filter.name())
                    || hasText(filter.phone()) || hasText(filter.email());
            if (needUser) {
                Join<SystemLog, User> user = root.join("user", JoinType.INNER);
                if (roleCode != null) {
                    predicates.add(cb.equal(user.join("role", JoinType.INNER).get("roleCode"), roleCode));
                }
                if (hasText(filter.name())) {
                    String pattern = "%" + VietnameseStringUtils.toUnaccent(filter.name().trim()).toLowerCase(Locale.ROOT) + "%";
                    predicates.add(cb.like(cb.lower(user.get("fullNameUnaccent")), pattern));
                }
                if (hasText(filter.phone())) {
                    predicates.add(cb.like(user.get("phone"), "%" + filter.phone().trim() + "%"));
                }
                if (hasText(filter.email())) {
                    predicates.add(cb.like(cb.lower(user.get("email")),
                            "%" + filter.email().trim().toLowerCase(Locale.ROOT) + "%"));
                }
            }
            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }

    private static RoleCode parseRole(String role) {
        if (!hasText(role)) {
            return null;
        }
        String normalized = role.trim().toUpperCase(Locale.ROOT);
        if (normalized.startsWith("ROLE_")) {
            normalized = normalized.substring(5);
        }
        try {
            return RoleCode.valueOf(normalized);
        } catch (IllegalArgumentException e) {
            throw new BusinessRuleException("Vai trò không hợp lệ: " + role);
        }
    }

    private static boolean hasText(String s) {
        return s != null && !s.isBlank();
    }
}
