package thang.bida.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import thang.bida.model.BidaTable;

import java.util.List;

@Repository
public interface BidaTableRepository extends JpaRepository<BidaTable, Long> {
    List<BidaTable> findByStatus(BidaTable.TableStatus status);

    BidaTable findByNumber(Integer number);

    List<BidaTable> findByCapacityGreaterThanEqual(Integer capacity);

    Long countByStatus(BidaTable.TableStatus status);
}