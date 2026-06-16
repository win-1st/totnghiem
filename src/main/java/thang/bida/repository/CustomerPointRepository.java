package thang.bida.repository;

import thang.bida.model.CustomerPoint;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import java.util.Optional;
import java.util.List;

@Repository
public interface CustomerPointRepository extends JpaRepository<CustomerPoint, Long> {

    Optional<CustomerPoint> findByPhone(String phone);

    boolean existsByPhone(String phone);

    List<CustomerPoint> findByCustomerNameContainingIgnoreCase(String name);

    @Modifying
    @Transactional
    @Query("UPDATE CustomerPoint c SET c.totalPoints = c.totalPoints + :points WHERE c.phone = :phone")
    int addPoints(@Param("phone") String phone, @Param("points") int points);

    @Modifying
    @Transactional
    @Query("UPDATE CustomerPoint c SET c.totalPoints = c.totalPoints - :points WHERE c.phone = :phone AND c.totalPoints >= :points")
    int redeemPoints(@Param("phone") String phone, @Param("points") int points);

    @Modifying
    @Transactional
    @Query("UPDATE CustomerPoint c SET c.totalHoursPlayed = c.totalHoursPlayed + :hours WHERE c.phone = :phone")
    int addHoursPlayed(@Param("phone") String phone, @Param("hours") int hours);
}