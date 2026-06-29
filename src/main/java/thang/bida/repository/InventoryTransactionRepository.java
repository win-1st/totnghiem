package thang.bida.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import thang.bida.model.InventoryTransaction;
import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface InventoryTransactionRepository extends JpaRepository<InventoryTransaction, Long> {

    // ========== CÁC QUERY CŨ (GIỮ NGUYÊN) ==========
    // Chỉ dùng khi thực sự cần, không dùng cho hiển thị

    List<InventoryTransaction> findByProductId(Long productId);

    List<InventoryTransaction> findByProductIdOrderByCreatedAtDesc(Long productId);

    List<InventoryTransaction> findByTransactionType(InventoryTransaction.TransactionType transactionType);

    List<InventoryTransaction> findByUserId(Long userId);

    // ========== QUERY MỚI - JOIN FETCH (FIX N+1) ==========

    /**
     * Lấy transactions theo product với JOIN FETCH
     * ✅ 1 query lấy tất cả dữ liệu cần
     */
    @Query("SELECT it FROM InventoryTransaction it " +
            "LEFT JOIN FETCH it.product p " +
            "LEFT JOIN FETCH p.category " + // Lấy luôn category của product
            "LEFT JOIN FETCH p.productType " + // Lấy luôn product type
            "LEFT JOIN FETCH it.user u " + // Lấy luôn user
            "WHERE it.product.id = :productId " +
            "ORDER BY it.createdAt DESC")
    List<InventoryTransaction> findByProductIdWithDetails(@Param("productId") Long productId);

    /**
     * Lấy tất cả transactions với JOIN FETCH
     * ✅ 1 query duy nhất cho tất cả
     */
    @Query("SELECT it FROM InventoryTransaction it " +
            "LEFT JOIN FETCH it.product p " +
            "LEFT JOIN FETCH p.category " +
            "LEFT JOIN FETCH p.productType " +
            "LEFT JOIN FETCH it.user u " +
            "ORDER BY it.createdAt DESC")
    List<InventoryTransaction> findAllWithDetails();

    /**
     * Lấy transactions theo type với JOIN FETCH
     */
    @Query("SELECT it FROM InventoryTransaction it " +
            "LEFT JOIN FETCH it.product p " +
            "LEFT JOIN FETCH p.category " +
            "LEFT JOIN FETCH it.user u " +
            "WHERE it.transactionType = :type " +
            "ORDER BY it.createdAt DESC")
    List<InventoryTransaction> findByTransactionTypeWithDetails(
            @Param("type") InventoryTransaction.TransactionType type);

    /**
     * Lấy transactions theo user với JOIN FETCH
     */
    @Query("SELECT it FROM InventoryTransaction it " +
            "LEFT JOIN FETCH it.product p " +
            "LEFT JOIN FETCH p.category " +
            "LEFT JOIN FETCH it.user u " +
            "WHERE it.user.id = :userId " +
            "ORDER BY it.createdAt DESC")
    List<InventoryTransaction> findByUserIdWithDetails(@Param("userId") Long userId);

    /**
     * Lấy transactions trong khoảng thời gian với JOIN FETCH
     */
    @Query("SELECT it FROM InventoryTransaction it " +
            "LEFT JOIN FETCH it.product p " +
            "LEFT JOIN FETCH p.category " +
            "LEFT JOIN FETCH it.user u " +
            "WHERE it.createdAt BETWEEN :startDate AND :endDate " +
            "ORDER BY it.createdAt DESC")
    List<InventoryTransaction> findByDateRangeWithDetails(
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate);

    /**
     * Lấy transactions theo product và type với JOIN FETCH
     */
    @Query("SELECT it FROM InventoryTransaction it " +
            "LEFT JOIN FETCH it.product p " +
            "LEFT JOIN FETCH p.category " +
            "LEFT JOIN FETCH it.user u " +
            "WHERE it.product.id = :productId " +
            "AND it.transactionType = :type " +
            "ORDER BY it.createdAt DESC")
    List<InventoryTransaction> findByProductIdAndTypeWithDetails(
            @Param("productId") Long productId,
            @Param("type") InventoryTransaction.TransactionType type);

    /**
     * Lấy transactions với phân trang
     * ✅ Có thể dùng pageable để giới hạn số lượng
     */
    @Query("SELECT it FROM InventoryTransaction it " +
            "LEFT JOIN FETCH it.product p " +
            "LEFT JOIN FETCH p.category " +
            "LEFT JOIN FETCH it.user u " +
            "ORDER BY it.createdAt DESC")
    List<InventoryTransaction> findAllWithDetailsPageable(
            org.springframework.data.domain.Pageable pageable);

    // ========== THỐNG KÊ ==========

    /**
     * Đếm số transactions theo product
     */
    long countByProductId(Long productId);

    /**
     * Tính tổng số lượng nhập/xuất theo product
     */
    @Query("SELECT COALESCE(SUM(it.quantity), 0) FROM InventoryTransaction it " +
            "WHERE it.product.id = :productId AND it.transactionType = :type")
    Integer sumQuantityByProductIdAndType(
            @Param("productId") Long productId,
            @Param("type") InventoryTransaction.TransactionType type);

    /**
     * Lấy số lượng tồn kho hiện tại của product (dùng transaction gần nhất)
     */
    @Query("SELECT it.afterQuantity FROM InventoryTransaction it " +
            "WHERE it.product.id = :productId " +
            "ORDER BY it.createdAt DESC LIMIT 1")
    Integer getCurrentStockByProductId(@Param("productId") Long productId);
}
