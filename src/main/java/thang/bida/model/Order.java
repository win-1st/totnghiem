package thang.bida.model;

import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import com.fasterxml.jackson.annotation.JsonManagedReference;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import lombok.Getter;
import lombok.Setter;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@Getter
@Setter
@Entity
@Table(name = "orders", indexes = {
        @Index(name = "idx_order_status", columnList = "status"),
        @Index(name = "idx_order_table", columnList = "table_id"),
        @Index(name = "idx_order_customer", columnList = "customer_id"),
        @Index(name = "idx_order_employee", columnList = "employee_id")
})
@JsonIgnoreProperties({ "hibernateLazyInitializer", "handler" })
public class Order {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // =========================
    // RELATIONSHIP
    // =========================

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "table_id")
    private BidaTable table;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "customer_id")
    private User customer;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "employee_id")
    private User employee;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "time_based_product_id")
    private Product timeBasedProduct;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "promotion_id")
    private Promotion promotion;

    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @JsonManagedReference
    private List<OrderItem> items = new ArrayList<>();

    // =========================
    // FIELDS
    // =========================

    @Enumerated(EnumType.STRING)
    @Column(name = "status")
    private OrderStatus status;

    @Column(name = "total_amount", precision = 10, scale = 2)
    private BigDecimal totalAmount = BigDecimal.ZERO;

    @Column(name = "table_fee", precision = 10, scale = 2)
    private BigDecimal tableFee = BigDecimal.ZERO;

    @Column(name = "product_fee", precision = 10, scale = 2)
    private BigDecimal productFee = BigDecimal.ZERO;

    @Column(name = "notes", columnDefinition = "TEXT")
    private String notes;

    @Column(name = "start_time")
    private LocalDateTime startTime;

    @Column(name = "end_time")
    private LocalDateTime endTime;

    // ✅ ĐÃ DI CHUYỂN XUỐNG ĐÂY - phần fields
    @Column(name = "stock_deducted", columnDefinition = "BOOLEAN DEFAULT FALSE")
    private boolean stockDeducted = false;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    // =========================
    // ENUM
    // =========================

    public enum OrderStatus {
        OPEN,
        WAITING_PAYMENT,
        PAID,
        CANCELLED
    }

    // =========================
    // CONSTRUCTORS
    // =========================

    public Order() {
    }

    public Order(BidaTable table, User employee, Product timeBasedProduct) {
        this.table = table;
        this.employee = employee;
        this.timeBasedProduct = timeBasedProduct;

        this.status = OrderStatus.OPEN;
        this.startTime = LocalDateTime.now();

        this.totalAmount = BigDecimal.ZERO;
        this.tableFee = BigDecimal.ZERO;
        this.productFee = BigDecimal.ZERO;
        this.stockDeducted = false;
    }

    public Order(BidaTable table, User customer, User employee, Product timeBasedProduct) {
        this.table = table;
        this.customer = customer;
        this.employee = employee;
        this.timeBasedProduct = timeBasedProduct;

        this.status = OrderStatus.OPEN;
        this.startTime = LocalDateTime.now();

        this.totalAmount = BigDecimal.ZERO;
        this.tableFee = BigDecimal.ZERO;
        this.productFee = BigDecimal.ZERO;
        this.stockDeducted = false;
    }

    // =========================
    // METHODS
    // =========================

    public void addItem(OrderItem item) {
        items.add(item);
        item.setOrder(this);

        recalculateTotal();
    }

    public void removeItem(OrderItem item) {
        items.remove(item);
        item.setOrder(null);

        recalculateTotal();
    }

    public void recalculateTotal() {

        // Tiền sản phẩm
        this.productFee = items.stream()
                .map(OrderItem::getSubtotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // Tiền bàn
        this.tableFee = calculateTableFee();

        // Tổng tiền
        this.totalAmount = this.tableFee.add(this.productFee);

        // Khuyến mãi
        if (promotion != null
                && promotion.getIsActive() != null
                && promotion.getIsActive()) {

            LocalDate now = LocalDate.now();

            if (promotion.getStartDate() != null
                    && now.isBefore(promotion.getStartDate())) {
                return;
            }

            if (promotion.getEndDate() != null
                    && now.isAfter(promotion.getEndDate())) {
                return;
            }

            BigDecimal discount = BigDecimal.ZERO;

            // Giảm %
            if (promotion.getDiscountPercentage() != null
                    && promotion.getDiscountPercentage()
                            .compareTo(BigDecimal.ZERO) > 0) {

                discount = this.totalAmount
                        .multiply(promotion.getDiscountPercentage())
                        .divide(BigDecimal.valueOf(100), 2,
                                java.math.RoundingMode.HALF_UP);
            }

            // Giảm tiền
            else if (promotion.getDiscountAmount() != null
                    && promotion.getDiscountAmount()
                            .compareTo(BigDecimal.ZERO) > 0) {

                discount = promotion.getDiscountAmount();

                if (discount.compareTo(this.totalAmount) > 0) {
                    discount = this.totalAmount;
                }
            }

            this.totalAmount = this.totalAmount.subtract(discount);
        }
    }

    public long getMinutesPlayed() {
        if (startTime == null) {
            return 0;
        }

        LocalDateTime end = (endTime != null) ? endTime : LocalDateTime.now();

        if (end.isBefore(startTime)) {
            return 0;
        }

        // Tính tổng số giây
        long totalSeconds = Duration.between(startTime, end).toSeconds();
        long minutes = totalSeconds / 60;
        long seconds = totalSeconds % 60;

        System.out.println("⏱️ getMinutesPlayed: " + minutes + " phút, " + seconds + " giây");

        // Nếu có giây lẻ (seconds > 0), làm tròn lên 1 phút
        if (seconds > 0) {
            minutes = minutes + 1;
            System.out.println("⏱️ Làm tròn lên: " + minutes + " phút");
        }

        return minutes;
    }

    public BigDecimal calculateTableFee() {
        // Chỉ tính tiền bàn nếu có timeBasedProduct VÀ đã bắt đầu tính giờ
        if (timeBasedProduct == null) {
            System.out.println("❌ calculateTableFee: timeBasedProduct is null");
            return BigDecimal.ZERO;
        }
        if (timeBasedProduct.getPricePerMinute() == null) {
            System.out.println("❌ calculateTableFee: pricePerMinute is null");
            return BigDecimal.ZERO;
        }
        if (startTime == null) {
            System.out.println("❌ calculateTableFee: startTime is null");
            return BigDecimal.ZERO;
        }

        long minutes = getMinutesPlayed();
        if (minutes <= 0) {
            minutes = 1;
        }

        BigDecimal fee = timeBasedProduct.getPricePerMinute().multiply(BigDecimal.valueOf(minutes));
        System.out.println("💰 calculateTableFee: " + minutes + " phút × " +
                timeBasedProduct.getPricePerMinute() + "đ = " + fee + "đ");
        return fee;
    }

    public double getHoursPlayed() {
        return Math.round(getMinutesPlayed() / 6.0) / 10.0;
    }

    public void checkout() {
        this.endTime = LocalDateTime.now();
        this.status = OrderStatus.WAITING_PAYMENT;

        recalculateTotal();
    }

    public void markAsPaid() {
        this.status = OrderStatus.PAID;
    }

    public void cancel() {
        this.status = OrderStatus.CANCELLED;
        this.endTime = LocalDateTime.now();
    }

    public String getFormattedPlayTime() {

        long minutes = getMinutesPlayed();

        long hours = minutes / 60;
        long remainingMinutes = minutes % 60;

        if (hours > 0) {
            return String.format("%d giờ %d phút",
                    hours,
                    remainingMinutes);
        }

        return String.format("%d phút", remainingMinutes);
    }

    public String getFormattedTotalAmount() {

        if (totalAmount == null) {
            return "0 VNĐ";
        }

        return String.format("%,.0f VNĐ", totalAmount.doubleValue());
    }
}