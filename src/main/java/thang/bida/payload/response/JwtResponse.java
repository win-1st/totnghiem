package thang.bida.payload.response;

import java.util.List;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class JwtResponse {
    private Long id;
    private String username;
    private String email;
    private List<String> roles;
    private String tokenType = "Bearer";
    private String accessToken;
    private String fullName;

    // Constructor đầy đủ
    public JwtResponse(String accessToken, Long id, String username, String fullName, String email,
            List<String> roles) {
        this.accessToken = accessToken;
        this.id = id;
        this.username = username;
        this.fullName = fullName;
        this.email = email;
        this.roles = roles;
    }
}