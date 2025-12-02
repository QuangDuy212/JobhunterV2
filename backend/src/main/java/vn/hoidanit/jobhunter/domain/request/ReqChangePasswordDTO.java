package vn.hoidanit.jobhunter.domain.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ReqChangePasswordDTO {

    @NotBlank(message = "Mật khẩu cũ không được để trống")
    private String currentPassword;

    @NotBlank(message = "Mật khẩu mới không được để trống")
    private String newPassword;

    // Trường confirmPassword nên được kiểm tra ở phía Frontend (antd)
    // hoặc thêm validation custom nếu cần ở backend, nhưng thường chỉ cần
    // newPassword là đủ cho logic thay đổi.
}
