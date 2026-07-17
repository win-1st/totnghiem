package thang.bida.services;

import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import thang.bida.model.Order;

import java.time.LocalDate;

@Service
public class WebSocketService {

    private final SimpMessagingTemplate messagingTemplate;

    public WebSocketService(SimpMessagingTemplate messagingTemplate) {
        this.messagingTemplate = messagingTemplate;
    }

    public void notifyNewOrder(Order order) {
        messagingTemplate.convertAndSend("/topic/orders", order);
    }

    public void notifyOrderUpdate(Order order) {
        messagingTemplate.convertAndSend("/topic/order-updates", order);
    }

    public void notifyTableStatus(Long tableId, String status) {
        messagingTemplate.convertAndSend("/topic/table-status",
                new TableStatusMessage(tableId, status));
    }

    // 🆕 Thêm method này để gửi message với đầy đủ thông tin
    public void sendTableStatusMessage(TableStatusMessage message) {
        messagingTemplate.convertAndSend("/topic/table-status", message);
    }

    // ========== INNER CLASS ==========
    public static class TableStatusMessage {
        private Long tableId;
        private String status;
        private String customerName;
        private LocalDate reservationDate;

        public TableStatusMessage() {
        }

        public TableStatusMessage(Long tableId, String status) {
            this.tableId = tableId;
            this.status = status;
        }

        // Getters and Setters
        public Long getTableId() {
            return tableId;
        }

        public void setTableId(Long tableId) {
            this.tableId = tableId;
        }

        public String getStatus() {
            return status;
        }

        public void setStatus(String status) {
            this.status = status;
        }

        public String getCustomerName() {
            return customerName;
        }

        public void setCustomerName(String customerName) {
            this.customerName = customerName;
        }

        public LocalDate getReservationDate() {
            return reservationDate;
        }

        public void setReservationDate(LocalDate reservationDate) {
            this.reservationDate = reservationDate;
        }

        @Override
        public String toString() {
            return "TableStatusMessage{" +
                    "tableId=" + tableId +
                    ", status='" + status + '\'' +
                    ", customerName='" + customerName + '\'' +
                    ", reservationDate=" + reservationDate +
                    '}';
        }
    }
}