package thang.bida.controllers;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import thang.bida.services.DashboardService;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/dashboard")
@CrossOrigin(origins = "*")
public class DashboardController {

        @Autowired
        private DashboardService dashboardService;

        @GetMapping("/overview")
        @PreAuthorize("hasAnyRole('ADMIN','STAFF')") // SỬA: cho phép STAFF
        public ResponseEntity<?> getDashboardOverview(
                        @RequestParam(required = false, defaultValue = "week") String timeRange) {

                Map<String, Object> response = dashboardService.getDashboardOverview(timeRange);

                return ResponseEntity.ok(Map.of(
                                "success", true,
                                "message", "Lấy dữ liệu dashboard thành công",
                                "data", response));
        }

        @GetMapping("/statistics/time-based")
        @PreAuthorize("hasAnyRole('ADMIN','STAFF')") // SỬA
        public ResponseEntity<?> getTimeBasedStatistics(
                        @RequestParam String timeRange,
                        @RequestParam(required = false) LocalDate startDate,
                        @RequestParam(required = false) LocalDate endDate) {

                Map<String, Object> stats = dashboardService.getTimeBasedStatistics(timeRange, startDate, endDate);

                return ResponseEntity.ok(Map.of(
                                "success", true,
                                "message", "Lấy thống kê theo thời gian thành công",
                                "data", stats));
        }

        @GetMapping("/top-products")
        @PreAuthorize("hasAnyRole('ADMIN','STAFF')") // SỬA
        public ResponseEntity<?> getTopProducts(
                        @RequestParam(required = false, defaultValue = "10") int limit,
                        @RequestParam(required = false) String timeRange) {

                List<Map<String, Object>> topProducts = dashboardService.getTopProducts(limit, timeRange);

                return ResponseEntity.ok(Map.of(
                                "success", true,
                                "message", "Lấy top sản phẩm bán chạy thành công",
                                "data", topProducts,
                                "count", topProducts.size()));
        }

        @GetMapping("/revenue/monthly")
        @PreAuthorize("hasAnyRole('ADMIN','STAFF')") // SỬA
        public ResponseEntity<?> getMonthlyRevenue(
                        @RequestParam(required = false) Integer year) {

                Map<String, BigDecimal> revenue = dashboardService.getMonthlyRevenue(year);

                return ResponseEntity.ok(Map.of(
                                "success", true,
                                "message", "Lấy doanh thu theo tháng thành công",
                                "data", revenue));
        }

        @GetMapping("/users/statistics")
        @PreAuthorize("hasRole('ADMIN')") // GIỮ NGUYÊN - chỉ ADMIN
        public ResponseEntity<?> getUserStatistics() {

                Map<String, Object> userStats = dashboardService.getUserStatistics();

                return ResponseEntity.ok(Map.of(
                                "success", true,
                                "message", "Lấy thống kê người dùng thành công",
                                "data", userStats));
        }

        @GetMapping("/recent-activities")
        @PreAuthorize("hasAnyRole('ADMIN','STAFF')") // SỬA
        public ResponseEntity<?> getRecentActivities(
                        @RequestParam(required = false, defaultValue = "20") int limit) {

                List<Map<String, Object>> activities = dashboardService.getRecentActivities(limit);

                return ResponseEntity.ok(Map.of(
                                "success", true,
                                "message", "Lấy hoạt động gần đây thành công",
                                "data", activities,
                                "count", activities.size()));
        }

        @GetMapping("/tables/statistics")
        @PreAuthorize("hasAnyRole('ADMIN','STAFF')") // SỬA
        public ResponseEntity<?> getTableStatistics() {

                Map<String, Object> tableStats = dashboardService.getTableStatistics();

                return ResponseEntity.ok(Map.of(
                                "success", true,
                                "message", "Lấy thống kê bàn thành công",
                                "data", tableStats));
        }

        @GetMapping("/revenue/daily")
        @PreAuthorize("hasAnyRole('ADMIN','STAFF')") // SỬA
        public ResponseEntity<?> getDailyRevenue(
                        @RequestParam(required = false) LocalDate date) {

                Map<String, Object> dailyRevenue = dashboardService.getDailyRevenue(date);

                return ResponseEntity.ok(Map.of(
                                "success", true,
                                "message", "Lấy báo cáo doanh thu hàng ngày thành công",
                                "data", dailyRevenue));
        }

        @GetMapping("/orders/statistics")
        @PreAuthorize("hasAnyRole('ADMIN','STAFF')") // SỬA
        public ResponseEntity<?> getOrderStatistics(
                        @RequestParam(required = false, defaultValue = "week") String timeRange) {

                Map<String, Object> orderStats = dashboardService.getOrderStatistics(timeRange);

                return ResponseEntity.ok(Map.of(
                                "success", true,
                                "message", "Lấy thống kê đơn hàng thành công",
                                "data", orderStats));
        }
}