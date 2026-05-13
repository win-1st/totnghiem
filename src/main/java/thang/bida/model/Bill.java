package thang.bida.model;

import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "bills")
public class Bill {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne
    @JoinColumn(name = "order_id")
    private Order order;

    @Column(name = "total_amount", precision = 10, scale = 2)
    private BigDecimal totalAmount;

    @Enumerated(EnumType.STRING)
    @Column(name = "payment_method")
    private PaymentMethod paymentMethod;

    @Enumerated(EnumType.STRING)
    @Column(name = "payment_status")
    private PaymentStatus paymentStatus;

    @Column(name = "issued_at")
    private LocalDateTime issuedAt;

    @Column(name = "notes", columnDefinition = "TEXT")
    private String notes;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @ManyToOne
    @JoinColumn(name = "table_id")
    private BidaTable table;

    @ManyToOne
    @JoinColumn(name = "customer_id")
    private User customer;

    public enum PaymentMethod {
        CASH, MOMO, PAYOS
    }

    public enum PaymentStatus {
        PENDING, PAID, FAILED, CANCELLED
    }

    // Constructors
    public Bill() {
    }

    public Bill(Order order, BigDecimal totalAmount) {
        this.order = order;
        this.totalAmount = totalAmount;
        this.paymentStatus = PaymentStatus.PENDING;
        this.issuedAt = LocalDateTime.now();
    }

}