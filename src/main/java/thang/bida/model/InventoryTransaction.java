package thang.bida.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.time.LocalDateTime;

@Getter
@Setter
@Entity
@Table(name = "inventory_transactions", indexes = {
        @Index(name = "idx_inventory_product", columnList = "product_id"),
        @Index(name = "idx_inventory_type", columnList = "transaction_type"),
        @Index(name = "idx_inventory_created", columnList = "created_at")
})
public class InventoryTransaction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Sản phẩm
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false)
    @JsonIgnoreProperties({ "orderItems", "inventoryTransactions", "category", "productType" })
    private Product product;

    // Người thực hiện
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(name = "transaction_type", nullable = false)
    private TransactionType transactionType;

    // Số lượng thay đổi
    @Column(name = "quantity", nullable = false)
    private Integer quantity;

    // Tồn kho trước khi thay đổi
    @Column(name = "before_quantity")
    private Integer beforeQuantity;

    // Tồn kho sau khi thay đổi
    @Column(name = "after_quantity")
    private Integer afterQuantity;

    @Column(name = "note", columnDefinition = "TEXT")
    private String note;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    public enum TransactionType {
        IMPORT, // Nhập kho
        EXPORT, // Xuất kho
        ADJUSTMENT // Điều chỉnh tồn kho
    }

    public InventoryTransaction() {
    }

    public InventoryTransaction(
            Product product,
            User user,
            TransactionType transactionType,
            Integer quantity,
            Integer beforeQuantity,
            Integer afterQuantity,
            String note) {

        this.product = product;
        this.user = user;
        this.transactionType = transactionType;
        this.quantity = quantity;
        this.beforeQuantity = beforeQuantity;
        this.afterQuantity = afterQuantity;
        this.note = note;
    }
}