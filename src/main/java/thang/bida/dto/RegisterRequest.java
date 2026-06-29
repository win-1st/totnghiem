package thang.bida.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class RegisterRequest {
    @NotBlank(message = "Số điện thoại không được để trống")
    @Pattern(regexp = "0[3|5|7|8|9][0-9]{8}", message = "Số điện thoại không hợp lệ (phải bắt đầu bằng 0 và có 10 số)")
    @Size(min = 10, max = 10, message = "Số điện thoại phải có đúng 10 số")
    private String phone;

    @Email(message = "Email không hợp lệ")
    private String email;

    @NotBlank(message = "Mật khẩu không được để trống")
    @Size(min = 6, message = "Mật khẩu tối thiểu 6 ký tự")
    private String password;

    private String fullName;
    private String address;
    private String imageUrl;
    private String role;
}