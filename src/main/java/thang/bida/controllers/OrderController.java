package thang.bida.controllers;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import thang.bida.model.Order;
import thang.bida.model.OrderItem;
import thang.bida.payload.request.OrderStatusRequest;
import thang.bida.services.OrderService;

@RestController
@RequestMapping("/api/orders")
@CrossOrigin(origins = "*")
public class OrderController {

    private final OrderService orderService;

    public OrderController(OrderService orderService) {
        this.orderService = orderService;
    }

    @PostMapping("/open")
    @PreAuthorize("hasAnyRole('STAFF','MANAGER')")
    public ResponseEntity<Order> openOrder(
            @RequestParam Long tableId,
            @RequestParam Long employeeId) {
        return ResponseEntity.ok(orderService.openOrder(tableId, employeeId));
    }

    @GetMapping("/table/{tableId}")
    @PreAuthorize("hasAnyRole('STAFF','MANAGER')")
    public ResponseEntity<Order> getOrderByTable(@PathVariable Long tableId) {
        return ResponseEntity.ok(orderService.getOrderByTable(tableId));
    }

    // ✅ THÊM ENDPOINT NÀY
    @PatchMapping("/{orderId}/status")
    @PreAuthorize("hasAnyRole('STAFF','MANAGER')")
    public ResponseEntity<Order> updateOrderStatus(
            @PathVariable Long orderId,
            @RequestBody OrderStatusRequest request) {
        return ResponseEntity.ok(orderService.updateOrderStatus(orderId, request.getStatus()));
    }

    @PostMapping("/{orderId}/items")
    @PreAuthorize("hasAnyRole('STAFF','MANAGER')")
    public ResponseEntity<OrderItem> addItem(
            @PathVariable Long orderId,
            @RequestParam Long productId,
            @RequestParam Integer quantity) {
        return ResponseEntity.ok(orderService.addItem(orderId, productId, quantity));
    }

    @PutMapping("/{orderId}/items/{itemId}")
    @PreAuthorize("hasAnyRole('STAFF','MANAGER')")
    public ResponseEntity<?> updateItem(
            @PathVariable Long orderId,
            @PathVariable Long itemId,
            @RequestParam Integer quantity) {
        orderService.updateItemQuantity(orderId, itemId, quantity);
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/{orderId}/items/{itemId}")
    @PreAuthorize("hasAnyRole('STAFF','MANAGER')")
    public ResponseEntity<?> removeItem(
            @PathVariable Long orderId,
            @PathVariable Long itemId) {
        orderService.removeItem(orderId, itemId);
        return ResponseEntity.ok().build();
    }

    @PatchMapping("/{orderId}/close")
    @PreAuthorize("hasAnyRole('STAFF','MANAGER')")
    public ResponseEntity<?> closeOrder(@PathVariable Long orderId) {
        orderService.closeOrder(orderId);
        return ResponseEntity.ok().build();
    }

    // ✅ THÊM ENDPOINT finishPlaying
    @PatchMapping("/{orderId}/finish")
    @PreAuthorize("hasAnyRole('STAFF','MANAGER')")
    public ResponseEntity<Order> finishPlaying(@PathVariable Long orderId) {
        return ResponseEntity.ok(orderService.finishPlaying(orderId));
    }

    // ✅ THÊM ENDPOINT cancelOrder
    @PatchMapping("/{orderId}/cancel")
    @PreAuthorize("hasAnyRole('STAFF','MANAGER')")
    public ResponseEntity<?> cancelOrder(@PathVariable Long orderId) {
        orderService.cancelOrder(orderId);
        return ResponseEntity.ok().build();
    }
}