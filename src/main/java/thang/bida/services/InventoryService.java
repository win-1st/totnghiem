package thang.bida.services;

import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import thang.bida.dto.InventoryTransactionDTO;
import thang.bida.model.InventoryTransaction;
import thang.bida.model.Product;
import thang.bida.model.User;
import thang.bida.repository.InventoryTransactionRepository;
import thang.bida.repository.ProductRepository;
import thang.bida.repository.UserRepository;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.stream.Collectors;
import java.util.Map;

@Service
@Transactional
@Slf4j
public class InventoryService {

    private final InventoryTransactionRepository inventoryTransactionRepository;
    private final ProductRepository productRepository;
    private final UserRepository userRepository;

    public InventoryService(
            InventoryTransactionRepository inventoryTransactionRepository,
            ProductRepository productRepository,
            UserRepository userRepository) {
        this.inventoryTransactionRepository = inventoryTransactionRepository;
        this.productRepository = productRepository;
        this.userRepository = userRepository;
    }

    // ========== QUERY METHODS (READ ONLY) ==========

    @Transactional(readOnly = true)
    public List<InventoryTransactionDTO> getTransactionsByProduct(Long productId) {
        List<InventoryTransaction> transactions = inventoryTransactionRepository
                .findByProductIdWithDetails(productId);

        return transactions.stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<InventoryTransactionDTO> getAllTransactions() {
        List<InventoryTransaction> transactions = inventoryTransactionRepository
                .findAllWithDetails();

        return transactions.stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<InventoryTransactionDTO> getTransactionsByUser(Long userId) {
        List<InventoryTransaction> transactions = inventoryTransactionRepository
                .findByUserIdWithDetails(userId);

        return transactions.stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<InventoryTransactionDTO> getTransactionsByDateRange(
            LocalDateTime startDate, LocalDateTime endDate) {
        List<InventoryTransaction> transactions = inventoryTransactionRepository
                .findByDateRangeWithDetails(startDate, endDate);

        return transactions.stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    // ========== TRANSACTION METHODS (WRITE) ==========

    /**
     * Nhập kho - Transaction riêng biệt
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW, rollbackFor = Exception.class)
    public InventoryTransactionDTO importStock(Long productId, Integer quantity, String note) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy sản phẩm"));

        if (product.isTimeBased()) {
            throw new RuntimeException("Không thể nhập kho cho sản phẩm tính giờ!");
        }

        User currentUser = getCurrentUser();
        int beforeQty = product.getStockQuantity() != null ? product.getStockQuantity() : 0;
        int afterQty = beforeQty + quantity;

        // 1. Cập nhật stock
        product.setStockQuantity(afterQty);
        productRepository.save(product);
        log.info("✅ Imported {} for product {}: {} -> {}", quantity, product.getName(), beforeQty, afterQty);

        // 2. Tạo inventory transaction
        InventoryTransaction tx = new InventoryTransaction(
                product,
                currentUser,
                InventoryTransaction.TransactionType.IMPORT,
                quantity,
                beforeQty,
                afterQty,
                note != null ? note : "Nhập kho");

        InventoryTransaction saved = inventoryTransactionRepository.save(tx);
        log.info("✅ Created inventory transaction: {}", saved.getId());

        return convertToDTO(saved);
    }

    /**
     * Xuất kho - Transaction riêng biệt
     * Dùng cho bán hàng, đổi điểm
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW, rollbackFor = Exception.class)
    public InventoryTransactionDTO exportStock(Long orderId, Product product, Integer quantity, String note) {
        if (product.isTimeBased()) {
            throw new RuntimeException("Không thể xuất kho cho sản phẩm tính giờ!");
        }

        User currentUser = getCurrentUser();
        int beforeQty = product.getStockQuantity() != null ? product.getStockQuantity() : 0;

        if (beforeQty < quantity) {
            throw new RuntimeException(
                    "Sản phẩm " + product.getName() + " không đủ số lượng. Còn: " + beforeQty);
        }

        int afterQty = beforeQty - quantity;

        // 1. Cập nhật stock
        product.setStockQuantity(afterQty);
        productRepository.save(product);
        log.info("✅ Exported {} for product {}: {} -> {}", quantity, product.getName(), beforeQty, afterQty);

        // 2. Tạo inventory transaction
        String defaultNote = orderId != null ? "Bán hàng - Order #" + orderId : "Xuất kho";
        InventoryTransaction tx = new InventoryTransaction(
                product,
                currentUser,
                InventoryTransaction.TransactionType.EXPORT,
                quantity,
                beforeQty,
                afterQty,
                note != null ? note : defaultNote);

        InventoryTransaction saved = inventoryTransactionRepository.save(tx);
        log.info("✅ Created export transaction: {}", saved.getId());

        return convertToDTO(saved);
    }

    /**
     * Điều chỉnh tồn kho
     * ✅ SỬA: Trả về InventoryTransactionDTO
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW, rollbackFor = Exception.class)
    public InventoryTransactionDTO adjustStock(Long productId, Integer quantity, String note) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy sản phẩm"));

        if (product.isTimeBased()) {
            throw new RuntimeException("Không thể điều chỉnh tồn kho cho sản phẩm tính giờ!");
        }

        User currentUser = getCurrentUser();
        int beforeQty = product.getStockQuantity() != null ? product.getStockQuantity() : 0;
        int afterQty = beforeQty + quantity;

        // Kiểm tra tồn kho không âm
        if (afterQty < 0) {
            throw new RuntimeException("Tồn kho không thể âm! Hiện tại: " + beforeQty + ", Điều chỉnh: " + quantity);
        }

        // 1. Cập nhật stock
        product.setStockQuantity(afterQty);
        productRepository.save(product);
        log.info("✅ Adjusted stock for product {}: {} -> {}", product.getName(), beforeQty, afterQty);

        // 2. Tạo inventory transaction
        InventoryTransaction tx = new InventoryTransaction(
                product,
                currentUser,
                InventoryTransaction.TransactionType.ADJUSTMENT,
                Math.abs(quantity),
                beforeQty,
                afterQty,
                note != null ? note : "Điều chỉnh tồn kho");

        InventoryTransaction saved = inventoryTransactionRepository.save(tx);
        log.info("✅ Created adjustment transaction: {}", saved.getId());

        // ✅ Trả về DTO
        return convertToDTO(saved);
    }

    // ========== PRIVATE METHODS ==========

    private User getCurrentUser() {
        try {
            Object principal = SecurityContextHolder.getContext().getAuthentication().getPrincipal();
            if (principal instanceof UserDetailsImpl) {
                Long userId = ((UserDetailsImpl) principal).getId();
                return userRepository.findById(userId).orElse(null);
            }
        } catch (Exception e) {
            log.warn("Cannot get current user: {}", e.getMessage());
        }
        return null;
    }

    private InventoryTransactionDTO convertToDTO(InventoryTransaction tx) {
        InventoryTransactionDTO dto = new InventoryTransactionDTO();
        dto.setId(tx.getId());
        dto.setProductId(tx.getProduct().getId());
        dto.setProductName(tx.getProduct().getName());
        dto.setProductCategory(tx.getProduct().getCategory() != null
                ? tx.getProduct().getCategory().getName()
                : null);
        dto.setTransactionType(tx.getTransactionType().name());
        dto.setQuantity(tx.getQuantity());
        dto.setBeforeQuantity(tx.getBeforeQuantity());
        dto.setAfterQuantity(tx.getAfterQuantity());
        dto.setNote(tx.getNote());
        dto.setCreatedAt(tx.getCreatedAt());

        if (tx.getUser() != null) {
            dto.setUserId(tx.getUser().getId());
            dto.setUserFullName(tx.getUser().getFullName());
        }

        return dto;
    }

    public Map<String, Object> getProductStats(Long productId) {
        Integer totalImported = inventoryTransactionRepository
                .sumQuantityByProductIdAndType(productId, InventoryTransaction.TransactionType.IMPORT);

        Integer totalExported = inventoryTransactionRepository
                .sumQuantityByProductIdAndType(productId, InventoryTransaction.TransactionType.EXPORT);

        Integer currentStock = inventoryTransactionRepository
                .getCurrentStockByProductId(productId);

        if (currentStock == null) {
            currentStock = 0;
        }

        Product product = productRepository.findById(productId).orElse(null);

        Map<String, Object> stats = new HashMap<>();
        stats.put("productId", productId);
        stats.put("productName", product != null ? product.getName() : "Không xác định");
        stats.put("totalImported", totalImported != null ? totalImported : 0);
        stats.put("totalExported", totalExported != null ? totalExported : 0);
        stats.put("currentStock", currentStock);

        return stats;
    }
}