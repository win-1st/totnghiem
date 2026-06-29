package thang.bida.services;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import thang.bida.model.Bill;
import thang.bida.model.CustomerPoint;
import thang.bida.model.Order;
import thang.bida.model.Promotion;
import thang.bida.model.Bill.PaymentStatus;
import thang.bida.repository.BillRepository;
import thang.bida.repository.CustomerPointRepository;
import thang.bida.repository.OrderRepository;
import thang.bida.repository.PromotionRepository;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
@Transactional
public class BillService {

    private final BillRepository billRepository;
    private final OrderRepository orderRepository;
    private final OrderService orderService;
    private final CustomerPointRepository customerPointRepository;
    private final PromotionRepository promotionRepository;

    public BillService(BillRepository billRepository,
            OrderRepository orderRepository,
            OrderService orderService,
            CustomerPointRepository customerPointRepository,
            PromotionRepository promotionRepository) {
        this.billRepository = billRepository;
        this.orderRepository = orderRepository;
        this.orderService = orderService;
        this.customerPointRepository = customerPointRepository;
        this.promotionRepository = promotionRepository;
    }

    public Bill createBill(Long orderId, Bill.PaymentMethod paymentMethod, String customerPhone, Long promotionId) {
        System.out.println("=== CREATE BILL ===");
        System.out.println("Order ID: " + orderId);
        System.out.println("Customer Phone: " + customerPhone);
        System.out.println("Promotion ID: " + promotionId);

        Order order = orderRepository.findByIdWithItems(orderId)
                .orElseThrow(() -> new RuntimeException("Order not found"));

        Optional<Bill> existingBill = billRepository.findByOrderId(orderId);
        if (existingBill.isPresent()) {
            return existingBill.get();
        }

        if (order.getStatus() == Order.OrderStatus.OPEN) {
            orderService.finishPlaying(orderId);
            order = orderRepository.findByIdWithItems(orderId)
                    .orElseThrow(() -> new RuntimeException("Order not found"));
        }

        orderService.updateOrderTotal(orderId);
        order = orderRepository.findByIdWithItems(orderId)
                .orElseThrow(() -> new RuntimeException("Order not found"));

        BigDecimal totalAmount = order.getTotalAmount() != null ? order.getTotalAmount() : BigDecimal.ZERO;

        System.out.println("Original total amount: " + totalAmount);

        // 🆕 Áp dụng khuyến mãi nếu có
        if (promotionId != null) {
            Promotion promotion = promotionRepository.findById(promotionId)
                    .orElseThrow(() -> new RuntimeException("Không tìm thấy khuyến mãi với ID: " + promotionId));

            System.out.println("Applying promotion: " + promotion.getName());

            if (promotion.getDiscountPercentage() != null
                    && promotion.getDiscountPercentage().compareTo(BigDecimal.ZERO) > 0) {
                // Giảm theo phần trăm
                BigDecimal discountPercent = promotion.getDiscountPercentage();
                BigDecimal discountAmount = totalAmount.multiply(discountPercent).divide(BigDecimal.valueOf(100), 2,
                        RoundingMode.HALF_UP);
                totalAmount = totalAmount.subtract(discountAmount);
                System.out.println("Discount percentage: " + discountPercent + "% - Saved: " + discountAmount);
            } else if (promotion.getDiscountAmount() != null
                    && promotion.getDiscountAmount().compareTo(BigDecimal.ZERO) > 0) {
                // Giảm theo số tiền
                totalAmount = totalAmount.subtract(promotion.getDiscountAmount());
                System.out.println("Discount amount: " + promotion.getDiscountAmount());
            }

            if (totalAmount.compareTo(BigDecimal.ZERO) < 0) {
                totalAmount = BigDecimal.ZERO;
            }

            System.out.println("Total after promotion: " + totalAmount);
        }

        Bill bill = new Bill();
        bill.setOrder(order);
        bill.setTotalAmount(totalAmount);
        bill.setPaymentMethod(paymentMethod);
        bill.setIssuedAt(LocalDateTime.now());
        bill.setTable(order.getTable());
        bill.setCustomer(order.getCustomer());
        bill.setPaymentStatus(PaymentStatus.PAID);

        Bill savedBill = billRepository.save(bill);

        // Tích điểm nếu có số điện thoại (dùng totalAmount đã áp dụng khuyến mãi)
        if (customerPhone != null && !customerPhone.isEmpty()) {
            accumulatePoints(customerPhone, totalAmount);
        }

        try {
            orderService.closeOrder(orderId);
        } catch (Exception e) {
            throw new RuntimeException("Không thể đóng order: " + e.getMessage());
        }

        return savedBill;
    }

