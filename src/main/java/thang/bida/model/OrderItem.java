package thang.bida.model;

import jakarta.persistence.*;

import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import com.fasterxml.jackson.annotation.JsonBackReference;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "order_items")
@JsonIgnoreProperties({ "hibernateLazyInitializer", "handler" })
public class OrderItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // =========================
    // RELATIONSHIP
    // =========================

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id")
    @JsonBackReference
    private Order order;

    @ManyToOne(fetch = FetchType.LAZY) // ← ĐỔI TỪ EAGER thành LAZY
    @JoinColumn(name = "product_id")
    @JsonIgnoreProperties({ "orderItems", "category", "productType" }) // ← THÊM
    private Product product;

    // =========================
    // FIELDS
    // =========================

    @Column(nullable = false)
    private Integer quantity = 1;

    @Column(name = "price", precision = 10, scale = 2)
    private BigDecimal price = BigDecimal.ZERO;

    @Column(name = "unit_price", precision = 10, scale = 2)
    private BigDecimal unitPrice = BigDecimal.ZERO;

    @Column(name = "subtotal", precision = 10, scale = 2)
    private BigDecimal subtotal = BigDecimal.ZERO;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    // =========================
    // CONSTRUCTORS
    // =========================

    public OrderItem() {
    }

    public OrderItem(
            Order order,
            Product product,
            Integer quantity,
            BigDecimal price) {

        this.order = order;
        this.product = product;
        this.quantity = quantity;
        this.price = price;
        this.unitPrice = price;

        calculateSubtotal();
    }

    // =========================
    // AUTO CALCULATE
    // =========================

    @PrePersist
    @PreUpdate
    public void calculateSubtotal() {

        if (this.quantity == null) {
            this.quantity = 1;
        }

        if (this.price == null) {
            this.price = BigDecimal.ZERO;
        }

        this.unitPrice = this.price;

        this.subtotal = this.price.multiply(
                BigDecimal.valueOf(this.quantity));
    }
}