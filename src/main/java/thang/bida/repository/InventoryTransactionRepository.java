package thang.bida.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import thang.bida.model.InventoryTransaction;
import java.util.List;

@Repository
public interface InventoryTransactionRepository extends JpaRepository<InventoryTransaction, Long> {

    List<InventoryTransaction> findByProductId(Long productId);

    List<InventoryTransaction> findByProductIdOrderByCreatedAtDesc(Long productId);

    List<InventoryTransaction> findByTransactionType(InventoryTransaction.TransactionType transactionType);

    List<InventoryTransaction> findByUserId(Long userId);
}