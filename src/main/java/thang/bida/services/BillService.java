package thang.bida.services;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import thang.bida.model.Bill;
import thang.bida.model.Order;
import thang.bida.repository.BillRepository;
import thang.bida.repository.OrderRepository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;

@Service
@Transactional
public class BillService {

    private final BillRepository billRepository;
    private final OrderRepository orderRepository;

    public BillService(BillRepository billRepository, OrderRepository orderRepository) {
        this.billRepository = billRepository;
        this.orderRepository = orderRepository;
    }

    public Bill createBill(Long orderId, Bill.PaymentMethod paymentMethod) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Order not found"));

        // 1️⃣ Set end time
        LocalDateTime endTime = LocalDateTime.now();
        order.setEndTime(endTime);

        // 2️⃣ Tính tiền giờ
        BigDecimal playFee = calculatePlayFee(order.getStartTime(), endTime);

        // 3️⃣ Tổng tiền = tiền giờ + tiền đồ
        BigDecimal total = order.getTotalAmount().add(playFee);
        order.setTotalAmount(total);

        // 4️⃣ Đóng order
        order.setStatus(Order.OrderStatus.PAID);
        orderRepository.save(order);

        // 5️⃣ Tạo bill
        Bill bill = new Bill(order, total);
        bill.setPaymentMethod(paymentMethod);
        bill.setPaymentStatus(Bill.PaymentStatus.PENDING);
        bill.setIssuedAt(endTime);

        return billRepository.save(bill);
    }

    public Bill updatePaymentStatus(Long billId, Bill.PaymentStatus paymentStatus) {
        Bill bill = billRepository.findById(billId)
                .orElseThrow(() -> new RuntimeException("Bill not found"));
        bill.setPaymentStatus(paymentStatus);
        return billRepository.save(bill);
    }

    public Bill getBillByOrderId(Long orderId) {
        Optional<Bill> bill = billRepository.findByOrderId(orderId);
        return bill.orElse(null);
    }

    // ========== THÊM CÁC PHƯƠNG THỨC BỊ THIẾU ==========

    // 1. Phương thức getBillByOrder - được gọi từ Controller
    public Bill getBillByOrder(Long orderId) {
        return getBillByOrderId(orderId);
    }

    // 2. Phương thức confirmCashPayment - được gọi từ Controller
    public void confirmCashPayment(Long billId) {
        Bill bill = billRepository.findById(billId)
                .orElseThrow(() -> new RuntimeException("Bill not found"));

        bill.setPaymentMethod(Bill.PaymentMethod.CASH);
        bill.setPaymentStatus(Bill.PaymentStatus.PAID);
        bill.setIssuedAt(LocalDateTime.now());

        billRepository.save(bill);

        // Cập nhật trạng thái order nếu cần
        Order order = bill.getOrder();
        if (order != null && order.getStatus() != Order.OrderStatus.PAID) {
            order.setStatus(Order.OrderStatus.PAID);
            orderRepository.save(order);
        }
    }

    // 3. Phương thức confirmMomoPayment - được gọi từ Controller
    public void confirmMomoPayment(Long billId) {
        Bill bill = billRepository.findById(billId)
                .orElseThrow(() -> new RuntimeException("Bill not found"));

        bill.setPaymentMethod(Bill.PaymentMethod.MOMO);
        bill.setPaymentStatus(Bill.PaymentStatus.PAID);
        bill.setIssuedAt(LocalDateTime.now());

        billRepository.save(bill);

        // Cập nhật trạng thái order nếu cần
        Order order = bill.getOrder();
        if (order != null && order.getStatus() != Order.OrderStatus.PAID) {
            order.setStatus(Order.OrderStatus.PAID);
            orderRepository.save(order);
        }
    }

    // 4. Phương thức bổ sung - lấy danh sách bill theo trạng thái
    public java.util.List<Bill> getBillsByPaymentStatus(Bill.PaymentStatus paymentStatus) {
        return billRepository.findByPaymentStatus(paymentStatus);
    }

    public BigDecimal getTotalRevenue(LocalDateTime startDate, LocalDateTime endDate) {
        BigDecimal revenue = billRepository.getTotalRevenueByDateRange(startDate, endDate);
        return revenue != null ? revenue : BigDecimal.ZERO;
    }

    private BigDecimal calculatePlayFee(LocalDateTime start, LocalDateTime end) {
        if (start == null || end == null) {
            return BigDecimal.ZERO;
        }

        long minutes = java.time.Duration.between(start, end).toMinutes();
        if (minutes <= 0)
            minutes = 1;

        // Giá bàn: 60.000 / giờ
        BigDecimal pricePerHour = new BigDecimal("60000");

        // Làm tròn lên theo phút
        BigDecimal hours = BigDecimal.valueOf(minutes)
                .divide(BigDecimal.valueOf(60), 2, java.math.RoundingMode.UP);

        return pricePerHour.multiply(hours);
    }

}