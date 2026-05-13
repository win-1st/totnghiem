package thang.bida.services;

import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import thang.bida.model.Order;

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

    public static class TableStatusMessage {
        private Long tableId;
        private String status;

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
    }
}