    // Tích điểm cho khách hàng - Nhận phone và totalAmount
    private void accumulatePoints(String phone, BigDecimal totalAmount) {
        System.out.println("=== TÍCH ĐIỂM ===");
        System.out.println("SĐT: " + phone);
        System.out.println("Tổng tiền: " + totalAmount + "đ");

        // 10,000đ = 1 điểm
        BigDecimal minPointsAmount = new BigDecimal("10000");
        int points = totalAmount.divide(minPointsAmount, 0, RoundingMode.FLOOR).intValue();
        if (points <= 0) {
            System.out.println("💰 Số tiền không đủ để tích điểm (cần 10,000đ)");
            return;
        }

        Optional<CustomerPoint> existingCp = customerPointRepository.findByPhone(phone);

        if (existingCp.isPresent()) {
            CustomerPoint cp = existingCp.get();
            cp.addPoints(points);
            customerPointRepository.save(cp);
            System.out.println("⭐ Đã cộng " + points + " điểm cho KH: " + phone);
            System.out.println("📊 Điểm hiện tại: " + cp.getTotalPoints());
        } else {
            CustomerPoint newCp = new CustomerPoint();
            newCp.setPhone(phone);
            newCp.setCustomerName("Khách hàng " + phone);
            newCp.setTotalPoints(points);
            customerPointRepository.save(newCp);
            System.out.println("⭐ Đã tạo mới và cộng " + points + " điểm cho KH: " + phone);
        }
    }

    // Các method khác
    public Bill getBillByOrder(Long orderId) {
        return billRepository.findByOrderId(orderId).orElse(null);
    }

    @Transactional(readOnly = true)
    public Bill getBillById(Long id) {
        return billRepository.findBillWithDetails(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy hóa đơn #" + id));
    }

    public void confirmCashPayment(Long billId) {
        Bill bill = billRepository.findById(billId)
                .orElseThrow(() -> new RuntimeException("Bill not found"));
        if (bill.getPaymentStatus() == PaymentStatus.PAID)
            return;
        bill.setPaymentStatus(PaymentStatus.PAID);
        billRepository.save(bill);
        Order order = bill.getOrder();
        if (order != null && order.getStatus() != Order.OrderStatus.PAID) {
            orderService.closeOrder(order.getId());
        }
    }

    public void confirmMomoPayment(Long billId) {
        Bill bill = billRepository.findById(billId)
                .orElseThrow(() -> new RuntimeException("Bill not found"));
        if (bill.getPaymentStatus() == PaymentStatus.PAID)
            return;
        bill.setPaymentStatus(PaymentStatus.PAID);
        billRepository.save(bill);
        Order order = bill.getOrder();
        if (order != null && order.getStatus() != Order.OrderStatus.PAID) {
            orderService.closeOrder(order.getId());
        }
    }

    public List<Bill> getBillsByPaymentStatus(Bill.PaymentStatus paymentStatus) {
        return billRepository.findByPaymentStatus(paymentStatus);
    }

    public List<Bill> getAllBills() {
        return billRepository.findAllWithTable();
    }

    public BigDecimal getTotalRevenue(LocalDateTime startDate, LocalDateTime endDate) {
        BigDecimal revenue = billRepository.getTotalRevenueByDateRange(startDate, endDate);
        return revenue != null ? revenue : BigDecimal.ZERO;
    }
}