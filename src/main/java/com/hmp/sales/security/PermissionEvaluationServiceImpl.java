package com.hmp.sales.security;

import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

/**
 * Implementation của {@link PermissionEvaluationService}.
 * <p>
 * Đọc quyền trực tiếp từ danh sách {@link GrantedAuthority} trong {@link Authentication}.
 * Quyền được embedded vào JWT claims khi đăng nhập — không query DB mỗi request.
 * <p>
 * Khi Admin thay đổi quyền của một vai trò, hệ thống sẽ revoke toàn bộ access token
 * và refresh token của các user thuộc vai trò đó, buộc họ đăng nhập lại để nhận JWT
 * mới với quyền cập nhật. Xem {@code architecture.md} section 18.
 */
@Service("permEval")
public class PermissionEvaluationServiceImpl implements PermissionEvaluationService {

    private static final String PRODUCT_PRICE_VIEW = "PRODUCT_PRICE_VIEW";

    private final PermissionAuthorizationManager permissionAuthorizationManager;

    /**
     * Khởi tạo service đánh giá quyền với PermissionAuthorizationManager.
     *
     * @param permissionAuthorizationManager bộ quản lý quyền hạn động trên RAM
     */
    public PermissionEvaluationServiceImpl(PermissionAuthorizationManager permissionAuthorizationManager) {
        this.permissionAuthorizationManager = permissionAuthorizationManager;
    }

    /**
     * {@inheritDoc}
     * <p>
     * Tra cứu quyền hạn của người dùng từ bộ nhớ đệm RAM thông qua
     * {@link PermissionAuthorizationManager} dựa trên danh sách vai trò (Role)
     * có trong thông tin xác thực {@link Authentication}.
     * <p>
     * Đối với khách vãng lai (chưa đăng nhập hoặc mang {@link AnonymousAuthenticationToken}),
     * hệ thống cho phép kế thừa quyền {@code PRODUCT_PRICE_VIEW} từ vai trò {@code CUSTOMER}
     * nếu Quản trị viên (Admin) đã bật quyền xem giá cho khách hàng. Mọi quyền hạn khác
     * đối với khách vãng lai đều bị từ chối (false).
     *
     * @param authentication đối tượng xác thực hiện tại (chứa danh sách roles hoặc anonymous)
     * @param permissionName tên quyền cần kiểm tra (ví dụ: {@code "PRODUCT_PRICE_VIEW"})
     * @return {@code true} nếu người dùng hoặc khách vãng lai được phép thực hiện quyền này
     */
    @Override
    public boolean hasPermission(Authentication authentication, String permissionName) {
        if (authentication == null || !authentication.isAuthenticated() || authentication instanceof AnonymousAuthenticationToken) {
            if (PRODUCT_PRICE_VIEW.equals(permissionName)) {
                return permissionAuthorizationManager.hasRolePermission("ROLE_CUSTOMER", PRODUCT_PRICE_VIEW);
            }
            return false;
        }

        return permissionAuthorizationManager.hasPermission(authentication, permissionName);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean hasPermission(String permissionName) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return hasPermission(auth, permissionName);
    }

    /**
     * {@inheritDoc}
     * <p>
     * Ủy thác kiểm tra cho {@link #hasPermission(Authentication, String)}
     * với quyền {@code PRODUCT_PRICE_VIEW}.
     */
    @Override
    public boolean canViewProductPrice(Authentication authentication) {
        return hasPermission(authentication, PRODUCT_PRICE_VIEW);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean canViewProductPrice() {
        return hasPermission(PRODUCT_PRICE_VIEW);
    }
}
