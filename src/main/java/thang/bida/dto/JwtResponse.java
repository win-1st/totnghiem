package thang.bida.dto;

import java.util.List;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class JwtResponse {
    private Long id;
    private String phone;
    private String email;
    private List<String> roles;
    private String tokenType = "Bearer";
    private String token;
    private String fullName;

    public JwtResponse(String token, Long id, String phone, String fullName, String email, List<String> roles) {
        this.token = token;
        this.id = id;
        this.phone = phone;
        this.fullName = fullName;
        this.email = email;
        this.roles = roles;
    }
}