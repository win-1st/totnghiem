package thang.bida.services;

import thang.bida.model.*;
import thang.bida.model.Order.OrderStatus;
import thang.bida.repository.*;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Service
@Transactional
public class OrderService {

    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    private final BidaTableRepository tableRepository;
    private final ProductRepository productRepository;
    private final UserRepository userRepository;
    private final WebSocketService webSocketService;
    private final InventoryTransactionRepository inventoryTransactionRepository;

    public OrderService(
            OrderRepository orderRepository,
            OrderItemRepository orderItemRepository,
            BidaTableRepository tableRepository,
            ProductRepository productRepository,
            UserRepository userRepository,
            WebSocketService webSocketService,
            InventoryTransactionRepository inventoryTransactionRepository) {
        this.orderRepository = orderRepository;
        this.orderItemRepository = orderItemRepository;
        this.tableRepository = tableRepository;
        this.productRepository = productRepository;
        this.userRepository = userRepository;
        this.webSocketService = webSocketService;
        this.inventoryTransactionRepository = inventoryTransactionRepository;
    }

    // =====================================================
    // PRIVATE COMMON LOGIC
    // =====================================================

    // ✅ Sửa: dùng stockQuantity
    private void updateStockAfterPayment(Product product, int quantity) {
        if (product.isTimeBased()) {
            System.out.println("✅ TIME_BASED product '" + product.getName() + "' - skipping stock check");
            return;
        }

        // Sử dụng stockQuantity (field trong Product)
        if (product.getStockQuantity() < quantity) {
            throw new RuntimeException(
                    "Sản phẩm " + product.getName() + " không đủ số lượng. Còn: " + product.getStockQuantity());
        }

        product.decreaseStock(quantity); // Dùng method có sẵn trong Product
        productRepository.save(product);
        System.out.println("✅ Đã trừ stock: " + product.getName() + " - " + quantity);
    }

    private void recalcAndNotify(Order order) {
        updateOrderTotal(order.getId());
        webSocketService.notifyOrderUpdate(order);
    }

    private OrderItem getOrderItem(Long orderId, Long productId) {
        return orderItemRepository
                .findByOrderIdAndProductId(orderId, productId)
                .orElseThrow(() -> new RuntimeException("Order item not found"));
    }

    // =====================================================
    // BASIC QUERY
    // =====================================================

    public List<Order> getAllOrders() {
        return orderRepository.findAll();
    }

    public Order getOrderById(Long orderId) {
        return orderRepository.findByIdWithItems(orderId)
                .orElseThrow(() -> new RuntimeException("Order not found"));
    }

    public List<Order> getPendingOrders() {
        return orderRepository.findByStatus(Order.OrderStatus.OPEN);
    }

    public List<Order> getOrdersByTable(Long tableId) {
        return orderRepository.findByTableIdWithItems(tableId);
    }

    // =====================================================
    // OPEN ORDER
    // =====================================================

    public Order openOrder(Long tableId, Long employeeId) {
        BidaTable table = tableRepository.findById(tableId)
                .orElseThrow(() -> new RuntimeException("Table not found"));

        User employee = userRepository.findById(employeeId)
                .orElseThrow(() -> new RuntimeException("Employee not found"));

        if (table.getStatus() != BidaTable.TableStatus.FREE) {
            throw new RuntimeException("Bàn không trống");
        }

        Order order = new Order();
        order.setTable(table);
        order.setEmployee(employee);
        order.setStatus(OrderStatus.OPEN);
        order.setStartTime(null);
        order.setTotalAmount(BigDecimal.ZERO);
        order.setTableFee(BigDecimal.ZERO);
        order.setProductFee(BigDecimal.ZERO);
        order.setStockDeducted(false);

        Order saved = orderRepository.save(order);

        table.setStatus(BidaTable.TableStatus.OCCUPIED);
        tableRepository.save(table);

        webSocketService.notifyTableStatus(table.getId(), "OCCUPIED");
        webSocketService.notifyOrderUpdate(saved);

        return saved;
    }

