package thang.bida.services;

import thang.bida.model.*;
import thang.bida.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

@Service
@Transactional
public class DashboardService {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private BillRepository billRepository;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private BidaTableRepository bidaTableRepository;

    @Autowired
    private OrderItemRepository orderItemRepository;

    // ===== TỔNG QUAN DASHBOARD =====
    public Map<String, Object> getDashboardOverview(String timeRange, Integer year) {
        LocalDate startDate;
        LocalDate endDate = LocalDate.now();

        if (year != null && "year".equalsIgnoreCase(timeRange)) {
            startDate = LocalDate.of(year, 1, 1);
            endDate = LocalDate.of(year, 12, 31);
        } else {
            startDate = getStartDateByTimeRange(timeRange);
        }

        Map<String, Object> response = new HashMap<>();

        long totalUsers = userRepository.count();
        long newUsers = userRepository.countByCreatedAtAfter(startDate.atStartOfDay());

        long totalOrders = orderRepository.countByCreatedAtBetween(
                startDate.atStartOfDay(), endDate.atTime(23, 59, 59));
        long newOrders = orderRepository.countByCreatedAtAfter(startDate.atStartOfDay());

        BigDecimal totalRevenue = billRepository.getRevenueBetween(
                startDate.atStartOfDay(), endDate.atTime(23, 59, 59));
        if (totalRevenue == null) {
            totalRevenue = BigDecimal.ZERO;
        }

        BigDecimal periodRevenue = totalRevenue;

        long totalProducts = productRepository.countByActiveTrue();
        long activeTables = bidaTableRepository.countByStatus(BidaTable.TableStatus.OCCUPIED);

        // Tính growth rate so với năm trước
        BigDecimal previousPeriodRevenue = BigDecimal.ZERO;
        if (year != null && "year".equalsIgnoreCase(timeRange)) {
            LocalDate prevYearStart = LocalDate.of(year - 1, 1, 1);
            LocalDate prevYearEnd = LocalDate.of(year - 1, 12, 31);
            previousPeriodRevenue = billRepository.getRevenueBetween(
                    prevYearStart.atStartOfDay(),
                    prevYearEnd.atTime(23, 59, 59));
            if (previousPeriodRevenue == null) {
                previousPeriodRevenue = BigDecimal.ZERO;
            }
        }

        BigDecimal growthRate = BigDecimal.ZERO;
        if (previousPeriodRevenue.compareTo(BigDecimal.ZERO) > 0) {
            growthRate = periodRevenue.subtract(previousPeriodRevenue)
                    .divide(previousPeriodRevenue, 4, RoundingMode.HALF_UP)
                    .multiply(BigDecimal.valueOf(100));
        }

        response.put("totalUsers", totalUsers);
        response.put("newUsers", newUsers);
        response.put("totalOrders", totalOrders);
        response.put("newOrders", newOrders);
        response.put("totalRevenue", totalRevenue);
        response.put("periodRevenue", periodRevenue);
        response.put("totalProducts", totalProducts);
        response.put("activeTables", activeTables);
        response.put("growthRate", growthRate);
        response.put("timeRange", timeRange);
        response.put("year", year);

        return response;
    }

