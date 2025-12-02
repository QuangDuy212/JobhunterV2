package vn.hoidanit.jobhunter.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.HandlerMapping;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import vn.hoidanit.jobhunter.domain.Permission;
import vn.hoidanit.jobhunter.domain.Role;
import vn.hoidanit.jobhunter.domain.User;
import vn.hoidanit.jobhunter.service.AuditLogService;
import vn.hoidanit.jobhunter.service.UserService;
import vn.hoidanit.jobhunter.util.SecurityUtil;
import vn.hoidanit.jobhunter.util.error.IdInvalidException;
import vn.hoidanit.jobhunter.util.error.PermissionException;

import java.util.List;

public class PermissionInterceptor implements HandlerInterceptor {

    @Autowired
    private UserService userService;

    @Autowired
    private AuditLogService auditLogService;

    @Override
    @Transactional
    public boolean preHandle(
            HttpServletRequest request,
            HttpServletResponse response, Object handler)
            throws Exception, PermissionException {
        String path = (String) request.getAttribute(HandlerMapping.BEST_MATCHING_PATTERN_ATTRIBUTE);
        String requestURI = request.getRequestURI();
        String httpMethod = request.getMethod();
        System.out.println(">>> RUN preHandle");
        System.out.println(">>> path= " + path);
        System.out.println(">>> httpMethod= " + httpMethod);
        System.out.println(">>> requestURI= " + requestURI);

        // check permission
        String email = SecurityUtil.getCurrentUserLogin().isPresent()
                ? SecurityUtil.getCurrentUserLogin().get()
                : "";
        if (email != null && !email.isEmpty()) {
            User user = this.userService.handleGetUserByUsername(email);
            if (user != null) {
                Role role = user.getRole();
                if (role != null) {
                    List<Permission> permissions = role.getPermissions();
                    boolean isAllow = permissions.stream()
                            .anyMatch(item -> item.getApiPath().equals(path) && item.getMethod().equals(httpMethod));
                    if (!isAllow) {
                        // ⭐️ GHI LOG: HÀNH ĐỘNG TRUY CẬP BỊ TỪ CHỐI
                        auditLogService.logAction(
                            "ACCESS_DENIED", 
                            email, 
                            "Thử truy cập " + httpMethod + " " + path + " (URI: " + requestURI + ")"
                        );
                        throw new PermissionException("Bạn không có quyền truy cập vào trang này");
                    }
                    // ⭐️ GHI LOG: HÀNH ĐỘNG TRUY CẬP THÀNH CÔNG
                    // Ghi log sau khi đã chắc chắn được phép
                    auditLogService.logAction(
                        httpMethod, 
                        email, 
                        "Truy cập " + httpMethod + " " + path + " (URI: " + requestURI + ")"
                    );
                } else {
                     // Role null cũng là không có quyền
                     auditLogService.logAction(
                        "ACCESS_DENIED", 
                        email, 
                        "User không có Role khi truy cập " + httpMethod + " " + path
                    );
                    throw new PermissionException("Bạn không có quyền truy cập vào trang này");
                }
            }
        }
        return true;
    }
}
