package thang.bida.services;

import thang.bida.dto.CustomerPointDTO;
import thang.bida.dto.TransactionDTO;
import thang.bida.model.CustomerPoint;
import thang.bida.model.Transaction;
import thang.bida.model.User;
import thang.bida.model.Product;
import thang.bida.repository.CustomerPointRepository;
import thang.bida.repository.TransactionRepository;
import thang.bida.repository.UserRepository;
import thang.bida.repository.ProductRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import lombok.extern.slf4j.Slf4j;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@Slf4j
public class CustomerPointService {

    @Autowired
    private CustomerPointRepository customerPointRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private TransactionRepository transactionRepository;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private InventoryService inventoryService; // ✅ SỬA: Dùng InventoryService

    // ========== CONVERT METHODS ==========

    private CustomerPointDTO convertToDTO(CustomerPoint entity) {
        if (entity == null)
            return null;

        CustomerPointDTO dto = new CustomerPointDTO();
        dto.setId(entity.getId());
        dto.setPhone(entity.getPhone());
        dto.setCustomerName(entity.getCustomerName());
        dto.setTotalPoints(entity.getTotalPoints());
        dto.setCreatedAt(entity.getCreatedAt() != null ? entity.getCreatedAt().toString() : null);
        dto.setUpdatedAt(entity.getUpdatedAt() != null ? entity.getUpdatedAt().toString() : null);
        return dto;
    }

    private TransactionDTO convertToTransactionDTO(Transaction entity) {
        if (entity == null)
            return null;

        TransactionDTO dto = new TransactionDTO();
        dto.setId(entity.getId());
        dto.setPoints(entity.getPoints());
        dto.setType(entity.getType());
        dto.setDescription(entity.getDescription());
        dto.setCreatedAt(entity.getCreatedAt());
        return dto;
    }

    // ========== API CHO CUSTOMER ==========

    public CustomerPointDTO getCustomerByPhone(String phone) {
        return customerPointRepository.findByPhone(phone)
                .map(this::convertToDTO)
                .orElse(null);
    }

    @Transactional
    public CustomerPointDTO createCustomerForUser(String phone) {
        var existing = customerPointRepository.findByPhone(phone);
        if (existing.isPresent()) {
            return convertToDTO(existing.get());
        }

        User user = userRepository.findByPhone(phone).orElse(null);

        CustomerPoint customerPoint = new CustomerPoint();
        customerPoint.setPhone(phone);
        customerPoint.setCustomerName(user != null ? user.getFullName() : "");
        customerPoint.setTotalPoints(0);
        customerPoint.setCreatedAt(LocalDateTime.now());
        customerPoint.setUpdatedAt(LocalDateTime.now());

        customerPoint = customerPointRepository.save(customerPoint);
        return convertToDTO(customerPoint);
    }

    public List<TransactionDTO> getTransactionsByPhone(String phone) {
        CustomerPoint customerPoint = customerPointRepository.findByPhone(phone)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy điểm của khách hàng với SĐT: " + phone));

        return transactionRepository.findByCustomerPointIdOrderByCreatedAtDesc(customerPoint.getId())
                .stream()
                .map(this::convertToTransactionDTO)
                .collect(Collectors.toList());
    }

    // ========== API CHO ADMIN/STAFF ==========

