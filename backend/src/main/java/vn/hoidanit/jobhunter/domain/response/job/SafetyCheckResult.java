package vn.hoidanit.jobhunter.domain.response.job;

import java.util.List;

import lombok.*;
import lombok.experimental.FieldDefaults;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class SafetyCheckResult {
    String status; // APPROVED hoặc REJECTED
    String reason;
    List<String> violatedWords; // Danh sách từ cần bôi đậm

    public boolean isSafe() {
        return "APPROVED".equalsIgnoreCase(this.status);
    }
}
