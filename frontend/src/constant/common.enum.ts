export const JobStatusEnum = {
    REVIEWING:"REVIEWING", // Trạng thái mặc định khi tạo Job
    APPROVED:"APPROVED",  // Đã được AI kiểm duyệt và chấp thuận
    REJECTED:"REJECTED",  // Bị từ chối do có nội dung phản cảm
    INACTIVE:"INACTIVE",  // Công việc không còn hoạt động (ví dụ: đã hết hạn)
    DRAFT:"INACTIVE"      // Bản nháp (nếu có)
}