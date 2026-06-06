package thang.bida.dto;

import lombok.Data;

@Data
public class UpdateStaffRequest {
    private String username;
    private String email;
    private String password;
    private String fullName;
    private String phone;
    private String address;
    private String imageUrl;
    private String role;
}
