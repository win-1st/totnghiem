// TimeBasedController.java
package thang.bida.controllers;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import thang.bida.model.Product;
import thang.bida.services.TimeBasedBillingService;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/time-based")
@CrossOrigin(origins = "*")
public class TimeBasedController {

    private final TimeBasedBillingService billingService;

    public TimeBasedController(TimeBasedBillingService billingService) {
        this.billingService = billingService;
    }

    // Lấy thông tin cấu hình tiền giờ
    @GetMapping("/config")
    @PreAuthorize("hasAnyRole('ADMIN','STAFF')")
    public ResponseEntity<?> getTimeBasedConfig() {
        try {
            Product timeProduct = billingService.getTimeBasedProduct();

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("data", Map.of(
                    "id", timeProduct.getId(),
                    "name", timeProduct.getName(),
                    "pricePerMinute", timeProduct.getPricePerMinute(),
                    "pricePerHour", timeProduct.getPricePerMinute().multiply(BigDecimal.valueOf(60)),
                    "isActive", timeProduct.getActive(),
                    "description", timeProduct.getDescription()));

            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", e.getMessage());
            return ResponseEntity.status(404).body(response);
        }
    }

    // Cập nhật giá tiền giờ
    @PutMapping("/config")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> updateTimeBasedConfig(
            @RequestParam BigDecimal pricePerMinute,
            @RequestParam(required = false) String name,
            @RequestParam(required = false) String description) {
        try {
            if (pricePerMinute.compareTo(BigDecimal.ZERO) <= 0) {
                throw new RuntimeException("Giá mỗi phút phải lớn hơn 0");
            }

            String productName = name != null ? name : "Tiền giờ bàn Billiards";
            String productDesc = description != null ? description : "Tính tiền theo thời gian sử dụng bàn";

            Product updated = billingService.createOrUpdateTimeBasedProduct(pricePerMinute, productName, productDesc);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Cập nhật cấu hình tiền giờ thành công");
            response.put("data", Map.of(
                    "pricePerMinute", updated.getPricePerMinute(),
                    "pricePerHour", updated.getPricePerMinute().multiply(BigDecimal.valueOf(60))));

            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    // Kiểm tra cấu hình
    @GetMapping("/check")
    @PreAuthorize("hasAnyRole('ADMIN','STAFF')")
    public ResponseEntity<?> checkConfig() {
        boolean hasConfig = billingService.hasTimeBasedProduct();

        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("hasTimeBasedProduct", hasConfig);

        if (hasConfig) {
            Product product = billingService.getTimeBasedProduct();
            response.put("pricePerMinute", product.getPricePerMinute());
            response.put("isActive", product.getActive());
        }

        return ResponseEntity.ok(response);
    }
}