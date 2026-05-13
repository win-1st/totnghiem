package thang.bida.model;

import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "tables", indexes = {
        @Index(name = "idx_table_status", columnList = "status"),
        @Index(name = "idx_table_type", columnList = "type")
})
public class BidaTable {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "table_name", nullable = false)
    private String tableName;

    @Column(name = "number", unique = true)
    private Integer number;

    @Column(name = "capacity")
    private Integer capacity;

    @Column(name = "type")
    private String type; // STANDARD, PREMIUM, VIP

    @Column(name = "hourly_rate", precision = 10, scale = 2)
    private java.math.BigDecimal hourlyRate;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private TableStatus status;

    @Version
    @Column(name = "version", nullable = false)
    private Integer version = 0;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    public enum TableStatus {
        FREE, OCCUPIED, RESERVED, MAINTENANCE
    }

    public BidaTable() {
    }

    public BidaTable(String tableName, Integer number, Integer capacity) {
        this.tableName = tableName;
        this.number = number;
        this.capacity = capacity;
        this.status = TableStatus.FREE;
        this.version = 0;
    }
}