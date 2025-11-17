package vn.hoidanit.jobhunter.domain;

import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;


@Entity
@Table(name = "audit_log")
@Getter
@Setter
public class AuditLog {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long id;

    // Hành động được thực hiện (Ví dụ: "LOGIN", "USER_CREATE", "COMPANY_DELETE")
    @Column(nullable = false)
    private String action; 

    // Ai đã thực hiện hành động này (tên hoặc email của Admin)
    @Column(nullable = false)
    private String performedBy; 

    // Chi tiết của hành động (Ví dụ: "Tạo mới user: John Doe (id: 5)", "Xóa company: ABC Corp (id: 10)")
    @Column(columnDefinition = "TEXT")
    private String details; 

    // Thời điểm hành động xảy ra
    @Column(nullable = false)
    private Instant timestamp; 

    // Constructors
    public AuditLog() {
        this.timestamp = Instant.now();
    }

    public AuditLog(String action, String performedBy, String details) {
        this.action = action;
        this.performedBy = performedBy;
        this.details = details;
        this.timestamp = Instant.now();
    }
}
