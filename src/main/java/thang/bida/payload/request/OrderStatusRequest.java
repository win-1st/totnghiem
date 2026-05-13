package thang.bida.payload.request;

import thang.bida.model.Order;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class OrderStatusRequest {

    private Order.OrderStatus status;
}