package thang.bida.dto;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CustomerPointDTO {
    private Long id;
    private String phone;
    private String customerName;
    private Integer totalPoints;
    private String createdAt;
    private String updatedAt;

    // Dùng cho create/update
    public CustomerPointDTO(String phone, String customerName, Integer totalPoints) {
        this.phone = phone;
        this.customerName = customerName;
        this.totalPoints = totalPoints != null ? totalPoints : 0;
    }

    // DTO cho request cộng/trừ điểm
    @Data
    public static class PointsRequest {
        private String phone;
        private Integer points;
    }
}