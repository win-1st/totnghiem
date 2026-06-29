package thang.bida.repository;

import thang.bida.model.Transaction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface TransactionRepository extends JpaRepository<Transaction, Long> {
    List<Transaction> findByCustomerPointIdOrderByCreatedAtDesc(Long customerPointId);

    List<Transaction> findByCustomerPointIdAndTypeOrderByCreatedAtDesc(Long customerPointId, String type);
}