    // ===== THỐNG KÊ THEO THỜI GIAN =====
    public Map<String, Object> getTimeBasedStatistics(String timeRange, LocalDate startDate, LocalDate endDate) {
        Map<String, Object> result = new HashMap<>();

        if (startDate == null || endDate == null) {
            startDate = getStartDateByTimeRange(timeRange);
            endDate = LocalDate.now();
        }

        LocalDateTime startDateTime = startDate.atStartOfDay();
        LocalDateTime endDateTime = endDate.atTime(23, 59, 59);

        List<Object[]> dailyRevenue = billRepository.getDailyRevenueBetween(startDateTime, endDateTime);
        Map<String, BigDecimal> revenueByDate = new HashMap<>();
        if (dailyRevenue != null) {
            revenueByDate = dailyRevenue.stream()
                    .collect(Collectors.toMap(
                            arr -> {
                                Object dateObj = arr[0];
                                if (dateObj instanceof java.sql.Date) {
                                    return ((java.sql.Date) dateObj).toLocalDate()
                                            .format(DateTimeFormatter.ofPattern("dd/MM"));
                                } else if (dateObj instanceof LocalDate) {
                                    return ((LocalDate) dateObj).format(DateTimeFormatter.ofPattern("dd/MM"));
                                }
                                return arr[0].toString();
                            },
                            arr -> {
                                BigDecimal revenue = (BigDecimal) arr[1];
                                return revenue != null ? revenue : BigDecimal.ZERO;
                            }));
        }

        List<Object[]> dailyOrders = orderRepository.getDailyOrdersBetween(startDateTime, endDateTime);
        Map<String, Long> ordersByDate = new HashMap<>();
        if (dailyOrders != null) {
            ordersByDate = dailyOrders.stream()
                    .collect(Collectors.toMap(
                            arr -> {
                                Object dateObj = arr[0];
                                if (dateObj instanceof java.sql.Date) {
                                    return ((java.sql.Date) dateObj).toLocalDate()
                                            .format(DateTimeFormatter.ofPattern("dd/MM"));
                                } else if (dateObj instanceof LocalDate) {
                                    return ((LocalDate) dateObj).format(DateTimeFormatter.ofPattern("dd/MM"));
                                }
                                return arr[0].toString();
                            },
                            arr -> {
                                Long count = (Long) arr[1];
                                return count != null ? count : 0L;
                            }));
        }

        BigDecimal totalRevenue = billRepository.getRevenueBetween(startDateTime, endDateTime);
        if (totalRevenue == null) {
            totalRevenue = BigDecimal.ZERO;
        }

        result.put("revenueByDate", revenueByDate);
        result.put("ordersByDate", ordersByDate);
        result.put("totalRevenue", totalRevenue);
        result.put("totalOrders", orderRepository.countByCreatedAtBetween(startDateTime, endDateTime));

        return result;
    }

    // ===== TOP SẢN PHẨM BÁN CHẠY =====
    public List<Map<String, Object>> getTopProducts(int limit, String timeRange, Integer year) {
        LocalDateTime startDate;
        LocalDateTime endDate = LocalDateTime.now();

        if (year != null && "year".equalsIgnoreCase(timeRange)) {
            startDate = LocalDate.of(year, 1, 1).atStartOfDay();
            endDate = LocalDate.of(year, 12, 31).atTime(23, 59, 59);
        } else {
            startDate = getStartDateByTimeRange(timeRange).atStartOfDay();
        }

        Pageable pageable = PageRequest.of(0, limit);

        List<Object[]> topProductsData;
        try {
            topProductsData = orderItemRepository.getTopProductsBetween(startDate, endDate, pageable);
        } catch (Exception e) {
            topProductsData = orderItemRepository.getTopProducts(startDate, pageable);
        }

        if (topProductsData == null || topProductsData.isEmpty()) {
            return new ArrayList<>();
        }

        return topProductsData.stream().map(arr -> {
            Map<String, Object> productMap = new HashMap<>();
            try {
                productMap.put("id", arr[0] != null ? arr[0] : 0L);
                productMap.put("name", arr[1] != null ? arr[1] : "Không xác định");
                productMap.put("soldQuantity", arr[2] != null ? arr[2] : 0);
                productMap.put("revenue", arr[3] != null ? arr[3] : BigDecimal.ZERO);
            } catch (Exception e) {
                productMap.put("id", 0L);
                productMap.put("name", "Sản phẩm");
                productMap.put("soldQuantity", 0);
                productMap.put("revenue", BigDecimal.ZERO);
            }
            return productMap;
        }).collect(Collectors.toList());
    }

