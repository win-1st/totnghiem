package thang.bida.services;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import thang.bida.model.Order;
import thang.bida.model.OrderItem;
import thang.bida.model.Product;
import thang.bida.repository.OrderRepository;
import thang.bida.repository.ProductRepository;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@Service
@Transactional
public class TimeBasedBillingService {

    private final ProductRepository productRepository;
    private final OrderRepository orderRepository;

    public TimeBasedBillingService(ProductRepository productRepository, OrderRepository orderRepository) {
        this.productRepository = productRepository;
        this.orderRepository = orderRepository;
    }

    // Lấy sản phẩm tính giờ - SỬA: dùng findFirstByProductTypeCode
    public Product getTimeBasedProduct() {
        Optional<Product> productOpt = productRepository.findFirstByProductTypeCode("TIME_BASED");
        return productOpt.orElseThrow(() -> new RuntimeException(
                "Chưa cấu hình sản phẩm tính giờ! Vui lòng tạo sản phẩm với product_type = 'TIME_BASED'"));
    }

    // Lấy sản phẩm tính giờ đang hoạt động
    public Product getActiveTimeBasedProduct() {
        Optional<Product> productOpt = productRepository.findActiveTimeBasedProduct();
        return productOpt.orElseThrow(() -> new RuntimeException("Không có sản phẩm tính giờ nào đang hoạt động!"));
    }

    // Kiểm tra đã có sản phẩm tính giờ chưa - SỬA: dùng existsByProductTypeCode
    public boolean hasTimeBasedProduct() {
        return productRepository.existsByProductTypeCode("TIME_BASED");
    }

    // Tính số phút đã sử dụng
    public long getUsedMinutes(Order order) {
        if (order.getStartTime() == null) {
            return 0;
        }
        LocalDateTime endTime = order.getEndTime() != null ? order.getEndTime() : LocalDateTime.now();
        long minutes = Duration.between(order.getStartTime(), endTime).toMinutes();
        return Math.max(minutes, 1);
    }

    // Tính số giờ đã sử dụng (làm tròn 2 số thập phân)
    public double getUsedHours(Order order) {
        long minutes = getUsedMinutes(order);
        double hours = minutes / 60.0;
        return Math.round(hours * 100) / 100.0;
    }

    // Tính tiền bàn dựa trên thời gian
    public BigDecimal calculateTableFee(Order order) {
        if (order.getStartTime() == null) {
            return BigDecimal.ZERO;
        }

        Product timeProduct = getTimeBasedProduct();
        BigDecimal pricePerMinute = timeProduct.getPricePerMinute();

        if (pricePerMinute == null) {
            pricePerMinute = new BigDecimal("666");
        }

        long minutes = getUsedMinutes(order);
        return pricePerMinute.multiply(BigDecimal.valueOf(minutes));
    }

    // Cập nhật tiền bàn cho order item
    public void updateTableFee(Order order) {
        if (order.getStartTime() == null) {
            return;
        }

        BigDecimal tableFee = calculateTableFee(order);
        Product timeProduct = getTimeBasedProduct();

        OrderItem timeItem = order.getItems().stream()
                .filter(item -> item.getProduct().isTimeBased())
                .findFirst()
                .orElse(null);

        if (timeItem != null) {
            timeItem.setSubtotal(tableFee);
            timeItem.setPrice(timeProduct.getPrice());
            orderRepository.save(order);
        }
    }

    // Tính tiền hiển thị dạng text
    public String getTableFeeDisplay(Order order) {
        if (order.getStartTime() == null) {
            return "Chưa bắt đầu";
        }

        long minutes = getUsedMinutes(order);
        BigDecimal fee = calculateTableFee(order);
        Product timeProduct = getTimeBasedProduct();

        return String.format("%d phút × %sđ/phút = %sđ",
                minutes,
                timeProduct.getPricePerMinute().toString(),
                fee.toString());
    }

    // Lấy thông tin chi tiết thanh toán
    public Map<String, Object> getBillingDetails(Order order) {
        Map<String, Object> details = new HashMap<>();

        if (order.getStartTime() == null) {
            details.put("hasStarted", false);
            return details;
        }

        Product timeProduct = getTimeBasedProduct();
        long minutes = getUsedMinutes(order);
        double hours = getUsedHours(order);
        BigDecimal pricePerMinute = timeProduct.getPricePerMinute();
        BigDecimal tableFee = calculateTableFee(order);

        details.put("hasStarted", true);
        details.put("startTime", order.getStartTime());
        details.put("endTime", order.getEndTime());
        details.put("minutesUsed", minutes);
        details.put("hoursUsed", hours);
        details.put("pricePerMinute", pricePerMinute);
        details.put("pricePerHour", pricePerMinute.multiply(BigDecimal.valueOf(60)));
        details.put("tableFee", tableFee);
        details.put("displayText", String.format("%d phút × %sđ/phút = %sđ", minutes, pricePerMinute, tableFee));

        return details;
    }

    // Tạo hoặc cập nhật sản phẩm TIME_BASED
    public Product createOrUpdateTimeBasedProduct(BigDecimal pricePerMinute, String name, String description) {
        // SỬA: dùng findFirstByProductTypeCode
        Optional<Product> existingOpt = productRepository.findFirstByProductTypeCode("TIME_BASED");

        Product timeProduct;
        if (existingOpt.isPresent()) {
            timeProduct = existingOpt.get();
            timeProduct.setPricePerMinute(pricePerMinute);
            if (name != null && !name.isEmpty()) {
                timeProduct.setName(name);
            }
            if (description != null && !description.isEmpty()) {
                timeProduct.setDescription(description);
            }
        } else {
            timeProduct = new Product();
            timeProduct.setProductTypeCode("TIME_BASED");
            timeProduct.setPricePerMinute(pricePerMinute);
            timeProduct.setName(name != null ? name : "Tiền giờ bàn Billiards");
            timeProduct.setDescription(description != null ? description : "Tính tiền theo thời gian sử dụng bàn");
            timeProduct.setPrice(BigDecimal.ZERO);
            timeProduct.setStockQuantity(0);
            timeProduct.setActive(true);
        }

        return productRepository.save(timeProduct);
    }
}