package thang.bida.dto;

import java.util.List;

import lombok.Data;

@Data
public class CreateStaffRequest {
    private String username;
    private String email;
    private String password;
    private String fullName;
    private String phone;
    private String address;
    private String imageUrl;
    private List<String> roles;
}