    // =====================================================
    // ADD ITEM TO ORDER - KHÔNG trừ stock ở đây
    // =====================================================

    public Order addItemToOrder(Long orderId, Long productId, Integer quantity) {
        Order order = getOrderById(orderId);

        if (order.getStatus() != OrderStatus.OPEN) {
            throw new RuntimeException("Không thể thêm món khi đã kết thúc chơi");
        }

        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new RuntimeException("Product not found"));

        if (product.isTimeBased()) {
            if (order.getTimeBasedProduct() != null) {
                throw new RuntimeException("Bàn đã có dịch vụ tính giờ rồi!");
            }
            if (quantity > 1) {
                throw new RuntimeException("Chỉ được chọn 1 lần dịch vụ tính giờ!");
            }

            order.setTimeBasedProduct(product);

            if (order.getStartTime() == null) {
                order.setStartTime(LocalDateTime.now());
                System.out.println("⏱️ Bắt đầu tính giờ từ: " + order.getStartTime());
            }

            BigDecimal initialPrice = product.getPricePerMinute() != null ? product.getPricePerMinute()
                    : BigDecimal.ZERO;
            OrderItem timeItem = new OrderItem(order, product, 1, initialPrice);
            timeItem.setSubtotal(BigDecimal.ZERO);
            orderItemRepository.save(timeItem);
            order.getItems().add(timeItem);

            // ✅ KHÔNG gọi recalcAndNotify vì updateOrderTotal sẽ xử lý TIME_BASED
            updateOrderTotal(order.getId());
            webSocketService.notifyOrderUpdate(order);
            return order;
        }

        // FOOD/DRINK
        OrderItem item = orderItemRepository
                .findByOrderIdAndProductId(orderId, productId)
                .orElse(new OrderItem(order, product, 0, product.getPrice()));

        item.setQuantity(item.getQuantity() + quantity);
        BigDecimal subtotal = product.getPrice().multiply(BigDecimal.valueOf(item.getQuantity()));
        item.setSubtotal(subtotal);
        orderItemRepository.save(item);

        // Nếu item mới, thêm vào order
        if (!order.getItems().contains(item)) {
            order.getItems().add(item);
        }

