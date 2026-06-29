package thang.bida.controllers;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import thang.bida.model.Bill;
import thang.bida.model.Bill.PaymentMethod;
import thang.bida.services.BillService;

@RestController
@RequestMapping("/api/bills")
@CrossOrigin(origins = "*")
public class BillController {

    private final BillService billService;

    public BillController(BillService billService) {
        this.billService = billService;
    }

    // Tạo hóa đơn - Hỗ trợ customerPhone và promotionId
    @PostMapping("/create")
    @PreAuthorize("hasAnyRole('ADMIN','STAFF')")
    public ResponseEntity<?> createBill(
            @RequestParam Long orderId,
            @RequestParam PaymentMethod method,
            @RequestParam(required = false) String customerPhone,
            @RequestParam(required = false) Long promotionId) {

        Bill bill = billService.createBill(orderId, method, customerPhone, promotionId);

        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("data", bill);
        response.put("message", "Tạo hóa đơn thành công");

        return ResponseEntity.ok(response);
    }

    @GetMapping("/all")
    @PreAuthorize("hasAnyRole('ADMIN','STAFF')")
    public ResponseEntity<?> getAllBills() {
        List<Bill> bills = billService.getAllBills();

        List<Map<String, Object>> result = bills.stream().map(bill -> {
            Map<String, Object> map = new HashMap<>();
            map.put("id", bill.getId());
            map.put("orderId", bill.getOrder() != null ? bill.getOrder().getId() : null);
            map.put("totalAmount", bill.getTotalAmount());
            map.put("paymentMethod", bill.getPaymentMethod());
            map.put("paymentStatus", bill.getPaymentStatus());
            map.put("createdAt", bill.getCreatedAt());
            map.put("tableNumber", bill.getTable() != null ? bill.getTable().getNumber() : null);
            map.put("tableFee", bill.getOrder() != null ? bill.getOrder().getTableFee() : 0);
            map.put("productFee", bill.getOrder() != null ? bill.getOrder().getProductFee() : 0);
            return map;
        }).collect(Collectors.toList());

        return ResponseEntity.ok(Map.of("success", true, "data", result, "count", result.size()));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','STAFF')")
    public ResponseEntity<?> getBillById(@PathVariable Long id) {
        Bill bill = billService.getBillById(id);

        Map<String, Object> response = new HashMap<>();
        response.put("id", bill.getId());
        response.put("orderId", bill.getOrder() != null ? bill.getOrder().getId() : null);
        response.put("totalAmount", bill.getTotalAmount());
        response.put("paymentMethod", bill.getPaymentMethod());
        response.put("paymentStatus", bill.getPaymentStatus());
        response.put("createdAt", bill.getCreatedAt());
        response.put("tableNumber", bill.getTable() != null ? bill.getTable().getNumber() : null);
        response.put("tableFee", bill.getOrder() != null ? bill.getOrder().getTableFee() : 0);
        response.put("productFee", bill.getOrder() != null ? bill.getOrder().getProductFee() : 0);

        if (bill.getOrder() != null && bill.getOrder().getItems() != null) {
            List<Map<String, Object>> items = bill.getOrder().getItems().stream().map(item -> {
                Map<String, Object> itemMap = new HashMap<>();
                itemMap.put("id", item.getId());
                itemMap.put("quantity", item.getQuantity());
                itemMap.put("unitPrice", item.getUnitPrice());
                itemMap.put("name", item.getProduct() != null ? item.getProduct().getName() : "Sản phẩm");
                itemMap.put("product", Map.of(
                        "id", item.getProduct().getId(),
                        "name", item.getProduct().getName(),
                        "price", item.getProduct().getPrice()));
                return itemMap;
            }).collect(Collectors.toList());
            response.put("items", items);
        } else {
            response.put("items", List.of());
        }

        return ResponseEntity.ok(Map.of("success", true, "data", response));
    }

    @GetMapping("/order/{orderId}")
    @PreAuthorize("hasAnyRole('STAFF','ADMIN')")
    public ResponseEntity<Bill> getBillByOrder(@PathVariable Long orderId) {
        return ResponseEntity.ok(billService.getBillByOrder(orderId));
    }

    @PatchMapping("/{billId}/pay-cash")
    @PreAuthorize("hasAnyRole('ADMIN','STAFF')")
    public ResponseEntity<?> payCash(@PathVariable Long billId) {
        billService.confirmCashPayment(billId);
        return ResponseEntity.ok().build();
    }

    @PatchMapping("/{billId}/confirm-momo")
    @PreAuthorize("hasAnyRole('ADMIN')")
    public ResponseEntity<?> confirmMomo(@PathVariable Long billId) {
        billService.confirmMomoPayment(billId);
        return ResponseEntity.ok().build();
    }
}