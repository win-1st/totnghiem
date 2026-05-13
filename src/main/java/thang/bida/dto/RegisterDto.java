package thang.bida.dto;

import lombok.Data;
import java.util.Set;

@Data
public class RegisterDto {
    private String username;
    private String email;
    private String password;
    private Set<String> roles;
    private String fullName;
    private String phone;
    private String address;
    private String imageUrl;
}