        updateOrderTotal(order.getId());
        webSocketService.notifyOrderUpdate(order);
        return order;
    }

    public OrderItem addItem(Long orderId, Long productId, Integer quantity) {
        addItemToOrder(orderId, productId, quantity);
        return getOrderItem(orderId, productId);
    }

    // =====================================================
    // UPDATE ITEM QUANTITY - KHÔNG cập nhật stock
    // =====================================================

    public void updateItemQuantity(Long orderId, Long itemId, Integer quantity, BigDecimal unitPrice) {
        Order order = getOrderById(orderId);

        OrderItem item = orderItemRepository.findById(itemId)
                .orElseThrow(() -> new RuntimeException("Order item not found"));

        Product product = item.getProduct();

        if (product.isTimeBased()) {
            throw new RuntimeException("Không thể thay đổi số lượng của dịch vụ tính giờ!");
        }

        if (quantity <= 0) {
            // ❌ KHÔNG hoàn stock
            order.removeItem(item);
            orderItemRepository.delete(item);
        } else {
            item.setQuantity(quantity);
            // 🆕 Nếu có unitPrice thì dùng, không thì dùng giá từ product
            BigDecimal finalUnitPrice = (unitPrice != null && unitPrice.compareTo(BigDecimal.ZERO) > 0)
                    ? unitPrice
                    : product.getPrice();
            item.setUnitPrice(finalUnitPrice);
            item.setSubtotal(finalUnitPrice.multiply(BigDecimal.valueOf(quantity)));
            orderItemRepository.save(item);
        }

        updateOrderTotal(orderId);
        webSocketService.notifyOrderUpdate(order);
    }

    // =====================================================
    // REMOVE ITEM - KHÔNG hoàn stock
    // =====================================================

    public void removeItem(Long orderId, Long itemId) {
        Order order = getOrderById(orderId);

        OrderItem item = orderItemRepository.findById(itemId)
                .orElseThrow(() -> new RuntimeException("Item not found"));

        if (item.getProduct().isTimeBased()) {
            throw new RuntimeException("Không thể xóa dịch vụ tính giờ! Hãy kết thúc order để tính tiền.");
        }

        // ❌ KHÔNG hoàn stock
        order.removeItem(item);
        orderItemRepository.delete(item);

        updateOrderTotal(orderId);
        webSocketService.notifyOrderUpdate(order);
    }

    // =====================================================
    // UPDATE ORDER TOTAL
    // =====================================================

    public void updateOrderTotal(Long orderId) {
        Order order = getOrderById(orderId);

        BigDecimal itemsTotal = order.getItems().stream()
                .filter(item -> !item.getProduct().isTimeBased())
                .map(OrderItem::getSubtotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        System.out.println("=== UPDATE ORDER TOTAL ===");
        System.out.println("Order ID: " + orderId);
        System.out.println("Items total (food/drink): " + itemsTotal);

        BigDecimal tableFee = BigDecimal.ZERO;
        if (order.getTimeBasedProduct() != null && order.getStartTime() != null) {
            long minutes = order.getMinutesPlayed();
            BigDecimal pricePerMinute = order.getTimeBasedProduct().getPricePerMinute();
            tableFee = pricePerMinute.multiply(BigDecimal.valueOf(minutes));

            final BigDecimal finalTableFee = tableFee;
            order.getItems().stream()
                    .filter(item -> item.getProduct().isTimeBased())
                    .findFirst()
                    .ifPresent(timeItem -> {
                        timeItem.setSubtotal(finalTableFee);
                        timeItem.setPrice(finalTableFee);
                        timeItem.setUnitPrice(finalTableFee);
                        orderItemRepository.save(timeItem);
                    });
        }

        BigDecimal total = itemsTotal.add(tableFee);
        order.setTotalAmount(total);
        order.setTableFee(tableFee);
        order.setProductFee(itemsTotal);
        orderRepository.save(order);
    }

    // =====================================================
    // FINISH PLAYING
    // =====================================================

    public Order finishPlaying(Long orderId) {
        Order order = getOrderById(orderId);

        if (order.getStatus() != OrderStatus.OPEN) {
            throw new RuntimeException("Order không ở trạng thái đang chơi");
        }

        if (order.getStartTime() != null) {
            order.setEndTime(LocalDateTime.now());
            System.out.println("⏱️ Kết thúc tính giờ lúc: " + order.getEndTime());
        }

        updateOrderTotal(orderId);
        order.setStatus(OrderStatus.WAITING_PAYMENT);

        Order saved = orderRepository.save(order);
        webSocketService.notifyOrderUpdate(saved);

        return saved;
    }

    // =====================================================
    // DEDUCT STOCK - Gọi KHI THANH TOÁN
    // =====================================================

    @Transactional
    public void deductStockForOrder(Long orderId) {
        Order order = getOrderById(orderId);

        if (order.isStockDeducted()) {
            System.out.println("Stock already deducted for order " + orderId);
            return;
        }

        // 🆕 Lấy user hiện tại
        User currentUser = null;
        try {
            Object principal = SecurityContextHolder.getContext().getAuthentication().getPrincipal();
            if (principal instanceof UserDetailsImpl) {
                Long userId = ((UserDetailsImpl) principal).getId();
                currentUser = userRepository.findById(userId).orElse(null);
            }
        } catch (Exception e) {
            System.out.println("Không lấy được user: " + e.getMessage());
        }

        for (OrderItem item : order.getItems()) {
            Product product = item.getProduct();
            if (!product.isTimeBased() && item.getQuantity() > 0) {
                int quantity = item.getQuantity();
                int beforeQty = product.getStockQuantity();

                if (beforeQty < quantity) {
                    throw new RuntimeException(
                            "Sản phẩm " + product.getName() + " không đủ số lượng. Còn: " + beforeQty);
                }

                product.decreaseStock(quantity);
                productRepository.save(product);

                // 🆕 Ghi log kho
                InventoryTransaction tx = new InventoryTransaction(
                        product, currentUser,
                        InventoryTransaction.TransactionType.EXPORT,
                        quantity, beforeQty, beforeQty - quantity,
                        "Bán hàng - Order #" + orderId);
                inventoryTransactionRepository.save(tx);
                System.out.println("📦 Đã ghi log xuất kho: " + product.getName() + " -" + quantity);
            }
        }

        order.setStockDeducted(true);
        orderRepository.save(order);
        System.out.println("✅ Đã trừ stock + ghi log kho cho order " + orderId);
    }
    // =====================================================
    // REFUND STOCK - Gọi khi hủy đơn (nếu đã trừ stock)
    // =====================================================

    @Transactional
    public void refundStockForOrder(Long orderId) {
        Order order = getOrderById(orderId);

        if (!order.isStockDeducted()) {
            System.out.println("Stock not deducted yet for order " + orderId);
            return;
        }

        for (OrderItem item : order.getItems()) {
            Product product = item.getProduct();
            if (!product.isTimeBased() && item.getQuantity() > 0) {
                // Hoàn stock - sử dụng increaseStock()
                product.increaseStock(item.getQuantity());
                productRepository.save(product);
                System.out.println("✅ Đã hoàn stock: " + product.getName() + " x" + item.getQuantity());
            }
        }

        order.setStockDeducted(false);
        orderRepository.save(order);
        System.out.println("✅ Đã hoàn stock cho order " + orderId);
    }

    // =====================================================
    // CLOSE ORDER (PAYMENT) - TRỪ STOCK TẠI ĐÂY
    // =====================================================

    public void closeOrder(Long orderId) {
        Order order = getOrderById(orderId);

        if (order.getStatus() == OrderStatus.PAID) {
            System.out.println("Order " + orderId + " already paid");
            return;
        }

        if (order.getStatus() != OrderStatus.OPEN && order.getStatus() != OrderStatus.WAITING_PAYMENT) {
            throw new RuntimeException(
                    "Order không ở trạng thái đang chơi hoặc chờ thanh toán. Status hiện tại: " + order.getStatus());
        }

        // ✅ TRỪ STOCK TRƯỚC KHI THANH TOÁN
        deductStockForOrder(orderId);

        order.setStatus(OrderStatus.PAID);
        orderRepository.save(order);

        BidaTable table = order.getTable();
        table.setStatus(BidaTable.TableStatus.FREE);
        tableRepository.save(table);

        webSocketService.notifyTableStatus(table.getId(), "FREE");
        webSocketService.notifyOrderUpdate(order);
    }

    // =====================================================
    // CANCEL ORDER - HOÀN STOCK NẾU ĐÃ TRỪ
    // =====================================================

    public void cancelOrder(Long orderId) {
        Order order = getOrderById(orderId);

        if (order.getStatus() == OrderStatus.PAID) {
            throw new RuntimeException("Order đã thanh toán, không thể hủy");
        }

        // Nếu đã trừ stock thì hoàn lại
        if (order.isStockDeducted()) {
            refundStockForOrder(orderId);
        }

        order.setStatus(OrderStatus.CANCELLED);
        orderRepository.save(order);

        BidaTable table = order.getTable();
        table.setStatus(BidaTable.TableStatus.FREE);
        tableRepository.save(table);

        webSocketService.notifyTableStatus(table.getId(), "FREE");
    }

    // =====================================================
    // OTHER METHODS
    // =====================================================

    public Order updateOrderStatus(Long orderId, Order.OrderStatus status) {
        Order order = getOrderById(orderId);
        order.setStatus(status);
        Order saved = orderRepository.save(order);
        webSocketService.notifyOrderUpdate(saved);
        return saved;
    }

    public Order getOrderByTable(Long tableId) {
        List<Order> orders = orderRepository.findByTableIdWithItems(tableId);
        return orders.stream()
                .filter(o -> o.getStatus() == OrderStatus.OPEN ||
                        o.getStatus() == OrderStatus.WAITING_PAYMENT)
                .reduce((first, second) -> second)
                .orElse(null);
    }

    public OrderItem addItemWithPrice(Long orderId, Long productId, Integer quantity, BigDecimal unitPrice) {
        Order order = getOrderById(orderId);

        if (order.getStatus() != OrderStatus.OPEN) {
            throw new RuntimeException("Không thể thêm món khi đã kết thúc chơi");
        }

        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new RuntimeException("Product not found"));

        // 🆕 Xử lý TIME_BASED
        if (product.isTimeBased()) {
            if (order.getTimeBasedProduct() != null) {
                throw new RuntimeException("Bàn đã có dịch vụ tính giờ rồi!");
            }
            if (quantity > 1) {
                throw new RuntimeException("Chỉ được chọn 1 lần dịch vụ tính giờ!");
            }

            order.setTimeBasedProduct(product);
            if (order.getStartTime() == null) {
                order.setStartTime(LocalDateTime.now());
            }

            BigDecimal initialPrice = product.getPricePerMinute() != null ? product.getPricePerMinute()
                    : BigDecimal.ZERO;
            OrderItem timeItem = new OrderItem(order, product, 1, initialPrice);
            timeItem.setSubtotal(BigDecimal.ZERO);
            orderItemRepository.save(timeItem);
            order.getItems().add(timeItem);

            updateOrderTotal(orderId);
            webSocketService.notifyOrderUpdate(order);
            return timeItem;
        }

        // FOOD/DRINK - giữ nguyên logic cũ
        OrderItem item = orderItemRepository
                .findByOrderIdAndProductId(orderId, productId)
                .orElse(new OrderItem(order, product, 0, unitPrice));

        item.setQuantity(item.getQuantity() + quantity);
        BigDecimal subtotal = unitPrice.multiply(BigDecimal.valueOf(item.getQuantity()));
        item.setSubtotal(subtotal);
        item.setUnitPrice(unitPrice);
        orderItemRepository.save(item);

        if (!order.getItems().contains(item)) {
            order.getItems().add(item);
        }

        updateOrderTotal(orderId);
        webSocketService.notifyOrderUpdate(order);
        return item;
    }

    @Transactional
    public void adjustPlayTime(Long orderId, Integer additionalMinutes) {
        Order order = getOrderById(orderId);

        if (order.getStatus() != OrderStatus.OPEN) {
            throw new RuntimeException("Chỉ có thể điều chỉnh thời gian khi đơn hàng đang mở");
        }

        if (order.getTimeBasedProduct() == null) {
            throw new RuntimeException("Đơn hàng chưa có dịch vụ tính giờ");
        }

        if (order.getStartTime() == null) {
            throw new RuntimeException("Chưa có thời gian bắt đầu");
        }

        // Lưu lại thời gian cũ để log
        LocalDateTime oldStartTime = order.getStartTime();

        // Điều chỉnh thời gian bắt đầu (cộng thêm phút)
        // additionalMinutes có thể âm (giảm thời gian) hoặc dương (tăng thời gian)
        LocalDateTime newStartTime = oldStartTime.minusMinutes(additionalMinutes);
        order.setStartTime(newStartTime);

        // Log chi tiết
        System.out.println("⏱️ Điều chỉnh thời gian cho order " + orderId);
        System.out.println("   - Thời gian cũ: " + oldStartTime);
        System.out.println("   - Thay đổi: " + additionalMinutes + " phút");
        System.out.println("   - Thời gian mới: " + newStartTime);

        // Lưu order
        orderRepository.save(order);

        // Cập nhật lại tổng tiền
        updateOrderTotal(orderId);

        // Notify qua WebSocket
        webSocketService.notifyOrderUpdate(order);

        System.out.println("✅ Đã điều chỉnh thời gian thành công!");
    }
}