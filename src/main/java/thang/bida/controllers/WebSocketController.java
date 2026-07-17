// ========== FILE: WebSocketController.java ==========
package thang.bida.controllers;

import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.stereotype.Controller;

import thang.bida.model.Order;
import thang.bida.model.BidaTable;
import thang.bida.services.OrderService;
import thang.bida.services.BidaTableService;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Controller
public class WebSocketController {

    private final OrderService orderService;
    private final BidaTableService bidaTableService;

    public WebSocketController(OrderService orderService, BidaTableService bidaTableService) {
        this.orderService = orderService;
        this.bidaTableService = bidaTableService;
    }

    // ========== EXISTING METHODS ==========

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

    // ========== 🆕 NEW METHODS FOR RESERVATION ==========

    /**
     * Nhận và broadcast đặt bàn mới
     * Client gửi: /app/reservation.new
     * Server broadcast: /topic/table-status
     */
    @MessageMapping("/reservation.new")
    @SendTo("/topic/table-status")
    public Map<String, Object> newReservation(Map<String, Object> reservation) {
        System.out.println("📤 New reservation broadcasted:");
        System.out.println("   - Customer: " + reservation.get("customerName"));
        System.out.println("   - Table: " + reservation.get("tableNumber"));
        System.out.println("   - Phone: " + reservation.get("customerPhone"));

        // Tạo response với thông tin đặt bàn
        Map<String, Object> response = new HashMap<>();
        response.put("tableId", reservation.get("tableId"));
        response.put("tableNumber", reservation.get("tableNumber"));
        response.put("customerName", reservation.get("customerName"));
        response.put("customerPhone", reservation.get("customerPhone"));
        response.put("reservationDate", reservation.get("reservationDate"));
        response.put("reservationTime", reservation.get("reservationTime"));
        response.put("numberOfGuests", reservation.get("numberOfGuests"));
        response.put("status", "RESERVED");
        response.put("timestamp", System.currentTimeMillis());
        response.put("message", "Có đặt bàn mới từ " + reservation.get("customerName"));

        // Cập nhật trạng thái bàn thành RESERVED
        try {
            Long tableId = Long.valueOf(reservation.get("tableId").toString());
            bidaTableService.updateTableStatus(tableId, BidaTable.TableStatus.RESERVED);
            System.out.println("✅ Table " + tableId + " updated to RESERVED");
        } catch (Exception e) {
            System.err.println("❌ Error updating table status: " + e.getMessage());
        }

        return response;
    }

    /**
     * Cập nhật trạng thái bàn
     * Client gửi: /app/table.status
     * Server broadcast: /topic/table-status
     */
    @MessageMapping("/table.status")
    @SendTo("/topic/table-status")
    public Map<String, Object> updateTableStatus(Map<String, Object> message) {
        System.out.println("📤 Table status update: " + message.get("tableId") + " -> " + message.get("status"));

        // Cập nhật database
        try {
            Long tableId = Long.valueOf(message.get("tableId").toString());
            String statusStr = message.get("status").toString();
            BidaTable.TableStatus status = BidaTable.TableStatus.valueOf(statusStr);

            bidaTableService.updateTableStatus(tableId, status);
            System.out.println("✅ Table " + tableId + " updated to " + status);
        } catch (Exception e) {
            System.err.println("❌ Error updating table status: " + e.getMessage());
        }

        return message;
    }

    /**
     * Broadcast khi có check-in
     * Client gửi: /app/reservation.checkin
     * Server broadcast: /topic/table-status
     */
    @MessageMapping("/reservation.checkin")
    @SendTo("/topic/table-status")
    public Map<String, Object> checkInReservation(Map<String, Object> data) {
        System.out.println("📤 Check-in broadcasted: Table " + data.get("tableId"));

        Map<String, Object> response = new HashMap<>();
        response.put("tableId", data.get("tableId"));
        response.put("tableNumber", data.get("tableNumber"));
        response.put("customerName", data.get("customerName"));
        response.put("status", "OCCUPIED");
        response.put("timestamp", System.currentTimeMillis());
        response.put("message", "Khách đã check-in bàn " + data.get("tableNumber"));

        // Cập nhật trạng thái bàn thành OCCUPIED
        try {
            Long tableId = Long.valueOf(data.get("tableId").toString());
            bidaTableService.updateTableStatus(tableId, BidaTable.TableStatus.OCCUPIED);
            System.out.println("✅ Table " + tableId + " updated to OCCUPIED (check-in)");
        } catch (Exception e) {
            System.err.println("❌ Error updating table status: " + e.getMessage());
        }

        return response;
    }

    /**
     * Broadcast khi hủy đặt bàn
     * Client gửi: /app/reservation.cancel
     * Server broadcast: /topic/table-status
     */
    @MessageMapping("/reservation.cancel")
    @SendTo("/topic/table-status")
    public Map<String, Object> cancelReservation(Map<String, Object> data) {
        System.out.println("📤 Cancellation broadcasted: Table " + data.get("tableId"));

        Map<String, Object> response = new HashMap<>();
        response.put("tableId", data.get("tableId"));
        response.put("tableNumber", data.get("tableNumber"));
        response.put("status", "FREE");
        response.put("timestamp", System.currentTimeMillis());
        response.put("message", "Đã hủy đặt bàn " + data.get("tableNumber"));

        // Cập nhật trạng thái bàn thành FREE
        try {
            Long tableId = Long.valueOf(data.get("tableId").toString());
            bidaTableService.updateTableStatus(tableId, BidaTable.TableStatus.FREE);
            System.out.println("✅ Table " + tableId + " updated to FREE (cancelled)");
        } catch (Exception e) {
            System.err.println("❌ Error updating table status: " + e.getMessage());
        }

        return response;
    }
}