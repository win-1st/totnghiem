package thang.bida.dto;

import java.util.List;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class MomoPaymentRequest {

    private Long orderId;
    private Double amount;
    private String orderInfo;
    private String extraData;
    private String tableName;
    private List<OrderItem> items;

    public MomoPaymentRequest() {
    }

    public MomoPaymentRequest(Long orderId, Double amount, String orderInfo, String extraData) {
        this.orderId = orderId;
        this.amount = amount;
        this.orderInfo = orderInfo;
        this.extraData = extraData;
    }

    @Getter
    @Setter
    public static class OrderItem {
        private String name;
        private Integer quantity;
        private Double price;
    }
}