package thang.bida.services;

import thang.bida.model.*;
import thang.bida.model.Order.OrderStatus;
import thang.bida.repository.*;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import lombok.extern.slf4j.Slf4j;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Service
@Transactional
@Slf4j
public class OrderService {

    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    private final BidaTableRepository tableRepository;
    private final ProductRepository productRepository;
    private final UserRepository userRepository;
    private final WebSocketService webSocketService;
    private final InventoryService inventoryService;

    public OrderService(
            OrderRepository orderRepository,
            OrderItemRepository orderItemRepository,
            BidaTableRepository tableRepository,
            ProductRepository productRepository,
            UserRepository userRepository,
            WebSocketService webSocketService,
            InventoryService inventoryService) {
        this.orderRepository = orderRepository;
        this.orderItemRepository = orderItemRepository;
        this.tableRepository = tableRepository;
        this.productRepository = productRepository;
        this.userRepository = userRepository;
        this.webSocketService = webSocketService;
        this.inventoryService = inventoryService;
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
    // CHECK ORDER STATUS (🆕 THÊM CÁC METHOD NÀY)
    // =====================================================

    /**
     * Kiểm tra xem bàn có đang có order active không (OPEN hoặc WAITING_PAYMENT)
     */
    public boolean hasActiveOrder(Long tableId) {
        List<Order> orders = orderRepository.findByTableIdWithItems(tableId);
        return orders.stream()
                .anyMatch(o -> o.getStatus() == OrderStatus.OPEN ||
                        o.getStatus() == OrderStatus.WAITING_PAYMENT);
    }

    /**
     * Lấy order active của bàn (OPEN hoặc WAITING_PAYMENT)
     */
    public Order getActiveOrder(Long tableId) {
        List<Order> orders = orderRepository.findByTableIdWithItems(tableId);
        return orders.stream()
                .filter(o -> o.getStatus() == OrderStatus.OPEN ||
                        o.getStatus() == OrderStatus.WAITING_PAYMENT)
                .findFirst()
                .orElse(null);
    }

    /**
     * Kiểm tra bàn có đang OCCUPIED không
     */
    public boolean isTableOccupied(Long tableId) {
        BidaTable table = tableRepository.findById(tableId).orElse(null);
        if (table == null)
            return false;
        return table.getStatus() == BidaTable.TableStatus.OCCUPIED;
    }

    /**
     * Kiểm tra bàn có đang FREE không
     */
    public boolean isTableFree(Long tableId) {
        BidaTable table = tableRepository.findById(tableId).orElse(null);
        if (table == null)
            return false;
        return table.getStatus() == BidaTable.TableStatus.FREE;
    }

    // =====================================================
    // OPEN ORDER
    // =====================================================

    @Transactional(rollbackFor = Exception.class)
    public Order openOrder(Long tableId, Long employeeId) {
        System.out.println("=== OPEN ORDER ===");
        System.out.println("Table ID: " + tableId);
        System.out.println("Employee ID: " + employeeId);

        BidaTable table = tableRepository.findById(tableId)
                .orElseThrow(() -> new RuntimeException("Table not found"));

        System.out.println("Table status: " + table.getStatus());
        System.out.println("Table number: " + table.getNumber());

        User employee = userRepository.findById(employeeId)
                .orElseThrow(() -> new RuntimeException("Employee not found"));

        // ✅ Kiểm tra: Nếu bàn OCCUPIED nhưng không có order active, vẫn cho mở
        // Lý do: Bàn có thể đã OCCUPIED từ check-in nhưng chưa tạo order
        if (table.getStatus() == BidaTable.TableStatus.OCCUPIED) {
            // Kiểm tra xem đã có order active chưa
            boolean hasActiveOrder = hasActiveOrder(tableId);
            if (hasActiveOrder) {
                System.out.println("❌ Table is OCCUPIED and has active order");
                throw new RuntimeException("Bàn đã có khách và đã có order!");
            } else {
                System.out.println("⚠️ Table is OCCUPIED but no active order - allowing order creation");
                // Vẫn cho phép tạo order mới
            }
        } else if (table.getStatus() != BidaTable.TableStatus.FREE &&
                table.getStatus() != BidaTable.TableStatus.RESERVED) {
            System.out.println("❌ Table status is " + table.getStatus() + ", cannot open order");
            throw new RuntimeException("Không thể mở bàn ở trạng thái: " + table.getStatus());
        }

        // Tạo order mới
        Order order = new Order();
        order.setTable(table);
        order.setEmployee(employee);
        order.setStatus(OrderStatus.OPEN);
        order.setStartTime(LocalDateTime.now()); // ✅ Bắt đầu tính thời gian ngay
        order.setTotalAmount(BigDecimal.ZERO);
        order.setTableFee(BigDecimal.ZERO);
        order.setProductFee(BigDecimal.ZERO);
        order.setStockDeducted(false);

        Order saved = orderRepository.save(order);
        System.out.println("✅ Order created: " + saved.getId());

        // ✅ Đảm bảo bàn đang OCCUPIED
        if (table.getStatus() != BidaTable.TableStatus.OCCUPIED) {
            table.setStatus(BidaTable.TableStatus.OCCUPIED);
            tableRepository.save(table);
            System.out.println("✅ Table " + table.getNumber() + " status -> OCCUPIED");
        }

        // Gửi notification
        webSocketService.notifyTableStatus(table.getId(), "OCCUPIED");
        webSocketService.notifyOrderUpdate(saved);

        log.info("✅ Order {} opened for table {}", saved.getId(), tableId);
        return saved;
    }
    // =====================================================
    // ADD ITEM TO ORDER
    // =====================================================

    @Transactional(rollbackFor = Exception.class)
    public Order addItemToOrder(Long orderId, Long productId, Integer quantity) {
        Order order = getOrderById(orderId);

        if (order.getStatus() != OrderStatus.OPEN) {
            throw new RuntimeException("Không thể thêm món khi đã kết thúc chơi");
        }

        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new RuntimeException("Product not found"));

        // TIME_BASED product
        if (product.isTimeBased()) {
            return addTimeBasedProduct(order, product);
        }

        // FOOD/DRINK
        return addFoodDrinkProduct(order, product, quantity);
    }

