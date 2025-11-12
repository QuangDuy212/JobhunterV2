package vn.hoidanit.jobhunter.util.constant;

public enum JobStatusEnum {
    REVIEWING, // Trạng thái mặc định khi tạo Job
    APPROVED,  // Đã được AI kiểm duyệt và chấp thuận
    REJECTED,  // Bị từ chối do có nội dung phản cảm
    INACTIVE,  // Công việc không còn hoạt động (ví dụ: đã hết hạn)
    DRAFT      // Bản nháp (nếu có)
}
