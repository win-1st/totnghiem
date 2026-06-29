package thang.bida.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "customer_points", indexes = {
        @Index(name = "idx_customer_phone", columnList = "phone"),
        @Index(name = "idx_customer_name", columnList = "customer_name")
})
public class CustomerPoint {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "phone", length = 15, nullable = false, unique = true)
    private String phone;

    @Column(name = "customer_name", length = 100)
    private String customerName;

    @Column(name = "total_points")
    private Integer totalPoints = 0;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
        if (totalPoints == null)
            totalPoints = 0;
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    // Constructor 1: SĐT và tên
    public CustomerPoint(String phone, String customerName) {
        this.phone = phone;
        this.customerName = customerName;
        this.totalPoints = 0;
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    // Constructor 2: SĐT, tên và điểm ban đầu
    public CustomerPoint(String phone, String customerName, Integer totalPoints) {
        this.phone = phone;
        this.customerName = customerName;
        this.totalPoints = totalPoints != null ? totalPoints : 0;
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    // Phương thức cộng điểm
    public void addPoints(int points) {
        this.totalPoints = (this.totalPoints == null ? 0 : this.totalPoints) + points;
        this.updatedAt = LocalDateTime.now();
    }

    // Phương thức trừ điểm (đổi quà)
    public boolean redeemPoints(int points) {
        if (this.totalPoints != null && this.totalPoints >= points) {
            this.totalPoints -= points;
            this.updatedAt = LocalDateTime.now();
            return true;
        }
        return false;
    }
}