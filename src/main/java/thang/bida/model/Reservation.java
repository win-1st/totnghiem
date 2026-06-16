package thang.bida.model;

import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.time.LocalDateTime;
import java.time.LocalDate;
import java.time.LocalTime;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "reservations", indexes = {
        @Index(name = "idx_reservation_table", columnList = "table_id"),
        @Index(name = "idx_reservation_customer", columnList = "customer_id"),
        @Index(name = "idx_reservation_date", columnList = "reservation_date"),
        @Index(name = "idx_reservation_status", columnList = "status")
})
@JsonIgnoreProperties({ "hibernateLazyInitializer", "handler" })
public class Reservation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "table_id", nullable = false)
    private BidaTable table;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "customer_id")
    private User customer;

    @Column(name = "customer_name", length = 100, nullable = false)
    private String customerName;

    @Column(name = "customer_phone", length = 15, nullable = false)
    private String customerPhone;

    @Column(name = "customer_email", length = 100)
    private String customerEmail;

    @Column(name = "number_of_guests")
    private Integer numberOfGuests = 4;

    @Column(name = "reservation_date", nullable = false)
    private LocalDate reservationDate;

    @Column(name = "reservation_time", nullable = false)
    private LocalTime reservationTime;

    @Column(name = "end_time")
    private LocalTime endTime;

    @Column(name = "notes", columnDefinition = "TEXT")
    private String notes;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private ReservationStatus status = ReservationStatus.PENDING;

    @Column(name = "deposit_amount", precision = 10, scale = 2)
    private java.math.BigDecimal depositAmount = java.math.BigDecimal.ZERO;

    @Column(name = "is_deposit_paid")
    private Boolean isDepositPaid = false;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Column(name = "cancelled_at")
    private LocalDateTime cancelledAt;

    @Column(name = "cancellation_reason", columnDefinition = "TEXT")
    private String cancellationReason;

    public enum ReservationStatus {
        PENDING, // Chờ xác nhận
        CONFIRMED, // Đã xác nhận
        CHECKED_IN, // Đã đến
        CANCELLED, // Đã hủy
        COMPLETED, // Đã hoàn thành
        NO_SHOW // Khách không đến
    }

    // Constructors
    public Reservation(BidaTable table, String customerName, String customerPhone,
            LocalDate reservationDate, LocalTime reservationTime, Integer numberOfGuests) {
        this.table = table;
        this.customerName = customerName;
        this.customerPhone = customerPhone;
        this.reservationDate = reservationDate;
        this.reservationTime = reservationTime;
        this.numberOfGuests = numberOfGuests;
        this.status = ReservationStatus.PENDING;
    }

    // Phương thức xác nhận đặt bàn
    public void confirm() {
        this.status = ReservationStatus.CONFIRMED;
    }

    // Phương thức hủy đặt bàn
    public void cancel(String reason) {
        this.status = ReservationStatus.CANCELLED;
        this.cancelledAt = LocalDateTime.now();
        this.cancellationReason = reason;
    }

    // Phương thức khách đã đến
    public void checkIn() {
        this.status = ReservationStatus.CHECKED_IN;
    }

    // Phương thức hoàn thành
    public void complete() {
        this.status = ReservationStatus.COMPLETED;
    }

    // Phương thức no-show
    public void markAsNoShow() {
        this.status = ReservationStatus.NO_SHOW;
    }

    // Kiểm tra có thể chỉnh sửa không (trước 1 giờ)
    public boolean isModifiable() {
        LocalDateTime reservationDateTime = LocalDateTime.of(reservationDate, reservationTime);
        return LocalDateTime.now().plusHours(1).isBefore(reservationDateTime);
    }
}