    private Order addTimeBasedProduct(Order order, Product product) {
        if (order.getTimeBasedProduct() != null) {
            throw new RuntimeException("Bàn đã có dịch vụ tính giờ rồi!");
        }

        order.setTimeBasedProduct(product);

        if (order.getStartTime() == null) {
            order.setStartTime(LocalDateTime.now());
            log.info("⏱️ Started timing for order {}", order.getId());
        }

        BigDecimal initialPrice = product.getPricePerMinute() != null ? product.getPricePerMinute() : BigDecimal.ZERO;
        OrderItem timeItem = new OrderItem(order, product, 1, initialPrice);
        timeItem.setSubtotal(BigDecimal.ZERO);
        orderItemRepository.save(timeItem);
        order.getItems().add(timeItem);

        updateOrderTotal(order.getId());
        webSocketService.notifyOrderUpdate(order);
        log.info("✅ Added time-based product {} to order {}", product.getName(), order.getId());
        return order;
    }

    private Order addFoodDrinkProduct(Order order, Product product, Integer quantity) {
        OrderItem item = orderItemRepository
                .findByOrderIdAndProductId(order.getId(), product.getId())
                .orElse(new OrderItem(order, product, 0, product.getPrice()));

        item.setQuantity(item.getQuantity() + quantity);
        BigDecimal subtotal = product.getPrice().multiply(BigDecimal.valueOf(item.getQuantity()));
        item.setSubtotal(subtotal);
        orderItemRepository.save(item);

        if (!order.getItems().contains(item)) {
            order.getItems().add(item);
        }

        updateOrderTotal(order.getId());
        webSocketService.notifyOrderUpdate(order);
        log.info("✅ Added {} x {} to order {}", quantity, product.getName(), order.getId());
        return order;
    }

    public OrderItem addItem(Long orderId, Long productId, Integer quantity) {
        addItemToOrder(orderId, productId, quantity);
        return orderItemRepository
                .findByOrderIdAndProductId(orderId, productId)
                .orElseThrow(() -> new RuntimeException("Order item not found"));
    }

    // =====================================================
    // ADD ITEM WITH PRICE
    // =====================================================

    @Transactional(rollbackFor = Exception.class)
    public OrderItem addItemWithPrice(Long orderId, Long productId, Integer quantity, BigDecimal unitPrice) {
        Order order = getOrderById(orderId);

        if (order.getStatus() != OrderStatus.OPEN) {
            throw new RuntimeException("Không thể thêm món khi đã kết thúc chơi");
        }

        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new RuntimeException("Product not found"));

        if (product.isTimeBased()) {
            return addTimeBasedProductWithPrice(order, product);
        }

