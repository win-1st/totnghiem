package thang.bida.controllers;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import thang.bida.dto.InventoryTransactionDTO;
import thang.bida.model.Product;
import thang.bida.repository.ProductRepository;
import thang.bida.services.InventoryService;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/inventory")
@CrossOrigin(origins = "*")
@PreAuthorize("hasRole('ADMIN')")
public class InventoryController {

    private final InventoryService inventoryService;
    private final ProductRepository productRepository;

    public InventoryController(InventoryService inventoryService, ProductRepository productRepository) {
        this.inventoryService = inventoryService;
        this.productRepository = productRepository;
    }

    // ✅ Lấy tất cả transactions
    @GetMapping
    public ResponseEntity<?> getAll() {
        List<InventoryTransactionDTO> transactions = inventoryService.getAllTransactions();

        return ResponseEntity.ok(Map.of(
                "success", true,
                "data", transactions,
                "count", transactions.size()));
    }

    // ✅ Lấy transactions theo product
    @GetMapping("/product/{productId}")
    public ResponseEntity<?> getByProduct(@PathVariable Long productId) {
        List<InventoryTransactionDTO> transactions = inventoryService
                .getTransactionsByProduct(productId);

        return ResponseEntity.ok(Map.of(
                "success", true,
                "data", transactions,
                "count", transactions.size()));
    }

    // ✅ Nhập kho
    @PostMapping("/import")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> importStock(@RequestBody Map<String, Object> request) {
        try {
            Long productId = Long.valueOf(request.get("productId").toString());
            Integer quantity = Integer.valueOf(request.get("quantity").toString());
            String note = (String) request.getOrDefault("note", "Nhập kho");

            InventoryTransactionDTO tx = inventoryService.importStock(productId, quantity, note);

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "Đã nhập " + quantity + " sản phẩm thành công",
                    "data", tx));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", e.getMessage()));
        }
    }

    // ✅ Xuất kho
    @PostMapping("/export")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> exportStock(@RequestBody Map<String, Object> request) {
        try {
            Long productId = Long.valueOf(request.get("productId").toString());
            Integer quantity = Integer.valueOf(request.get("quantity").toString());
            String note = (String) request.getOrDefault("note", "Xuất kho");

            Product product = productRepository.findById(productId)
                    .orElseThrow(() -> new RuntimeException("Không tìm thấy sản phẩm"));

            InventoryTransactionDTO tx = inventoryService.exportStock(null, product, quantity, note);

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "Đã xuất " + quantity + " sản phẩm thành công",
                    "data", tx));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", e.getMessage()));
        }
    }

    // ✅ Điều chỉnh tồn kho
    @PostMapping("/adjust")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> adjustStock(@RequestBody Map<String, Object> request) {
        try {
            Long productId = Long.valueOf(request.get("productId").toString());
            Integer quantity = Integer.valueOf(request.get("quantity").toString());
            String note = (String) request.getOrDefault("note", "Điều chỉnh tồn kho");

            InventoryTransactionDTO tx = inventoryService.adjustStock(productId, quantity, note);

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "Đã điều chỉnh tồn kho thành công",
                    "data", tx));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", e.getMessage()));
        }
    }

    // ✅ Lấy transactions theo user
    @GetMapping("/user/{userId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> getByUser(@PathVariable Long userId) {
        List<InventoryTransactionDTO> transactions = inventoryService
                .getTransactionsByUser(userId);

        return ResponseEntity.ok(Map.of(
                "success", true,
                "data", transactions,
                "count", transactions.size()));
    }

    // ✅ Lấy transactions theo khoảng thời gian
    @GetMapping("/date-range")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> getByDateRange(
            @RequestParam String startDate,
            @RequestParam String endDate) {
        try {
            LocalDateTime start = LocalDateTime.parse(startDate);
            LocalDateTime end = LocalDateTime.parse(endDate);

            List<InventoryTransactionDTO> transactions = inventoryService
                    .getTransactionsByDateRange(start, end);

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "data", transactions,
                    "count", transactions.size()));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", "Định dạng ngày không hợp lệ. Dùng ISO-8601: yyyy-MM-ddTHH:mm:ss"));
        }
    }

    // ✅ Thống kê tồn kho - Gọi Service
    @GetMapping("/stats/product/{productId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> getProductStats(@PathVariable Long productId) {
        try {
            Map<String, Object> stats = inventoryService.getProductStats(productId);

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "data", stats));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", e.getMessage()));
        }
    }
}