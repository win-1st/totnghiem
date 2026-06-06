package thang.bida.controllers;

import java.util.HashMap;
import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import thang.bida.payos.CreatePaymentRequest;
import thang.bida.payos.PayOSClient;

@RestController
@RequestMapping("/api/payos")
public class PayOSController {

    private final PayOSClient payOsClient;

    public PayOSController(PayOSClient payOSClient) {
        this.payOsClient = payOSClient;
    }

    @PostMapping("/create")
    @PreAuthorize("hasAnyRole('ADMIN','STAFF')") // THÊM xác thực
    public ResponseEntity<?> createPayment(@RequestBody CreatePaymentRequest req) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("orderCode", req.getOrderCode());
        payload.put("amount", req.getAmount());
        payload.put("description", req.getDescription());
        payload.put("returnUrl", req.getReturnUrl());
        payload.put("cancelUrl", req.getCancelUrl());

        if (req.getItems() != null && !req.getItems().isEmpty()) {
            payload.put("items", req.getItems());
        }

        Map<String, Object> res = payOsClient.createPaymentLink(payload);
        return ResponseEntity.ok(res);
    }
}