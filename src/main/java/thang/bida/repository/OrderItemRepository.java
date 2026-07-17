package thang.bida.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import thang.bida.model.OrderItem;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface OrderItemRepository extends JpaRepository<OrderItem, Long> {
    List<OrderItem> findByOrderId(Long orderId);

    Optional<OrderItem> findByOrderIdAndProductId(Long orderId, Long productId);

    @Query("SELECT oi FROM OrderItem oi WHERE oi.order.id = :orderId")
    List<OrderItem> findOrderItemsByOrderId(Long orderId);

    @Query("""
                SELECT COALESCE(SUM(oi.price * oi.quantity), 0)
                FROM OrderItem oi
                WHERE oi.order.id = :orderId
            """)
    java.math.BigDecimal getTotalAmountByOrderId(Long orderId);

    @Query("""
                SELECT oi.product.id, oi.product.name,
                       SUM(oi.quantity), SUM(oi.quantity * oi.price)
                FROM OrderItem oi
                WHERE oi.order.createdAt >= :startDate
                GROUP BY oi.product.id, oi.product.name
                ORDER BY SUM(oi.quantity) DESC
            """)
    List<Object[]> getTopProducts(
            @Param("startDate") LocalDateTime startDate,
            org.springframework.data.domain.Pageable pageable);

    // ===== THÊM METHOD MỚI =====
    @Query("""
                SELECT oi.product.id, oi.product.name,
                       SUM(oi.quantity), SUM(oi.quantity * oi.unitPrice)
                FROM OrderItem oi
                JOIN oi.order o
                WHERE o.createdAt BETWEEN :startDate AND :endDate
                  AND o.status = 'PAID'
                GROUP BY oi.product.id, oi.product.name
                ORDER BY SUM(oi.quantity * oi.unitPrice) DESC
            """)
    List<Object[]> getTopProductsBetween(
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate,
            org.springframework.data.domain.Pageable pageable);
}