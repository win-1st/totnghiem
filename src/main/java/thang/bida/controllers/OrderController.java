package thang.bida.controllers;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.stream.Collectors;
import java.util.Map;

import thang.bida.dto.OrderStatusRequest;
import thang.bida.model.Order;
import thang.bida.model.OrderItem;
import thang.bida.services.OrderService;
import thang.bida.services.UserDetailsImpl;

@RestController
@RequestMapping("/api/orders")
@CrossOrigin(origins = "*")
public class OrderController {

    private final OrderService orderService;

    public OrderController(OrderService orderService) {
        this.orderService = orderService;
    }

    @PostMapping("/open-table/{tableId}")
    @PreAuthorize("hasAnyRole('ADMIN','STAFF')")
    public ResponseEntity<Order> openOrderForTable(@PathVariable Long tableId) {
        UserDetailsImpl userDetails = (UserDetailsImpl) SecurityContextHolder.getContext().getAuthentication()
                .getPrincipal();
        Long employeeId = userDetails.getId();
        System.out.println("=== OPEN ORDER FOR TABLE ===");
        System.out.println("Table ID: " + tableId);
        System.out.println("Employee ID: " + employeeId);
        System.out.println("Employee Name: " + userDetails.getUsername());
        return ResponseEntity.ok(orderService.openOrder(tableId, employeeId));
    }

    @GetMapping("/table/{tableId}")
    @PreAuthorize("hasAnyRole('ADMIN','STAFF')")
    public ResponseEntity<?> getOrderByTable(@PathVariable Long tableId) {

        Order order = orderService.getOrderByTable(tableId);

        if (order == null) {
            return ResponseEntity.ok(null);
        }

        return ResponseEntity.ok(order);
    }

    @GetMapping("/{orderId}")
    @PreAuthorize("hasAnyRole('ADMIN','STAFF')")
    public ResponseEntity<Order> getOrderById(@PathVariable Long orderId) {
        return ResponseEntity.ok(orderService.getOrderById(orderId));
    }

    // SỬA: Thay vì gọi method không tồn tại, lấy items từ order
    @GetMapping("/{orderId}/items")
    @PreAuthorize("hasAnyRole('ADMIN','STAFF')")
    public ResponseEntity<List<OrderItem>> getOrderItems(@PathVariable Long orderId) {
        Order order = orderService.getOrderById(orderId);
        return ResponseEntity.ok(order.getItems());
    }

    @GetMapping("/all")
    @PreAuthorize("hasAnyRole('ADMIN','STAFF')")
    public ResponseEntity<?> getAllOrders() {
        List<Order> orders = orderService.getAllOrders();

        List<Map<String, Object>> result = orders.stream().map(order -> {
            Map<String, Object> map = new HashMap<>();
            map.put("id", order.getId());
            map.put("totalAmount", order.getTotalAmount());
            map.put("status", order.getStatus());
            map.put("createdAt", order.getCreatedAt());
            map.put("tableNumber", order.getTable() != null ? order.getTable().getNumber() : null);
            map.put("tableId", order.getTable() != null ? order.getTable().getId() : null);
            return map;
        }).collect(Collectors.toList());

        return ResponseEntity.ok(Map.of("success", true, "data", result, "count", result.size()));
    }

    @PatchMapping("/{orderId}/status")
    @PreAuthorize("hasAnyRole('ADMIN','STAFF')")
    public ResponseEntity<Order> updateOrderStatus(
            @PathVariable Long orderId,
            @RequestBody OrderStatusRequest request) {
        return ResponseEntity.ok(orderService.updateOrderStatus(orderId, request.getStatus()));
    }

    @PostMapping("/{orderId}/items")
    @PreAuthorize("hasAnyRole('ADMIN','STAFF')")
    public ResponseEntity<OrderItem> addItem(
            @PathVariable Long orderId,
            @RequestParam Long productId,
            @RequestParam Integer quantity,
            @RequestParam(required = false) BigDecimal unitPrice) { // 🆕 Thêm unitPrice
        System.out.println("=== ADD ITEM ===");
        System.out.println("Order ID: " + orderId);
        System.out.println("Product ID: " + productId);
        System.out.println("Quantity: " + quantity);
        System.out.println("Unit Price: " + unitPrice);

        if (unitPrice != null && unitPrice.compareTo(BigDecimal.ZERO) > 0) {
            return ResponseEntity.ok(orderService.addItemWithPrice(orderId, productId, quantity, unitPrice));
        }
        return ResponseEntity.ok(orderService.addItem(orderId, productId, quantity));
    }

    @PutMapping("/{orderId}/items/{itemId}")
    @PreAuthorize("hasAnyRole('ADMIN','STAFF')")
    public ResponseEntity<?> updateItem(
            @PathVariable Long orderId,
            @PathVariable Long itemId,
            @RequestParam Integer quantity,
            @RequestParam(required = false) BigDecimal unitPrice) {
        System.out.println("=== UPDATE ITEM ===");
        System.out.println("Order ID: " + orderId);
        System.out.println("Item ID: " + itemId);
        System.out.println("Quantity: " + quantity);

        orderService.updateItemQuantity(orderId, itemId, quantity, unitPrice);
        return ResponseEntity.ok().build();
    }

    @PatchMapping("/{orderId}/adjust-time")
    @PreAuthorize("hasAnyRole('ADMIN','STAFF')")
    public ResponseEntity<?> adjustPlayTime(
            @PathVariable Long orderId,
            @RequestParam Integer additionalMinutes) {
        System.out.println("=== ADJUST PLAY TIME ===");
        System.out.println("Order ID: " + orderId);
        System.out.println("Additional Minutes: " + additionalMinutes);

        orderService.adjustPlayTime(orderId, additionalMinutes);
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/{orderId}/items/{itemId}")
    @PreAuthorize("hasAnyRole('ADMIN','STAFF')")
    public ResponseEntity<?> removeItem(
            @PathVariable Long orderId,
            @PathVariable Long itemId) {
        System.out.println("=== REMOVE ITEM ===");
        System.out.println("Order ID: " + orderId);
        System.out.println("Item ID: " + itemId);

        orderService.removeItem(orderId, itemId);
        return ResponseEntity.ok().build();
    }

    @PatchMapping("/{orderId}/close")
    @PreAuthorize("hasAnyRole('ADMIN','STAFF')")
    public ResponseEntity<?> closeOrder(@PathVariable Long orderId) {
        System.out.println("=== CLOSE ORDER ===");
        System.out.println("Order ID: " + orderId);

        orderService.closeOrder(orderId);
        return ResponseEntity.ok().build();
    }

    @PatchMapping("/{orderId}/finish")
    @PreAuthorize("hasAnyRole('ADMIN','STAFF')")
    public ResponseEntity<Order> finishPlaying(@PathVariable Long orderId) {
        System.out.println("=== FINISH PLAYING ===");
        System.out.println("Order ID: " + orderId);

        return ResponseEntity.ok(orderService.finishPlaying(orderId));
    }

    @PatchMapping("/{orderId}/cancel")
    @PreAuthorize("hasAnyRole('ADMIN','STAFF')")
    public ResponseEntity<?> cancelOrder(@PathVariable Long orderId) {
        orderService.cancelOrder(orderId);
        return ResponseEntity.ok().build();
    }
}