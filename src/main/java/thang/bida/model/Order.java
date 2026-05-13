package thang.bida.model;

import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "orders", indexes = {
        @Index(name = "idx_order_status", columnList = "status"),
        @Index(name = "idx_order_table", columnList = "table_id"),
        @Index(name = "idx_order_customer", columnList = "customer_id"),
        @Index(name = "idx_order_employee", columnList = "employee_id")
})
public class Order {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "table_id")
    private BidaTable table;

    @ManyToOne
    @JoinColumn(name = "customer_id")
    private User customer;

    @ManyToOne
    @JoinColumn(name = "employee_id")
    private User employee; // ✅ Đã sửa thành User

    @Enumerated(EnumType.STRING)
    @Column(name = "status")
    private OrderStatus status;

    @Column(name = "total_amount", precision = 10, scale = 2)
    private BigDecimal totalAmount;

    @ManyToOne
    @JoinColumn(name = "promotion_id")
    private Promotion promotion;

    @Column(name = "notes", columnDefinition = "TEXT")
    private String notes;

    @Column(name = "start_time")
    private LocalDateTime startTime;

    @Column(name = "end_time")
    private LocalDateTime endTime;

    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<OrderItem> items = new ArrayList<>();

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    public enum OrderStatus {
        OPEN,
        WAITING_PAYMENT,
        PAID,
        CANCELLED
    }

    /* ================= CONSTRUCTORS ================= */
    public Order() {
        // constructor rỗng cho JPA
    }

    // ✅ CÁCH 1: Constructor nhận User employee
    public Order(BidaTable table, User employee) {
        this.table = table;
        this.employee = employee;
        this.status = OrderStatus.OPEN;
        this.startTime = LocalDateTime.now();
        this.totalAmount = BigDecimal.ZERO;
    }

    // ✅ CÁCH 2: Constructor đầy đủ (có cả customer)
    public Order(BidaTable table, User customer, User employee) {
        this.table = table;
        this.customer = customer;
        this.employee = employee;
        this.status = OrderStatus.OPEN;
        this.startTime = LocalDateTime.now();
        this.totalAmount = BigDecimal.ZERO;
    }

    // ✅ CÁCH 3: Constructor chỉ có employee (nếu cần)
    public Order(User employee) {
        this.employee = employee;
        this.status = OrderStatus.OPEN;
        this.startTime = LocalDateTime.now();
        this.totalAmount = BigDecimal.ZERO;
    }

    // ===== Các phương thức tiện ích =====

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
        this.totalAmount = items.stream()
                .map(OrderItem::getSubtotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    // Tính số giờ chơi (nếu có)
    public double getHoursPlayed() {
        if (startTime == null || endTime == null) {
            return 0;
        }
        java.time.Duration duration = java.time.Duration.between(startTime, endTime);
        return duration.toMinutes() / 60.0;
    }

    // Tính tiền bàn (nếu có hourlyRate)
    public BigDecimal getTableFee() {
        if (table == null || table.getHourlyRate() == null) {
            return BigDecimal.ZERO;
        }
        double hours = getHoursPlayed();
        return table.getHourlyRate().multiply(BigDecimal.valueOf(hours));
    }

    public LocalDateTime getStartTime() {
        return startTime;
    }

    public void setStartTime(LocalDateTime startTime) {
        this.startTime = startTime;
    }

    public LocalDateTime getEndTime() {
        return endTime;
    }

    public void setEndTime(LocalDateTime endTime) {
        this.endTime = endTime;
    }

    public List<OrderItem> getItems() {
        return items;
    }

    public void setItems(List<OrderItem> items) {
        this.items = items;
    }
}