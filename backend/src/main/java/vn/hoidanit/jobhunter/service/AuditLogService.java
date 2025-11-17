package vn.hoidanit.jobhunter.service;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import vn.hoidanit.jobhunter.domain.AuditLog;
import vn.hoidanit.jobhunter.repository.AuditLogRepository;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

@Service
public class AuditLogService {

    private final AuditLogRepository auditLogRepository;

    public AuditLogService(AuditLogRepository auditLogRepository) {
        this.auditLogRepository = auditLogRepository;
    }

    /**
     * Ghi lại một hành động quản trị mới.
     * Các Service khác (UserService, CompanyService...) sẽ gọi hàm này.
     * @param action Hành động (Ví dụ: "USER_CREATE", "COMPANY_DELETE", "LOGIN")
     * @param performedBy Người thực hiện (Ví dụ: "admin@example.com")
     * @param details Chi tiết hành động (Ví dụ: "Đã tạo user id: 10")
     */
    public void logAction(String action, String performedBy, String details) {
        AuditLog log = new AuditLog(action, performedBy, details);
        auditLogRepository.save(log);
        System.out.println("✅ LOGGED: [" + action + "] by " + performedBy + " - " + details);
    }

    /**
     * Lấy tất cả lịch sử hoạt động để hiển thị trên Dashboard Admin.
     * (Có thể thêm Pageable cho phân trang trong thực tế).
     * @return Danh sách các bản ghi AuditLog
     */
    public List<AuditLog> getAllLogs() {
        // Tạo yêu cầu Phân trang: Lấy trang 0, 10 phần tử
        // Sắp xếp theo 'timestamp' giảm dần (DESC)
        Pageable topTen = PageRequest.of(0, 10, Sort.by(Sort.Direction.DESC, "timestamp"));
        
        // Dùng findAll(Pageable) và lấy nội dung (getContent)
        return auditLogRepository.findAll(topTen).getContent();
    }

    /**
     * CHỨC NĂNG DỌN DẸP LỊCH SỬ TỰ ĐỘNG
     * * Cấu hình Cron: "0 0 0 * * *" 
     * -> Chạy lúc 00 phút, 00 giây, 00 giờ (12h đêm/0h sáng) mỗi ngày.
     * * Lưu ý: Nếu muốn log tồn tại ít nhất 12h, bạn nên xóa log cũ hơn 1 ngày
     * hoặc đặt lịch chạy 2 lần/ngày (ví dụ: cron = "0 0 0,12 * * *").
     */
    @Scheduled(cron = "0 0 0 * * *") // Chạy lúc 00:00:00 mỗi ngày
    @Transactional // Bắt buộc phải có @Transactional cho thao tác xóa
    public void deleteOldLogs() {
        // Xóa các bản ghi cũ hơn 1 ngày (điều chỉnh ChronoUnit.DAYS nếu muốn giữ lâu hơn)
        Instant oneDayAgo = Instant.now().minus(1, ChronoUnit.DAYS);
        
        System.out.println("⏰ SCHEDULED CLEANUP: Bắt đầu xóa log cũ hơn: " + oneDayAgo);
        
        auditLogRepository.deleteByTimestampBefore(oneDayAgo);
        
        System.out.println("✅ SCHEDULED CLEANUP: Đã hoàn tất xóa log cũ.");
    }
}
