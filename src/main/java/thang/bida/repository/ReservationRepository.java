package thang.bida.repository;

import thang.bida.model.Reservation;
import thang.bida.model.Reservation.ReservationStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface ReservationRepository extends JpaRepository<Reservation, Long> {

        List<Reservation> findByStatus(ReservationStatus status);

        List<Reservation> findByCustomerPhone(String phone);

        List<Reservation> findByCustomerNameContainingIgnoreCase(String name);

        List<Reservation> findByReservationDate(LocalDate date);

        List<Reservation> findByReservationDateBetween(LocalDate startDate, LocalDate endDate);

        @Query("SELECT r FROM Reservation r WHERE r.table.id = :tableId AND r.reservationDate = :date AND r.status NOT IN ('CANCELLED', 'COMPLETED')")
        List<Reservation> findActiveReservationsByTableAndDate(@Param("tableId") Long tableId,
                        @Param("date") LocalDate date);

        @Query("SELECT r FROM Reservation r WHERE r.reservationDate = :date AND r.reservationTime BETWEEN :startTime AND :endTime")
        List<Reservation> findByDateAndTimeRange(@Param("date") LocalDate date,
                        @Param("startTime") java.time.LocalTime startTime,
                        @Param("endTime") java.time.LocalTime endTime);

        @Query("SELECT COUNT(r) FROM Reservation r WHERE r.table.id = :tableId AND r.reservationDate = :date AND r.status NOT IN ('CANCELLED', 'COMPLETED')")
        Long countActiveReservationsByTableAndDate(@Param("tableId") Long tableId, @Param("date") LocalDate date);

        List<Reservation> findByCreatedAtAfter(LocalDateTime cutoff);

        Optional<Reservation> findByIdAndCustomerPhone(Long id, String customerPhone);

        // Lấy các đặt bàn sắp tới
        @Query("SELECT r FROM Reservation r WHERE r.reservationDate >= CURRENT_DATE AND r.status IN ('PENDING', 'CONFIRMED') ORDER BY r.reservationDate ASC, r.reservationTime ASC")
        List<Reservation> findUpcomingReservations();

        // Lấy các đặt bàn cho hôm nay
        @Query("SELECT r FROM Reservation r WHERE r.reservationDate = CURRENT_DATE ORDER BY r.reservationTime ASC")
        List<Reservation> findTodaysReservations();

        List<Reservation> findByCustomerIdOrderByReservationDateDescReservationTimeDesc(Long customerId);
}