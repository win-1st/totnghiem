package thang.bida.controllers;

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

    // Tạo hóa đơn
    @PostMapping("/create")
    @PreAuthorize("hasAnyRole('MANAGER','ADMIN','STAFF')")
    public ResponseEntity<Bill> createBill(
            @RequestParam Long orderId,
            @RequestParam PaymentMethod method) {
        return ResponseEntity.ok(
                billService.createBill(orderId, method));
    }

    // Xem hóa đơn theo order
    @GetMapping("/order/{orderId}")
    @PreAuthorize("hasAnyRole('STAFF','MANAGER','ADMIN')")
    public ResponseEntity<Bill> getBillByOrder(@PathVariable Long orderId) {
        return ResponseEntity.ok(billService.getBillByOrder(orderId));
    }

    // Thanh toán tiền mặt
    @PatchMapping("/{billId}/pay-cash")
    @PreAuthorize("hasAnyRole('MANAGER','ADMIN','STAFF')")
    public ResponseEntity<?> payCash(@PathVariable Long billId) {
        billService.confirmCashPayment(billId);
        return ResponseEntity.ok().build();
    }

    // Xác nhận MoMo thủ công
    @PatchMapping("/{billId}/confirm-momo")
    @PreAuthorize("hasAnyRole('MANAGER','ADMIN')")
    public ResponseEntity<?> confirmMomo(@PathVariable Long billId) {
        billService.confirmMomoPayment(billId);
        return ResponseEntity.ok().build();
    }
}
