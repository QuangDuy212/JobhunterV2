package vn.hoidanit.jobhunter.domain.response;

import lombok.Getter;
import lombok.Setter;

// DTO để parse phản hồi JSON từ Gemini
@Getter
@Setter
public class AIReviewResult {
    // Giá trị TRUE nếu nội dung hoàn toàn OK, FALSE nếu có vấn đề
    private boolean is_safe; 
    
    // Lý do nếu không an toàn
    private String reason; 

    // Mức độ tin cậy (ví dụ: 0.0 - 1.0) hoặc chỉ là một mô tả đơn giản
    private String confidence;
}