        return addFoodDrinkProductWithPrice(order, product, quantity, unitPrice);
    }

    private OrderItem addTimeBasedProductWithPrice(Order order, Product product) {
        if (order.getTimeBasedProduct() != null) {
            throw new RuntimeException("Bàn đã có dịch vụ tính giờ rồi!");
        }

        order.setTimeBasedProduct(product);
        if (order.getStartTime() == null) {
            order.setStartTime(LocalDateTime.now());
        }

        BigDecimal initialPrice = product.getPricePerMinute() != null ? product.getPricePerMinute() : BigDecimal.ZERO;
        OrderItem timeItem = new OrderItem(order, product, 1, initialPrice);
        timeItem.setSubtotal(BigDecimal.ZERO);
        orderItemRepository.save(timeItem);
        order.getItems().add(timeItem);

        updateOrderTotal(order.getId());
        webSocketService.notifyOrderUpdate(order);
        log.info("✅ Added time-based product {}", product.getName());
        return timeItem;
    }

    private OrderItem addFoodDrinkProductWithPrice(Order order, Product product, Integer quantity,
            BigDecimal unitPrice) {
        OrderItem item = orderItemRepository
                .findByOrderIdAndProductId(order.getId(), product.getId())
                .orElse(new OrderItem(order, product, 0, unitPrice));

        item.setQuantity(item.getQuantity() + quantity);
        BigDecimal subtotal = unitPrice.multiply(BigDecimal.valueOf(item.getQuantity()));
        item.setSubtotal(subtotal);
        item.setUnitPrice(unitPrice);
        orderItemRepository.save(item);

        if (!order.getItems().contains(item)) {
            order.getItems().add(item);
        }

        updateOrderTotal(order.getId());
        webSocketService.notifyOrderUpdate(order);
        log.info("✅ Added {} x {} with price {}", quantity, product.getName(), unitPrice);
        return item;
    }

    // =====================================================
    // UPDATE ITEM QUANTITY
    // =====================================================

    @Transactional(rollbackFor = Exception.class)
    public void updateItemQuantity(Long orderId, Long itemId, Integer quantity, BigDecimal unitPrice) {
        Order order = getOrderById(orderId);

        OrderItem item = orderItemRepository.findById(itemId)
                .orElseThrow(() -> new RuntimeException("Order item not found"));

        Product product = item.getProduct();

        if (product.isTimeBased()) {
            throw new RuntimeException("Không thể thay đổi số lượng của dịch vụ tính giờ!");
        }

        if (quantity <= 0) {
            // Không hoàn stock vì chưa trừ
            order.removeItem(item);
            orderItemRepository.delete(item);
            log.info("✅ Removed item {} from order {}", itemId, orderId);
        } else {
            BigDecimal finalUnitPrice = (unitPrice != null && unitPrice.compareTo(BigDecimal.ZERO) > 0)
                    ? unitPrice
                    : product.getPrice();
            item.setQuantity(quantity);
            item.setUnitPrice(finalUnitPrice);
            item.setSubtotal(finalUnitPrice.multiply(BigDecimal.valueOf(quantity)));
            orderItemRepository.save(item);
            log.info("✅ Updated item {} quantity to {}", itemId, quantity);
        }

        updateOrderTotal(orderId);
        webSocketService.notifyOrderUpdate(order);
    }

    // =====================================================
    // REMOVE ITEM
    // =====================================================

    @Transactional(rollbackFor = Exception.class)
    public void removeItem(Long orderId, Long itemId) {
        Order order = getOrderById(orderId);

        OrderItem item = orderItemRepository.findById(itemId)
                .orElseThrow(() -> new RuntimeException("Item not found"));

        if (item.getProduct().isTimeBased()) {
            throw new RuntimeException("Không thể xóa dịch vụ tính giờ! Hãy kết thúc order để tính tiền.");
        }

        order.removeItem(item);
        orderItemRepository.delete(item);

        updateOrderTotal(orderId);
        webSocketService.notifyOrderUpdate(order);
        log.info("✅ Removed item {} from order {}", itemId, orderId);
    }

    // =====================================================
    // UPDATE ORDER TOTAL
    // =====================================================

    @Transactional(rollbackFor = Exception.class)
    public void updateOrderTotal(Long orderId) {
        Order order = getOrderById(orderId);

        // Tính tổng tiền sản phẩm (FOOD/DRINK)
        BigDecimal itemsTotal = order.getItems().stream()
                .filter(item -> !item.getProduct().isTimeBased())
                .map(OrderItem::getSubtotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // Tính tiền bàn
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

        log.debug("Order {} total: items={}, tableFee={}, total={}", orderId, itemsTotal, tableFee, total);
    }

    // =====================================================
    // FINISH PLAYING
    // =====================================================

    @Transactional(rollbackFor = Exception.class)
    public Order finishPlaying(Long orderId) {
        Order order = getOrderById(orderId);

        if (order.getStatus() != OrderStatus.OPEN) {
            throw new RuntimeException("Order không ở trạng thái đang chơi");
        }

        if (order.getStartTime() != null) {
            order.setEndTime(LocalDateTime.now());
            log.info("⏱️ Finished timing at: {}", order.getEndTime());
        }

        updateOrderTotal(orderId);
        order.setStatus(OrderStatus.WAITING_PAYMENT);

        Order saved = orderRepository.save(order);
        webSocketService.notifyOrderUpdate(saved);

        log.info("✅ Order {} finished playing", orderId);
        return saved;
    }

    // =====================================================
    // DEDUCT STOCK - Gọi InventoryService
    // =====================================================

    @Transactional(propagation = Propagation.REQUIRES_NEW, rollbackFor = Exception.class)
    public void deductStockForOrder(Long orderId) {
        Order order = getOrderById(orderId);

        if (order.isStockDeducted()) {
            log.info("Stock already deducted for order {}", orderId);
            return;
        }

        for (OrderItem item : order.getItems()) {
            Product product = item.getProduct();
            if (!product.isTimeBased() && item.getQuantity() > 0) {
                // ✅ Gọi InventoryService - transaction riêng
                inventoryService.exportStock(
                        orderId,
                        product,
                        item.getQuantity(),
                        "Bán hàng - Order #" + orderId);
                log.info("✅ Exported stock: {} x {}", product.getName(), item.getQuantity());
            }
        }

        order.setStockDeducted(true);
        orderRepository.save(order);
        log.info("✅ Stock deducted for order {}", orderId);
    }

    // =====================================================
    // REFUND STOCK - Gọi InventoryService
    // =====================================================

    @Transactional(propagation = Propagation.REQUIRES_NEW, rollbackFor = Exception.class)
    public void refundStockForOrder(Long orderId) {
        Order order = getOrderById(orderId);

        if (!order.isStockDeducted()) {
            log.info("Stock not deducted yet for order {}", orderId);
            return;
        }

        for (OrderItem item : order.getItems()) {
            Product product = item.getProduct();
            if (!product.isTimeBased() && item.getQuantity() > 0) {
                // ✅ Gọi InventoryService để hoàn stock
                inventoryService.importStock(
                        product.getId(),
                        item.getQuantity(),
                        "Hoàn hàng - Order #" + orderId);
                log.info("✅ Refunded stock: {} x {}", product.getName(), item.getQuantity());
            }
        }

        order.setStockDeducted(false);
        orderRepository.save(order);
        log.info("✅ Stock refunded for order {}", orderId);
    }

    // =====================================================
    // CLOSE ORDER (PAYMENT)
    // =====================================================

    @Transactional(rollbackFor = Exception.class)
    public void closeOrder(Long orderId) {
        Order order = getOrderById(orderId);

        if (order.getStatus() == OrderStatus.PAID) {
            log.info("Order {} already paid", orderId);
            return;
        }

        if (order.getStatus() != OrderStatus.OPEN &&
                order.getStatus() != OrderStatus.WAITING_PAYMENT) {
            throw new RuntimeException(
                    "Order không ở trạng thái đang chơi hoặc chờ thanh toán. Status: " + order.getStatus());
        }

        try {
            // ✅ Trừ stock - transaction riêng
            deductStockForOrder(orderId);

            // Cập nhật order
            order.setStatus(OrderStatus.PAID);
            orderRepository.save(order);

            // Cập nhật bàn
            BidaTable table = order.getTable();
            table.setStatus(BidaTable.TableStatus.FREE);
            tableRepository.save(table);

            // Notify
            webSocketService.notifyTableStatus(table.getId(), "FREE");
            webSocketService.notifyOrderUpdate(order);

            log.info("✅ Order {} closed successfully", orderId);

        } catch (Exception e) {
            log.error("Error closing order {}: {}", orderId, e.getMessage());
            throw new RuntimeException("Không thể đóng order: " + e.getMessage(), e);
        }
    }

    // =====================================================
    // CANCEL ORDER
    // =====================================================

    @Transactional(rollbackFor = Exception.class)
    public void cancelOrder(Long orderId) {
        Order order = getOrderById(orderId);

        if (order.getStatus() == OrderStatus.PAID) {
            throw new RuntimeException("Order đã thanh toán, không thể hủy");
        }

        try {
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
            log.info("✅ Order {} cancelled", orderId);

        } catch (Exception e) {
            log.error("Error cancelling order {}: {}", orderId, e.getMessage());
            throw new RuntimeException("Không thể hủy order: " + e.getMessage(), e);
        }
    }

    // =====================================================
    // OTHER METHODS
    // =====================================================

    @Transactional(rollbackFor = Exception.class)
    public Order updateOrderStatus(Long orderId, Order.OrderStatus status) {
        Order order = getOrderById(orderId);
        order.setStatus(status);
        Order saved = orderRepository.save(order);
        webSocketService.notifyOrderUpdate(saved);
        log.info("✅ Order {} status updated to {}", orderId, status);
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

    @Transactional(rollbackFor = Exception.class)
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

        LocalDateTime oldStartTime = order.getStartTime();
        LocalDateTime newStartTime = oldStartTime.minusMinutes(additionalMinutes);
        order.setStartTime(newStartTime);

        log.info("⏱️ Adjusted time for order {}: {} -> {} ({} minutes)",
                orderId, oldStartTime, newStartTime, additionalMinutes);

        orderRepository.save(order);
        updateOrderTotal(orderId);
        webSocketService.notifyOrderUpdate(order);

        log.info("✅ Time adjusted successfully for order {}", orderId);
    }

    @Transactional(rollbackFor = Exception.class)
    public Order moveOrderToTable(Long orderId, Long targetTableId) {
        log.info("=== MOVE ORDER TO TABLE ===");
        log.info("Order ID: {}", orderId);
        log.info("Target Table ID: {}", targetTableId);

        // 1. Lấy order hiện tại
        Order order = getOrderById(orderId);

        // 2. Kiểm tra trạng thái order
        if (order.getStatus() == OrderStatus.PAID) {
            throw new RuntimeException("Không thể chuyển đơn đã thanh toán");
        }
        if (order.getStatus() == OrderStatus.CANCELLED) {
            throw new RuntimeException("Không thể chuyển đơn đã hủy");
        }

        // 3. Lấy bàn đích
        BidaTable targetTable = tableRepository.findById(targetTableId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy bàn đích"));

        // 4. Kiểm tra bàn đích có trống không
        if (targetTable.getStatus() != BidaTable.TableStatus.FREE) {
            throw new RuntimeException("Bàn đích không trống (trạng thái: " + targetTable.getStatus() + ")");
        }

        // 5. Lưu bàn cũ
        BidaTable oldTable = order.getTable();

        // 6. Cập nhật bàn mới cho order
        order.setTable(targetTable);
        order = orderRepository.save(order);
        log.info("✅ Order {} moved to table {}", orderId, targetTableId);

        // 7. Cập nhật trạng thái bàn mới thành OCCUPIED
        targetTable.setStatus(BidaTable.TableStatus.OCCUPIED);
        tableRepository.save(targetTable);
        log.info("✅ Table {} status -> OCCUPIED", targetTable.getNumber());

        // 8. Cập nhật trạng thái bàn cũ
        if (oldTable != null) {
            // Kiểm tra xem bàn cũ có order active khác không
            boolean hasOtherActiveOrder = orderRepository.findByTableIdAndStatusNot(
                    oldTable.getId(), OrderStatus.PAID).stream().anyMatch(
                            o -> o.getStatus() == OrderStatus.OPEN ||
                                    o.getStatus() == OrderStatus.WAITING_PAYMENT);

            if (!hasOtherActiveOrder) {
                oldTable.setStatus(BidaTable.TableStatus.FREE);
                tableRepository.save(oldTable);
                log.info("✅ Old table {} status -> FREE", oldTable.getNumber());
                webSocketService.notifyTableStatus(oldTable.getId(), "FREE");
            } else {
                log.info("⚠️ Old table {} still has active orders, keeping OCCUPIED", oldTable.getNumber());
            }
        }

        // 9. Notify WebSocket
        webSocketService.notifyTableStatus(targetTable.getId(), "OCCUPIED");
        webSocketService.notifyOrderUpdate(order);

        log.info("✅ Successfully moved order {} to table {}", orderId, targetTableId);
        return order;
    }
}
