package com.hmp.sales.feature.order.repository;

import com.hmp.sales.common.util.VietnameseStringUtils;
import com.hmp.sales.feature.order.dto.ManagerOrderFilterRequest;
import com.hmp.sales.feature.order.dto.OrderFilterRequest;
import com.hmp.sales.feature.order.entity.Order;
import com.hmp.sales.feature.user.entity.User;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.LocalTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;

/**
 * Lớp tiện ích xây dựng JPA {@link Specification} cho các truy vấn tìm kiếm và lọc đơn hàng.
 */
@Component 
public final class OrderSpecification {

    private OrderSpecification() {
        // Utility class private constructor
    }

    /**
     * Xây dựng Specification cho khách hàng tra cứu đơn hàng của chính mình.
     *
     * @param customerId ID khách hàng sở hữu đơn
     * @param filter     bộ lọc theo ngày và trạng thái
     * @return {@link Specification} cho {@link Order}
     */
    public static Specification<Order> forCustomer(Long customerId, OrderFilterRequest filter) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            predicates.add(cb.equal(root.get("customer").get("id"), customerId));
            predicates.add(cb.isFalse(root.get("isDeleted")));

            if (filter != null) {
                if (filter.status() != null) {
                    predicates.add(cb.equal(root.get("status"), filter.status()));
                }
                if (filter.dateFrom() != null) {
                    Instant fromInstant = filter.dateFrom().atStartOfDay().toInstant(ZoneOffset.UTC);
                    predicates.add(cb.greaterThanOrEqualTo(root.get("createdAt"), fromInstant));
                }
                if (filter.dateTo() != null) {
                    Instant toInstant = filter.dateTo().atTime(LocalTime.MAX).toInstant(ZoneOffset.UTC);
                    predicates.add(cb.lessThanOrEqualTo(root.get("createdAt"), toInstant));
                }
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }

    /**
     * Xây dựng Specification cho Quản lý / Admin tra cứu và lọc toàn bộ đơn hàng trong hệ thống.
     *
     * @param filter bộ lọc đa tiêu chí (ngày tạo, trạng thái, tên khách, SĐT, email)
     * @return {@link Specification} cho {@link Order}
     */
    public static Specification<Order> forManager(ManagerOrderFilterRequest filter) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            // Chỉ lấy các đơn hàng chưa bị xóa mềm
            predicates.add(cb.isFalse(root.get("isDeleted")));

            if (filter != null) {
                // Lọc theo trạng thái
                if (filter.status() != null) {
                    predicates.add(cb.equal(root.get("status"), filter.status()));
                }

                // Lọc theo khoảng ngày tạo
                if (filter.dateFrom() != null) {
                    Instant fromInstant = filter.dateFrom().atStartOfDay().toInstant(ZoneOffset.UTC);
                    predicates.add(cb.greaterThanOrEqualTo(root.get("createdAt"), fromInstant));
                }
                if (filter.dateTo() != null) {
                    Instant toInstant = filter.dateTo().atTime(LocalTime.MAX).toInstant(ZoneOffset.UTC);
                    predicates.add(cb.lessThanOrEqualTo(root.get("createdAt"), toInstant));
                }

                // JOIN với Customer (User) để tìm kiếm thông tin khách hàng nếu cần
                boolean hasCustomerName = filter.customerName() != null && !filter.customerName().isBlank();
                boolean hasCustomerPhone = filter.customerPhone() != null && !filter.customerPhone().isBlank();
                boolean hasCustomerEmail = filter.customerEmail() != null && !filter.customerEmail().isBlank();

                Join<Order, User> customerJoin = null;
                if (hasCustomerName || hasCustomerPhone || hasCustomerEmail) {
                    customerJoin = root.join("customer", JoinType.LEFT);
                }

                // Tìm kiếm theo tên khách hàng (hỗ trợ tiếng Việt không dấu) hoặc tên người nhận
                if (hasCustomerName && customerJoin != null) {
                    String rawName = filter.customerName().trim();
                    String unaccentName = VietnameseStringUtils.toUnaccent(rawName);
                    String pattern = "%" + unaccentName.toLowerCase() + "%";
                    String rawPattern = "%" + rawName.toLowerCase() + "%";

                    Predicate customerNameMatch = cb.like(cb.lower(customerJoin.get("fullNameUnaccent")), pattern);
                    Predicate receiverNameMatch = cb.like(cb.lower(root.get("receiverName")), rawPattern);
                    predicates.add(cb.or(customerNameMatch, receiverNameMatch));
                }

                // Tìm kiếm theo số điện thoại (SĐT tài khoản hoặc SĐT người nhận)
                if (hasCustomerPhone && customerJoin != null) {
                    String phonePattern = "%" + filter.customerPhone().trim() + "%";
                    Predicate customerPhoneMatch = cb.like(customerJoin.get("phone"), phonePattern);
                    Predicate receiverPhoneMatch = cb.like(root.get("receiverPhone"), phonePattern);
                    predicates.add(cb.or(customerPhoneMatch, receiverPhoneMatch));
                }

                // Tìm kiếm theo email khách hàng
                if (hasCustomerEmail && customerJoin != null) {
                    String emailPattern = "%" + filter.customerEmail().trim().toLowerCase() + "%";
                    predicates.add(cb.like(cb.lower(customerJoin.get("email")), emailPattern));
                }
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }
}

