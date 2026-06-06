package thang.bida.services;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import thang.bida.model.Bill;
import thang.bida.model.Order;
import thang.bida.model.Bill.PaymentMethod;
import thang.bida.model.Bill.PaymentStatus;
import thang.bida.repository.BillRepository;
import thang.bida.repository.OrderRepository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
@Transactional
public class BillService {

    private final BillRepository billRepository;
    private final OrderRepository orderRepository;
    private final OrderService orderService;

    public BillService(BillRepository billRepository,
            OrderRepository orderRepository,
            OrderService orderService) {
        this.billRepository = billRepository;
        this.orderRepository = orderRepository;
        this.orderService = orderService;
    }

    public Bill createBill(Long orderId, Bill.PaymentMethod paymentMethod) {
        System.out.println("=== CREATE BILL ===");
        System.out.println("Order ID: " + orderId);
        System.out.println("Payment Method: " + paymentMethod);

        // 1. Lấy order
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Order not found"));

        // Kiểm tra bill đã tồn tại
        Optional<Bill> existingBill = billRepository.findByOrderId(orderId);
        if (existingBill.isPresent()) {
            System.out.println("Bill already exists for order " + orderId);
            return existingBill.get();
        }

        // 2. Nếu OPEN → finish
        if (order.getStatus() == Order.OrderStatus.OPEN) {
            System.out.println("Order is OPEN, finishing...");
            orderService.finishPlaying(orderId);
            order = orderRepository.findById(orderId).get();
        }

        // 3. Tính tổng tiền
        orderService.updateOrderTotal(orderId);
        order = orderRepository.findById(orderId).get();
        BigDecimal totalAmount = order.getTotalAmount() != null ? order.getTotalAmount() : BigDecimal.ZERO;
        System.out.println("Total amount: " + totalAmount);

        // 4. Tạo bill
        Bill bill = new Bill();
        bill.setOrder(order);
        bill.setTotalAmount(totalAmount);
        bill.setPaymentMethod(paymentMethod);
        bill.setIssuedAt(LocalDateTime.now());

        // ✅ QUAN TRỌNG: Với thanh toán online (MOMO/BANKING) thì đã thanh toán ngay
        if (paymentMethod == PaymentMethod.CASH) {
            bill.setPaymentStatus(PaymentStatus.PENDING);
        } else {
            bill.setPaymentStatus(PaymentStatus.PAID); // ONLINE: PAID ngay
        }

        Bill savedBill = billRepository.save(bill);
        System.out.println("Bill saved with ID: " + savedBill.getId() + ", Status: " + savedBill.getPaymentStatus());

        // 5. Close order → trừ stock và giải phóng bàn
        System.out.println("Closing order...");
        try {
            orderService.closeOrder(orderId);
            System.out.println("Order closed successfully!");
        } catch (Exception e) {
            System.err.println("Error closing order: " + e.getMessage());
            throw new RuntimeException("Không thể đóng order: " + e.getMessage());
        }

        return savedBill;
    }

    public Bill getBillByOrder(Long orderId) {
        return billRepository.findByOrderId(orderId).orElse(null);
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

    public BigDecimal getTotalRevenue(LocalDateTime startDate, LocalDateTime endDate) {
        BigDecimal revenue = billRepository.getTotalRevenueByDateRange(startDate, endDate);
        return revenue != null ? revenue : BigDecimal.ZERO;
    }
}