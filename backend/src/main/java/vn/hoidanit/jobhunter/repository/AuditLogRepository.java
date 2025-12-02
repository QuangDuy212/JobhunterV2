package vn.hoidanit.jobhunter.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import vn.hoidanit.jobhunter.domain.AuditLog;

import java.time.Instant;

@Repository
public interface AuditLogRepository extends JpaRepository<AuditLog, Long> {

    /**
     * Xóa tất cả các bản ghi có timestamp CŨ HƠN thời điểm truyền vào.
     * Phương thức này được sử dụng bởi @Scheduled cleanup job.
     * @param timestampBefore Thời điểm giới hạn (chỉ xóa các log cũ hơn thời điểm này)
     */
    void deleteByTimestampBefore(Instant timestampBefore);
}
