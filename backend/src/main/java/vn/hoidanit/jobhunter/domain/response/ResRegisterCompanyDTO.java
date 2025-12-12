package vn.hoidanit.jobhunter.domain.response;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
// Kế thừa từ ResCreateUserDTO để giữ lại thông tin User cơ bản
public class ResRegisterCompanyDTO extends ResCreateUserDTO {
    
    private String paymentUrl; // URL thanh toán hoặc chuỗi dữ liệu QR
    private String message;
    private long paymentId; // ID của bản ghi Payment vừa tạo

    public ResRegisterCompanyDTO(ResCreateUserDTO userDTO) {
        // Copy thông tin cơ bản từ ResCreateUserDTO
        this.setId(userDTO.getId());
        this.setName(userDTO.getName());
        this.setEmail(userDTO.getEmail());
        this.setCreatedAt(userDTO.getCreatedAt());
        this.setRole(userDTO.getRole());
    }
}