package vn.hoidanit.jobhunter.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import vn.hoidanit.jobhunter.domain.AuditLog;
import vn.hoidanit.jobhunter.service.AuditLogService;
import java.util.List;


@RestController
@RequestMapping("/api/v1/admin/audit-logs")
public class AuditLogController {

    private final AuditLogService auditLogService;

    public AuditLogController(AuditLogService auditLogService) {
        this.auditLogService = auditLogService;
    }

    /**
     * GET /api/v1/admin/audit-logs
     * Endpoint để Admin xem tất cả lịch sử hoạt động.
     * Cần bảo vệ bằng Spring Security (chỉ Role ADMIN mới được truy cập).
     */
    @GetMapping
    public ResponseEntity<List<AuditLog>> getAdminAuditLogs() {
        List<AuditLog> logs = auditLogService.getAllLogs();
        return ResponseEntity.ok(logs);
    }
    
    /**
     * Endpoint demo để nhanh chóng tạo ra vài bản ghi log test.
     * Truy cập: GET /api/v1/admin/audit-logs/log-demo
     */
    @GetMapping("/log-demo")
    public ResponseEntity<String> logDemoAction() {
        // Giả lập Admin đăng nhập
        auditLogService.logAction("LOGIN", "admin_user@hoidanit.vn", "Đăng nhập thành công.");
        
        // Giả lập Admin tạo mới user
        auditLogService.logAction("USER_CREATE", "admin_user@hoidanit.vn", "Tạo mới user: Lê Văn B (ID: 102)");
        
        // Giả lập Admin xóa công ty
        auditLogService.logAction("COMPANY_DELETE", "admin_user@hoidanit.vn", "Xóa công ty: Google Việt Nam (ID: 7)");
        
        return ResponseEntity.ok("Đã ghi log demo thành công. Truy cập /api/v1/admin/audit-logs để xem.");
    }
}
