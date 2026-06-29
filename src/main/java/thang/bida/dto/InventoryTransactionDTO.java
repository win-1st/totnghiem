package thang.bida.dto;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class InventoryTransactionDTO {
    private Long id;
    private Long productId;
    private String productName;
    private String productCategory;
    private String transactionType;
    private Integer quantity;
    private Integer beforeQuantity;
    private Integer afterQuantity;
    private String note;
    private Long userId;
    private String userFullName;
    private LocalDateTime createdAt;
}