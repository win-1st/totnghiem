package thang.bida.controllers;

import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import thang.bida.payos.PayOSClient;
import thang.bida.services.PayOSWebhookService;

@RestController
@RequestMapping("/api/payos")
public class PayOSWebhookController {

    private final PayOSWebhookService webhookService;
    private final PayOSClient payOSClient;

    public PayOSWebhookController(PayOSWebhookService webhookService, PayOSClient payOSClient) {
        this.webhookService = webhookService;
        this.payOSClient = payOSClient;
    }

    @PostMapping("/webhook")
    public ResponseEntity<?> handleWebhook(@RequestBody Map<String, Object> payload) {
        try {
            System.out.println(">>> Received PayOS webhook: " + payload);

            // Verify signature nếu PayOS gửi
            String signature = (String) payload.get("signature");
            Map<String, Object> data = (Map<String, Object>) payload.get("data");

            if (signature != null && !webhookService.verifySignature(data, signature)) {
                return ResponseEntity.status(401).body("Invalid signature");
            }

            // Xử lý webhook
            String code = (String) data.get("code");
            String orderCode = String.valueOf(data.get("orderCode"));

            // 00 = success, 01 = cancelled
            if ("01".equals(code)) {
                System.out.println(">>> Payment cancelled for order: " + orderCode);
            } else if ("00".equals(code)) {
                System.out.println(">>> Payment success for order: " + orderCode);
            }

            return ResponseEntity.ok(Map.of("success", true));

        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(500).body("Webhook processing failed");
        }
    }

    // THÊM ENDPOINT NÀY
    @GetMapping("/check-status/{orderCode}")
    public ResponseEntity<?> checkPaymentStatus(@PathVariable String orderCode) {
        try {
            Map<String, Object> status = payOSClient.getPaymentStatus(orderCode);
            return ResponseEntity.ok(status);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(500).body(Map.of("error", "Cannot check payment status"));
        }
    }
}