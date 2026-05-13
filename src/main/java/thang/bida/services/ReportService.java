package thang.bida.services;

import org.springframework.stereotype.Service;

import thang.bida.repository.BillRepository;
import thang.bida.repository.OrderRepository;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@Service
public class ReportService {

    private final BillRepository billRepository;
    private final OrderRepository orderRepository;

    public ReportService(BillRepository billRepository, OrderRepository orderRepository) {
        this.billRepository = billRepository;
        this.orderRepository = orderRepository;
    }

    public Map<String, Object> getDashboardReport() {
        Map<String, Object> report = new HashMap<>();

        LocalDateTime todayStart = LocalDateTime.now().withHour(0).withMinute(0).withSecond(0);
        LocalDateTime todayEnd = LocalDateTime.now().withHour(23).withMinute(59).withSecond(59);

        // ✅ SỬA - dùng count thay vì .size()
        long todayOrders = orderRepository.countByCreatedAtBetween(todayStart, todayEnd);
        report.put("todayOrders", todayOrders);

        long pendingOrders = orderRepository
                .findByStatus(thang.bida.model.Order.OrderStatus.WAITING_PAYMENT).size();
        report.put("pendingOrders", pendingOrders);

        return report;
    }

    public Map<String, Object> getRevenueReport(LocalDateTime startDate, LocalDateTime endDate) {
        Map<String, Object> report = new HashMap<>();

        long totalBills = billRepository.findByIssuedAtBetween(startDate, endDate).size();
        report.put("totalBills", totalBills);

        return report;
    }
}