    // ===== DOANH THU THEO THÁNG =====
    public Map<String, BigDecimal> getMonthlyRevenue(Integer yearParam) {
        final int year;
        if (yearParam == null) {
            year = LocalDate.now().getYear();
        } else {
            year = yearParam;
        }

        LocalDateTime startDate = LocalDate.of(year, 1, 1).atStartOfDay();
        LocalDateTime endDate = LocalDate.of(year, 12, 31).atTime(23, 59, 59);

        List<Object[]> monthlyData = billRepository.getMonthlyRevenue(startDate, endDate);

        Map<String, BigDecimal> result = new LinkedHashMap<>();
        for (int i = 1; i <= 12; i++) {
            String monthKey = String.format("%02d/%d", i, year);
            result.put(monthKey, BigDecimal.ZERO);
        }

        if (monthlyData != null) {
            monthlyData.forEach(arr -> {
                try {
                    Integer month = (Integer) arr[0];
                    BigDecimal revenue = (BigDecimal) arr[1];
                    if (revenue == null) {
                        revenue = BigDecimal.ZERO;
                    }
                    String monthKey = String.format("%02d/%d", month, year);
                    result.put(monthKey, revenue);
                } catch (Exception e) {
                    // Bỏ qua nếu dữ liệu không đúng format
                }
            });
        }

        return result;
    }

    // ===== THỐNG KÊ NGƯỜI DÙNG =====
    public Map<String, Object> getUserStatistics() {
        Map<String, Object> stats = new HashMap<>();

        long adminCount = userRepository.countByRole(Role.ADMIN);
        long staffCount = userRepository.countByRole(Role.STAFF);
        long customerCount = userRepository.countByRole(Role.CUSTOMER);

        LocalDateTime startOfMonth = YearMonth.now().atDay(1).atStartOfDay();
        long newUsersThisMonth = userRepository.countByCreatedAtAfter(startOfMonth);

        long totalUsers = adminCount + staffCount + customerCount;
        long activeUsers = userRepository.countByIsActiveTrue();

        stats.put("totalUsers", totalUsers);
        stats.put("adminCount", adminCount);
        stats.put("staffCount", staffCount);
        stats.put("customerCount", customerCount);
        stats.put("newUsersThisMonth", newUsersThisMonth);
        stats.put("activeUsers", activeUsers);

        return stats;
    }

    // ===== HOẠT ĐỘNG GẦN ĐÂY =====
    public List<Map<String, Object>> getRecentActivities(int limit) {
        LocalDateTime cutoff = LocalDateTime.now().minusDays(7);

        List<Map<String, Object>> activities = new ArrayList<>();

        List<Order> recentOrders = orderRepository.findTop10ByCreatedAtAfterOrderByCreatedAtDesc(cutoff);
        if (recentOrders != null && !recentOrders.isEmpty()) {
            for (Order order : recentOrders) {
                Map<String, Object> activity = new HashMap<>();
                activity.put("time", order.getCreatedAt().format(DateTimeFormatter.ofPattern("HH:mm")));
                String employeeInfo = "Nhân viên #"
                        + (order.getEmployee() != null ? order.getEmployee().getId() : "Unknown");
                activity.put("content", String.format("Đơn hàng #%s được tạo bởi %s",
                        order.getId(), employeeInfo));
                activity.put("color", "blue");
                activities.add(activity);
            }
        }

        List<Bill> recentBills = billRepository.findTop10ByCreatedAtAfterOrderByCreatedAtDesc(cutoff);
        if (recentBills != null && !recentBills.isEmpty()) {
            for (Bill bill : recentBills) {
                Map<String, Object> activity = new HashMap<>();
                activity.put("time", bill.getCreatedAt().format(DateTimeFormatter.ofPattern("HH:mm")));
                activity.put("content", String.format("Hóa đơn #%s đã thanh toán - %s VNĐ",
                        bill.getId(), bill.getTotalAmount()));
                activity.put("color", "green");
                activities.add(activity);
            }
        }

        List<User> recentUsers = userRepository.findTop5ByOrderByCreatedAtDesc();
        if (recentUsers != null && !recentUsers.isEmpty()) {
            for (User user : recentUsers) {
                if (user.getCreatedAt() != null && user.getCreatedAt().isAfter(cutoff)) {
                    Map<String, Object> activity = new HashMap<>();
                    activity.put("time", user.getCreatedAt().format(DateTimeFormatter.ofPattern("HH:mm")));
                    activity.put("content", String.format("Người dùng mới: %s (%s)",
                            user.getFullName() != null ? user.getFullName() : user.getPhone(),
                            user.getEmail() != null ? user.getEmail() : ""));
                    activity.put("color", "purple");
                    activities.add(activity);
                }
            }
        }

        activities.sort((a, b) -> {
            String timeA = (String) a.get("time");
            String timeB = (String) b.get("time");
            return timeB.compareTo(timeA);
        });

        return activities.stream().limit(limit).collect(Collectors.toList());
    }

