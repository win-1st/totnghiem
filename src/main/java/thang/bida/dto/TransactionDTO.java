package thang.bida.dto;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class TransactionDTO {
    private Long id;
    private Integer points;
    private String type; // EARN, REDEEM, BONUS
    private String description;
    private LocalDateTime createdAt;
}