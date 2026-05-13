package thang.bida.controllers;

import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.stereotype.Controller;

import thang.bida.model.Order;
import thang.bida.services.OrderService;

import java.util.List;

@Controller
public class WebSocketController {

    private final OrderService orderService;

    public WebSocketController(OrderService orderService) {
        this.orderService = orderService;
    }

    @MessageMapping("/orders.subscribe")
    @SendTo("/topic/orders")
    public List<Order> subscribeToOrders() {
        return orderService.getPendingOrders();
    }

    @MessageMapping("/order.update")
    @SendTo("/topic/order-updates")
    public Order updateOrder(Order order) {
        return order;
    }
}