    public List<CustomerPointDTO> getAllCustomers() {
        return customerPointRepository.findAll()
                .stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    public CustomerPointDTO getCustomerById(Long id) {
        CustomerPoint customer = customerPointRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy khách hàng với ID: " + id));
        return convertToDTO(customer);
    }

    @Transactional
    public CustomerPointDTO createCustomer(CustomerPointDTO dto) {
        if (customerPointRepository.existsByPhone(dto.getPhone())) {
            throw new RuntimeException("Số điện thoại " + dto.getPhone() + " đã tồn tại!");
        }

        CustomerPoint customer = new CustomerPoint();
        customer.setPhone(dto.getPhone());
        customer.setCustomerName(dto.getCustomerName());
        customer.setTotalPoints(dto.getTotalPoints() != null ? dto.getTotalPoints() : 0);
        customer.setCreatedAt(LocalDateTime.now());
        customer.setUpdatedAt(LocalDateTime.now());

        CustomerPoint saved = customerPointRepository.save(customer);
        return convertToDTO(saved);
    }

    @Transactional
    public CustomerPointDTO updateCustomer(Long id, CustomerPointDTO dto) {
        CustomerPoint existing = customerPointRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy khách hàng với ID: " + id));

        if (!existing.getPhone().equals(dto.getPhone()) &&
                customerPointRepository.existsByPhone(dto.getPhone())) {
            throw new RuntimeException("Số điện thoại " + dto.getPhone() + " đã thuộc về khách hàng khác!");
        }

        existing.setPhone(dto.getPhone());
        existing.setCustomerName(dto.getCustomerName());
        existing.setTotalPoints(dto.getTotalPoints());
        existing.setUpdatedAt(LocalDateTime.now());

        CustomerPoint updated = customerPointRepository.save(existing);
        return convertToDTO(updated);
    }

    @Transactional
    public void deleteCustomer(Long id) {
        CustomerPoint existing = customerPointRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy khách hàng với ID: " + id));
        customerPointRepository.delete(existing);
    }

    @Transactional
    public CustomerPointDTO addPoints(String phone, int points) {
        int updated = customerPointRepository.addPoints(phone, points);
        if (updated == 0) {
            throw new RuntimeException("Không tìm thấy khách hàng với SĐT: " + phone);
        }

        CustomerPoint customer = customerPointRepository.findByPhone(phone)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy khách hàng"));

        Transaction transaction = new Transaction();
        transaction.setCustomerPoint(customer);
        transaction.setPoints(points);
        transaction.setType("EARN");
        transaction.setDescription("Cộng điểm thưởng");
        transaction.setCreatedAt(LocalDateTime.now());
        transactionRepository.save(transaction);

        return getCustomerByPhone(phone);
    }

    @Transactional
    public CustomerPointDTO redeemPoints(String phone, int points) {
        int updated = customerPointRepository.redeemPoints(phone, points);
        if (updated == 0) {
            throw new RuntimeException("Không tìm thấy khách hàng hoặc không đủ điểm!");
        }

        CustomerPoint customer = customerPointRepository.findByPhone(phone)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy khách hàng"));

        Transaction transaction = new Transaction();
        transaction.setCustomerPoint(customer);
        transaction.setPoints(points);
        transaction.setType("REDEEM");
        transaction.setDescription("Đổi điểm thưởng");
        transaction.setCreatedAt(LocalDateTime.now());
        transactionRepository.save(transaction);

        return getCustomerByPhone(phone);
    }

    // =====================================================
    // ========== ĐỔI SẢN PHẨM BẰNG ĐIỂM ==========
    // =====================================================

    /**
     * Lấy danh sách sản phẩm có thể đổi bằng điểm
     */
    public Map<String, Object> getRedeemableProducts(String phone) {
        CustomerPoint customer = customerPointRepository.findByPhone(phone)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy khách hàng với SĐT: " + phone));

        // ✅ Lấy danh sách sản phẩm với JOIN FETCH để fix N+1
        List<Product> redeemableProducts = productRepository.findByIsRedeemableTrueAndActiveTrue();

        Map<String, Object> result = new HashMap<>();
        result.put("customerName", customer.getCustomerName());
        result.put("phone", customer.getPhone());
        result.put("totalPoints", customer.getTotalPoints());

        List<Map<String, Object>> products = redeemableProducts.stream()
                .map(product -> {
                    Map<String, Object> item = new HashMap<>();
                    item.put("id", product.getId());
                    item.put("name", product.getName());
                    item.put("imageUrl", product.getImageUrl());
                    item.put("description", product.getDescription());
                    item.put("pointsRequired", product.getPointsRequired());
                    item.put("price", product.getPrice());
                    item.put("salePrice", product.getSalePrice());
                    item.put("unit", product.getUnit());
                    item.put("stockQuantity", product.getStockQuantity());
                    // ✅ Không gọi category ở đây để tránh N+1

                    int maxByPoints = product.getPointsRequired() != null && product.getPointsRequired() > 0
                            ? customer.getTotalPoints() / product.getPointsRequired()
                            : 0;
                    int maxByStock = product.getStockQuantity() != null ? product.getStockQuantity() : 0;
                    item.put("maxQuantity", Math.min(maxByPoints, maxByStock));

                    return item;
                })
                .collect(Collectors.toList());

        result.put("availableProducts", products);
        return result;
    }

    /**
     * Đổi sản phẩm bằng điểm - SỬA: Thêm rollbackFor và dùng InventoryService
     */
    @Transactional(rollbackFor = Exception.class) // ✅ Thêm rollbackFor
    public Map<String, Object> redeemProduct(String phone, Long productId, Integer quantity) {
        log.info("=== REDEEM PRODUCT START ===");
        log.info("Phone: {}, ProductId: {}, Quantity: {}", phone, productId, quantity);

        // 1. Validate
        if (quantity == null || quantity <= 0) {
            throw new RuntimeException("Số lượng phải lớn hơn 0");
        }

        // 2. Kiểm tra khách hàng
        CustomerPoint customer = customerPointRepository.findByPhone(phone)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy khách hàng với SĐT: " + phone));

        // 3. Kiểm tra sản phẩm
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy sản phẩm"));

        // 4. Kiểm tra sản phẩm có cho phép đổi điểm không
        if (product.getIsRedeemable() == null || !product.getIsRedeemable()) {
            throw new RuntimeException("Sản phẩm '" + product.getName() + "' không hỗ trợ đổi điểm");
        }

        if (product.getPointsRequired() == null || product.getPointsRequired() <= 0) {
            throw new RuntimeException("Sản phẩm '" + product.getName() + "' chưa cấu hình điểm đổi");
        }

        // 5. Tính tổng điểm cần đổi
        int totalPointsNeeded = product.getPointsRequired() * quantity;

        // 6. Kiểm tra số dư điểm
        if (customer.getTotalPoints() < totalPointsNeeded) {
            throw new RuntimeException(
                    "Không đủ điểm! Cần: " + totalPointsNeeded + ", Có: " + customer.getTotalPoints());
        }

        // 7. Kiểm tra tồn kho
        if (!product.isTimeBased()) {
            if (product.getStockQuantity() == null || product.getStockQuantity() < quantity) {
                throw new RuntimeException(
                        "Sản phẩm '" + product.getName() + "' không đủ số lượng. Còn: " + product.getStockQuantity());
            }
        }

        try {
            // 8. Trừ điểm khách hàng
            customer.redeemPoints(totalPointsNeeded);
            customerPointRepository.save(customer);
            log.info("✅ Deducted {} points from customer {}", totalPointsNeeded, phone);

            // 9. Trừ stock - ✅ Dùng InventoryService
            if (!product.isTimeBased()) {
                inventoryService.exportStock(
                        null, // orderId
                        product,
                        quantity,
                        "Đổi điểm - KH: " + customer.getCustomerName() + " (" + phone + ")");
                log.info("✅ Exported stock: {} x {}", product.getName(), quantity);
            }

            // 10. Ghi log giao dịch điểm
            Transaction transaction = new Transaction();
            transaction.setCustomerPoint(customer);
            transaction.setPoints(totalPointsNeeded);
            transaction.setType("REDEEM");
            transaction.setDescription("Đổi " + quantity + " " + product.getUnit() + " " + product.getName());
            transaction.setCreatedAt(LocalDateTime.now());
            transactionRepository.save(transaction);
            log.info("✅ Created transaction for customer {}", phone);

        } catch (Exception e) {
            log.error("Error redeeming product: {}", e.getMessage());
            // ✅ Nếu có lỗi, tất cả sẽ rollback
            throw new RuntimeException("Đổi sản phẩm thất bại: " + e.getMessage(), e);
        }

        // 11. Trả về kết quả
        Map<String, Object> result = new HashMap<>();
        result.put("success", true);
        result.put("message", "Đổi sản phẩm thành công!");
        result.put("productId", product.getId());
        result.put("productName", product.getName());
        result.put("quantity", quantity);
        result.put("pointsUsed", totalPointsNeeded);
        result.put("remainingPoints", customer.getTotalPoints());
        result.put("stockRemaining", product.getStockQuantity());

        log.info("=== REDEEM PRODUCT SUCCESS ===");
        return result;
    }

    /**
     * Lấy lịch sử đổi sản phẩm
     */
    public List<Map<String, Object>> getRedeemHistory(String phone) {
        CustomerPoint customer = customerPointRepository.findByPhone(phone)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy khách hàng với SĐT: " + phone));

        List<Transaction> redeemTransactions = transactionRepository
                .findByCustomerPointIdAndTypeOrderByCreatedAtDesc(customer.getId(), "REDEEM");

        return redeemTransactions.stream()
                .map(tx -> {
                    Map<String, Object> item = new HashMap<>();
                    item.put("id", tx.getId());
                    item.put("points", tx.getPoints());
                    item.put("description", tx.getDescription());
                    item.put("createdAt", tx.getCreatedAt());
                    return item;
                })
                .collect(Collectors.toList());
    }
}