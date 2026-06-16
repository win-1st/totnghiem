package thang.bida.dto;

import lombok.Data;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

@Data
public class UpdateStaffRequest {
    @Pattern(regexp = "(84|0[3|5|7|8|9])+([0-9]{8})\\b", message = "Số điện thoại không hợp lệ")
    @Size(min = 10, max = 11, message = "Số điện thoại phải có 10-11 số")
    private String phone; // Đã sửa: username -> phone

    @Email(message = "Email không hợp lệ")
    private String email;

    @Size(min = 6, message = "Mật khẩu tối thiểu 6 ký tự")
    private String password;

    private String fullName;
    private String address;
    private String imageUrl;
    private String role;
}