    // ===== THỐNG KÊ BÀN =====
    public Map<String, Object> getTableStatistics() {
        Map<String, Object> stats = new HashMap<>();

        long totalTables = bidaTableRepository.count();
        long occupiedTables = bidaTableRepository.countByStatus(BidaTable.TableStatus.OCCUPIED);
        long freeTables = bidaTableRepository.countByStatus(BidaTable.TableStatus.FREE);
        long reservedTables = bidaTableRepository.countByStatus(BidaTable.TableStatus.RESERVED);

        double occupancyRate = 0;
        if (totalTables > 0) {
            occupancyRate = (occupiedTables * 100.0) / totalTables;
        }

        stats.put("totalTables", totalTables);
        stats.put("occupiedTables", occupiedTables);
        stats.put("freeTables", freeTables);
        stats.put("reservedTables", reservedTables);
        stats.put("occupancyRate", Math.round(occupancyRate * 100.0) / 100.0);

        return stats;
    }

    // ===== DOANH THU THEO NGÀY =====
    public Map<String, Object> getDailyRevenue(LocalDate date) {
        if (date == null) {
            date = LocalDate.now();
        }

        LocalDateTime startOfDay = date.atStartOfDay();
        LocalDateTime endOfDay = date.atTime(23, 59, 59);

        Map<String, Object> result = new HashMap<>();

        BigDecimal dailyRevenue = billRepository.getRevenueBetween(startOfDay, endOfDay);
        if (dailyRevenue == null) {
            dailyRevenue = BigDecimal.ZERO;
        }

        long dailyOrders = orderRepository.countByCreatedAtBetween(startOfDay, endOfDay);

        List<Object[]> paymentMethodStats = billRepository.getRevenueByPaymentMethod(startOfDay, endOfDay);
        Map<String, BigDecimal> revenueByPaymentMethod = new HashMap<>();
        if (paymentMethodStats != null) {
            revenueByPaymentMethod = paymentMethodStats.stream()
                    .collect(Collectors.toMap(
                            arr -> arr[0].toString(),
                            arr -> (BigDecimal) arr[1]));
        }

        BigDecimal averageOrderValue = BigDecimal.ZERO;
        if (dailyOrders > 0) {
            averageOrderValue = dailyRevenue.divide(BigDecimal.valueOf(dailyOrders), 2, RoundingMode.HALF_UP);
        }

        result.put("date", date.toString());
        result.put("revenue", dailyRevenue);
        result.put("orders", dailyOrders);
        result.put("revenueByPaymentMethod", revenueByPaymentMethod);
        result.put("averageOrderValue", averageOrderValue);

        return result;
    }

