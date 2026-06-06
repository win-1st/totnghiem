package thang.bida.payos;

import lombok.Data;
import java.util.List;

@Data
public class CreatePaymentRequest {
    private long orderCode;
    private int amount;
    private String description;
    private String returnUrl;
    private String cancelUrl;
    private List<Item> items;

    @Data
    public static class Item {
        private String name;
        private int quantity;
        private int price;
    }
}
