package thang.bida.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Setter
@Entity
@Table(name = "game_sessions", indexes = {
        @Index(name = "idx_session_status", columnList = "status"),
        @Index(name = "idx_session_table", columnList = "table_id"),
        @Index(name = "idx_session_start_time", columnList = "start_time")
})
public class GameSession {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "table_id", nullable = false)
    private BidaTable table;

    @ManyToOne
    @JoinColumn(name = "customer_id")
    private User customer;

    @ManyToOne
    @JoinColumn(name = "staff_id")
    private User staff;

    @Column(name = "start_time", nullable = false)
    private LocalDateTime startTime;

    @Column(name = "end_time")
    private LocalDateTime endTime;

    @Column(name = "total_hours")
    private Double totalHours;

    @Column(name = "table_fee", precision = 10, scale = 2)
    private BigDecimal tableFee;

    @OneToOne
    @JoinColumn(name = "order_id")
    private Order order;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private SessionStatus status;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    public enum SessionStatus {
        ACTIVE, COMPLETED, CANCELLED
    }

    public GameSession() {
    }

    public GameSession(BidaTable table, User customer, User staff) {
        this.table = table;
        this.customer = customer;
        this.staff = staff;
        this.startTime = LocalDateTime.now();
        this.status = SessionStatus.ACTIVE;
    }
}