package thang.bida.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import thang.bida.model.Bill;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface BillRepository extends JpaRepository<Bill, Long> {
    Optional<Bill> findByOrderId(Long orderId);

    List<Bill> findByPaymentStatus(Bill.PaymentStatus paymentStatus);

    @Query("SELECT b FROM Bill b WHERE b.issuedAt BETWEEN :startDate AND :endDate")
    List<Bill> findByIssuedAtBetween(LocalDateTime startDate, LocalDateTime endDate);

    @Query("""
                SELECT COALESCE(SUM(b.totalAmount), 0)
                FROM Bill b
                WHERE b.paymentStatus = thang.bida.model.Bill.PaymentStatus.PAID
                AND b.issuedAt BETWEEN :start AND :end
            """)
    BigDecimal getTotalRevenueByDateRange(
            @Param("start") LocalDateTime start,
            @Param("end") LocalDateTime end);

    @Query("""
            SELECT COALESCE(SUM(b.totalAmount), 0)
            FROM Bill b
            WHERE b.paymentStatus = 'PAID'
            """)
    BigDecimal getTotalRevenue();

    @Query("""
            SELECT COALESCE(SUM(b.totalAmount), 0)
            FROM Bill b
            WHERE b.paymentStatus = 'PAID'
            AND b.issuedAt >= :startDate
            """)
    BigDecimal getRevenueAfter(@Param("startDate") LocalDateTime startDate);

    @Query("""
            SELECT COALESCE(SUM(b.totalAmount), 0)
            FROM Bill b
            WHERE b.paymentStatus = 'PAID'
            AND b.issuedAt BETWEEN :startDate AND :endDate
            """)
    BigDecimal getRevenueBetween(
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate);

    @Query("""
            SELECT DATE(b.issuedAt), SUM(b.totalAmount)
            FROM Bill b
            WHERE b.paymentStatus = 'PAID'
            AND b.issuedAt BETWEEN :startDate AND :endDate
            GROUP BY DATE(b.issuedAt)
            ORDER BY DATE(b.issuedAt)
            """)
    List<Object[]> getDailyRevenueBetween(
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate);

    @Query("""
            SELECT MONTH(b.issuedAt), SUM(b.totalAmount)
            FROM Bill b
            WHERE b.paymentStatus = 'PAID'
            AND b.issuedAt BETWEEN :startDate AND :endDate
            GROUP BY MONTH(b.issuedAt)
            ORDER BY MONTH(b.issuedAt)
            """)
    List<Object[]> getMonthlyRevenue(
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate);

    @Query("""
                SELECT b.paymentMethod, SUM(b.totalAmount)
                FROM Bill b
                WHERE b.paymentStatus = 'PAID'
                AND b.issuedAt BETWEEN :startDate AND :endDate
                GROUP BY b.paymentMethod
            """)
    List<Object[]> getRevenueByPaymentMethod(
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate);

    List<Bill> findTop10ByCreatedAtAfterOrderByCreatedAtDesc(LocalDateTime cutoff);

    // Method này đã đúng (dùng 'PAID')
    @Query("SELECT COALESCE(SUM(b.totalAmount), 0) FROM Bill b " +
            "WHERE b.paymentStatus = 'PAID' AND b.createdAt BETWEEN :startDate AND :endDate")
    BigDecimal getRevenueFromPaidOrders(@Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate);
}