    // ===== THỐNG KÊ ĐƠN HÀNG =====
    public Map<String, Object> getOrderStatistics(String timeRange, Integer year) {
        Map<String, Object> stats = new HashMap<>();

        LocalDateTime startDateTime;
        LocalDateTime endDateTime = LocalDateTime.now();

        if (year != null && "year".equalsIgnoreCase(timeRange)) {
            startDateTime = LocalDate.of(year, 1, 1).atStartOfDay();
            endDateTime = LocalDate.of(year, 12, 31).atTime(23, 59, 59);
        } else {
            LocalDate startDate = getStartDateByTimeRange(timeRange);
            startDateTime = startDate.atStartOfDay();
        }

        long totalOrders = orderRepository.countByCreatedAtBetween(startDateTime, endDateTime);

        long openOrders = orderRepository.countOrdersByStatusBetween(
                Order.OrderStatus.OPEN, startDateTime, endDateTime);
        long waitingPaymentOrders = orderRepository.countOrdersByStatusBetween(
                Order.OrderStatus.WAITING_PAYMENT, startDateTime, endDateTime);
        long paidOrders = orderRepository.countOrdersByStatusBetween(
                Order.OrderStatus.PAID, startDateTime, endDateTime);
        long cancelledOrders = orderRepository.countOrdersByStatusBetween(
                Order.OrderStatus.CANCELLED, startDateTime, endDateTime);

        BigDecimal revenueFromPaidOrders = billRepository.getRevenueFromPaidOrders(startDateTime, endDateTime);
        if (revenueFromPaidOrders == null) {
            revenueFromPaidOrders = BigDecimal.ZERO;
        }

        // Tính growth rate cho đơn hàng
        BigDecimal ordersGrowth = BigDecimal.ZERO;
        if (year != null && "year".equalsIgnoreCase(timeRange)) {
            LocalDateTime prevYearStart = LocalDate.of(year - 1, 1, 1).atStartOfDay();
            LocalDateTime prevYearEnd = LocalDate.of(year - 1, 12, 31).atTime(23, 59, 59);
            long prevYearOrders = orderRepository.countByCreatedAtBetween(prevYearStart, prevYearEnd);
            if (prevYearOrders > 0) {
                ordersGrowth = BigDecimal.valueOf(totalOrders - prevYearOrders)
                        .divide(BigDecimal.valueOf(prevYearOrders), 4, RoundingMode.HALF_UP)
                        .multiply(BigDecimal.valueOf(100));
            }
        }

        // Tính growth rate cho giá trị trung bình
        BigDecimal avgValueGrowth = BigDecimal.ZERO;
        if (year != null && "year".equalsIgnoreCase(timeRange)) {
            LocalDateTime prevYearStart = LocalDate.of(year - 1, 1, 1).atStartOfDay();
            LocalDateTime prevYearEnd = LocalDate.of(year - 1, 12, 31).atTime(23, 59, 59);
            BigDecimal prevYearRevenue = billRepository.getRevenueFromPaidOrders(prevYearStart, prevYearEnd);
            if (prevYearRevenue == null) {
                prevYearRevenue = BigDecimal.ZERO;
            }

            long prevYearOrders = orderRepository.countByCreatedAtBetween(prevYearStart, prevYearEnd);
            BigDecimal prevYearAvg = BigDecimal.ZERO;
            if (prevYearOrders > 0) {
                prevYearAvg = prevYearRevenue.divide(BigDecimal.valueOf(prevYearOrders), 2, RoundingMode.HALF_UP);
            }

            BigDecimal currentAvg = BigDecimal.ZERO;
            if (totalOrders > 0) {
                currentAvg = revenueFromPaidOrders.divide(BigDecimal.valueOf(totalOrders), 2, RoundingMode.HALF_UP);
            }

            if (prevYearAvg.compareTo(BigDecimal.ZERO) > 0) {
                avgValueGrowth = currentAvg.subtract(prevYearAvg)
                        .divide(prevYearAvg, 4, RoundingMode.HALF_UP)
                        .multiply(BigDecimal.valueOf(100));
            }
        }

        stats.put("totalOrders", totalOrders);
        stats.put("openOrders", openOrders);
        stats.put("waitingPaymentOrders", waitingPaymentOrders);
        stats.put("paidOrders", paidOrders);
        stats.put("cancelledOrders", cancelledOrders);
        stats.put("revenueFromPaidOrders", revenueFromPaidOrders);
        stats.put("timeRange", timeRange);
        stats.put("year", year);
        stats.put("ordersGrowth", ordersGrowth);
        stats.put("avgValueGrowth", avgValueGrowth);

        return stats;
    }

    // ===== CÁC PHƯƠNG THỨC HỖ TRỢ =====
    private LocalDate getStartDateByTimeRange(String timeRange) {
        LocalDate now = LocalDate.now();

        if (timeRange == null) {
            return now.minusDays(7);
        }

        switch (timeRange.toUpperCase()) {
            case "TODAY":
            case "DAY":
                return now;
            case "WEEK":
                return now.minusDays(7);
            case "MONTH":
                return now.minusMonths(1);
            case "QUARTER":
                return now.minusMonths(3);
            case "YEAR":
                return now.minusYears(1);
            default:
                return now.minusDays(7);
        }
    }

    private int getDaysByTimeRange(String timeRange) {
        if (timeRange == null) {
            return 7;
        }

        switch (timeRange.toLowerCase()) {
            case "day":
                return 1;
            case "week":
                return 7;
            case "month":
                return 30;
            case "quarter":
                return 90;
            case "year":
                return 365;
            default:
                return 7;
        }
    }
}