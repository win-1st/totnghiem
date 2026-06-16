package thang.bida.controllers;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import thang.bida.model.InventoryTransaction;
import thang.bida.model.Product;
import thang.bida.model.User;
import thang.bida.repository.InventoryTransactionRepository;
import thang.bida.repository.ProductRepository;
import thang.bida.repository.UserRepository;
import thang.bida.services.UserDetailsImpl;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/inventory")
@CrossOrigin(origins = "*")
@PreAuthorize("hasRole('ADMIN')")
public class InventoryController {

    private final InventoryTransactionRepository inventoryTransactionRepository;
    private final ProductRepository productRepository;
    private final UserRepository userRepository;

    public InventoryController(InventoryTransactionRepository inventoryTransactionRepository,
            ProductRepository productRepository,
            UserRepository userRepository) {
        this.inventoryTransactionRepository = inventoryTransactionRepository;
        this.productRepository = productRepository;
        this.userRepository = userRepository;
    }

    @GetMapping
    public ResponseEntity<?> getAll() {
        List<InventoryTransaction> list = inventoryTransactionRepository.findAll();
        return ResponseEntity.ok(Map.of("success", true, "data", list));
    }

    @GetMapping("/product/{productId}")
    public ResponseEntity<?> getByProduct(@PathVariable Long productId) {
        List<InventoryTransaction> list = inventoryTransactionRepository.findByProductId(productId);
        return ResponseEntity.ok(Map.of("success", true, "data", list));
    }

    @PostMapping("/import")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> importStock(@RequestBody Map<String, Object> request) {
        Long productId = Long.valueOf(request.get("productId").toString());
        Integer quantity = Integer.valueOf(request.get("quantity").toString());
        String note = (String) request.getOrDefault("note", "Nhập kho");

        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy sản phẩm"));

        // Lấy user hiện tại
        User currentUser = null;
        try {
            Object principal = SecurityContextHolder.getContext().getAuthentication().getPrincipal();
            if (principal instanceof UserDetailsImpl) {
                currentUser = userRepository.findById(((UserDetailsImpl) principal).getId()).orElse(null);
            }
        } catch (Exception e) {
            System.out.println("Không lấy được user: " + e.getMessage());
        }

        int beforeQty = product.getStockQuantity() != null ? product.getStockQuantity() : 0;
        product.setStockQuantity(beforeQty + quantity);
        productRepository.save(product);

        InventoryTransaction tx = new InventoryTransaction(
                product, currentUser, InventoryTransaction.TransactionType.IMPORT,
                quantity, beforeQty, beforeQty + quantity, note);
        inventoryTransactionRepository.save(tx);

        return ResponseEntity.ok(Map.of("success", true,
                "message", "Đã nhập " + quantity + " " + product.getName(),
                "data", tx));
    }
}