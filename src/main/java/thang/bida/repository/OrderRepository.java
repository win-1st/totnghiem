package thang.bida.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import thang.bida.model.Order;
import thang.bida.model.OrderItem;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface OrderRepository extends JpaRepository<Order, Long> {
        List<Order> findByStatus(Order.OrderStatus status);

        @Query("SELECT DISTINCT o FROM Order o " +
                        "LEFT JOIN FETCH o.items i " +
                        "LEFT JOIN FETCH i.product p " +
                        "LEFT JOIN FETCH p.category " +
                        "LEFT JOIN FETCH o.timeBasedProduct tp " +
                        "WHERE o.table.id = :tableId")
        List<Order> findByTableIdWithItems(@Param("tableId") Long tableId);

        // Vẫn giữ method cũ cho các trường hợp khác
        @Query("SELECT o FROM Order o WHERE o.table.id = :tableId")
        List<Order> findByTableId(@Param("tableId") Long tableId);

        List<Order> findByEmployeeId(Long employeeId);

        @Query("SELECT o FROM Order o WHERE o.createdAt BETWEEN :startDate AND :endDate")
        List<Order> findByCreatedAtBetween(LocalDateTime startDate, LocalDateTime endDate);

        List<Order> findByTableIdAndStatus(Long tableId, Order.OrderStatus status);

        // Thêm phương thức này
        List<Order> findByTableIdAndStatusNot(Long tableId, Order.OrderStatus status);

        @Query("""
                            SELECT DISTINCT o FROM Order o LEFT JOIN FETCH o.items
                            WHERE o.table.id = :tableId
                              AND o.status IN ('OPEN', 'WAITING_PAYMENT')
                            ORDER BY o.createdAt DESC
                        """)
        List<Order> findActiveOrderByTable(@Param("tableId") Long tableId);

        // ✅ Thêm method để lấy order với items
        @Query("SELECT DISTINCT o FROM Order o " +
                        "LEFT JOIN FETCH o.items i " +
                        "LEFT JOIN FETCH i.product p " +
                        "LEFT JOIN FETCH p.category " +
                        "LEFT JOIN FETCH o.timeBasedProduct tp " +
                        "LEFT JOIN FETCH o.table t " + // 🆕
                        "LEFT JOIN FETCH o.customer c " + // 🆕
                        "WHERE o.id = :orderId")
        Optional<Order> findByIdWithItems(@Param("orderId") Long orderId);

        Long countByCreatedAtAfter(LocalDateTime date);

        Long countByCreatedAtBetween(LocalDateTime start, LocalDateTime end);

        @Query("SELECT DATE(o.createdAt) as date, COUNT(o) as count " +
                        "FROM Order o WHERE o.createdAt BETWEEN :startDate AND :endDate " +
                        "GROUP BY DATE(o.createdAt) ORDER BY DATE(o.createdAt)")
        List<Object[]> getDailyOrdersBetween(@Param("startDate") LocalDateTime startDate,
                        @Param("endDate") LocalDateTime endDate);

        List<Order> findTop10ByCreatedAtAfterOrderByCreatedAtDesc(LocalDateTime cutoff);

        @Query("SELECT COUNT(o) FROM Order o WHERE o.status = :status AND o.createdAt BETWEEN :startDate AND :endDate")
        Long countOrdersByStatusBetween(@Param("status") Order.OrderStatus status,
                        @Param("startDate") LocalDateTime startDate,
                        @Param("endDate") LocalDateTime endDate);

        Long countByStatusAndCreatedAtBetween(Order.OrderStatus status, LocalDateTime start, LocalDateTime end);

        @Query("SELECT oi FROM OrderItem oi WHERE oi.order.id = :orderId AND oi.product.id = :productId")
        Optional<OrderItem> findByOrderIdAndProductId(@Param("orderId") Long orderId,
                        @Param("productId") Long productId);

}