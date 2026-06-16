package thang.bida.services;

import thang.bida.dto.ReservationDTO;
import thang.bida.model.BidaTable;
import thang.bida.model.Reservation;
import thang.bida.model.User;
import thang.bida.repository.BidaTableRepository;
import thang.bida.repository.ReservationRepository;
import thang.bida.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class ReservationService {

    @Autowired
    private ReservationRepository reservationRepository;

    @Autowired
    private BidaTableRepository bidaTableRepository;

    @Autowired
    private UserRepository userRepository;

    private ReservationDTO convertToDTO(Reservation reservation) {
        ReservationDTO dto = new ReservationDTO();
        dto.setId(reservation.getId());
        dto.setTableId(reservation.getTable().getId());
        dto.setTableNumber(reservation.getTable().getNumber());
        dto.setCustomerId(reservation.getCustomer() != null ? reservation.getCustomer().getId() : null);
        dto.setCustomerName(reservation.getCustomerName());
        dto.setCustomerPhone(reservation.getCustomerPhone());
        dto.setCustomerEmail(reservation.getCustomerEmail());
        dto.setNumberOfGuests(reservation.getNumberOfGuests());
        dto.setReservationDate(reservation.getReservationDate());
        dto.setReservationTime(reservation.getReservationTime());
        dto.setEndTime(reservation.getEndTime());
        dto.setNotes(reservation.getNotes());
        dto.setStatus(reservation.getStatus().name());
        dto.setDepositAmount(reservation.getDepositAmount());
        dto.setIsDepositPaid(reservation.getIsDepositPaid());
        dto.setCreatedAt(reservation.getCreatedAt() != null ? reservation.getCreatedAt().toString() : null);
        dto.setUpdatedAt(reservation.getUpdatedAt() != null ? reservation.getUpdatedAt().toString() : null);
        return dto;
    }

    public List<ReservationDTO> getReservationsByCustomerId(Long customerId) {
        List<Reservation> reservations = reservationRepository
                .findByCustomerIdOrderByReservationDateDescReservationTimeDesc(customerId);
        return reservations.stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    // Lấy tất cả đặt bàn
    public List<ReservationDTO> getAllReservations() {
        return reservationRepository.findAll().stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    // Lấy đặt bàn theo ID
    public ReservationDTO getReservationById(Long id) {
        Reservation reservation = reservationRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy đặt bàn với ID: " + id));
        return convertToDTO(reservation);
    }

    // Lấy đặt bàn theo số điện thoại
    public List<ReservationDTO> getReservationsByPhone(String phone) {
        return reservationRepository.findByCustomerPhone(phone).stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    // Lấy đặt bàn theo ngày
    public List<ReservationDTO> getReservationsByDate(LocalDate date) {
        return reservationRepository.findByReservationDate(date).stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    // Lấy đặt bàn hôm nay
    public List<ReservationDTO> getTodaysReservations() {
        return reservationRepository.findTodaysReservations().stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    // Lấy đặt bàn sắp tới
    public List<ReservationDTO> getUpcomingReservations() {
        return reservationRepository.findUpcomingReservations().stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    // Kiểm tra bàn có trống không
    public boolean isTableAvailable(Long tableId, LocalDate date, LocalTime time, int durationHours) {
        List<Reservation> reservations = reservationRepository.findActiveReservationsByTableAndDate(tableId, date);

        LocalTime startTime = time;
        LocalTime endTime = time.plusHours(durationHours);

        for (Reservation res : reservations) {
            LocalTime resStart = res.getReservationTime();
            LocalTime resEnd = res.getEndTime() != null ? res.getEndTime() : resStart.plusHours(2);

            if (!(endTime.isBefore(resStart) || startTime.isAfter(resEnd))) {
                return false; // Bị trùng giờ
            }
        }
        return true;
    }

    // Tạo đặt bàn mới
    @Transactional
    public ReservationDTO createReservation(ReservationDTO dto, Long customerId) {
        BidaTable table = bidaTableRepository.findById(dto.getTableId())
                .orElseThrow(() -> new RuntimeException("Không tìm thấy bàn"));

        // Kiểm tra bàn có trống không
        int duration = 2; // Mặc định 2 giờ
        if (dto.getEndTime() != null) {
            duration = dto.getEndTime().getHour() - dto.getReservationTime().getHour();
        }

        if (!isTableAvailable(dto.getTableId(), dto.getReservationDate(), dto.getReservationTime(), duration)) {
            throw new RuntimeException("Bàn đã được đặt vào khung giờ này");
        }

        Reservation reservation = new Reservation();
        reservation.setTable(table);
        reservation.setCustomerName(dto.getCustomerName());
        reservation.setCustomerPhone(dto.getCustomerPhone());
        reservation.setCustomerEmail(dto.getCustomerEmail());
        reservation.setNumberOfGuests(dto.getNumberOfGuests() != null ? dto.getNumberOfGuests() : table.getCapacity());
        reservation.setReservationDate(dto.getReservationDate());
        reservation.setReservationTime(dto.getReservationTime());
        reservation.setEndTime(dto.getEndTime());
        reservation.setNotes(dto.getNotes());
        reservation.setStatus(Reservation.ReservationStatus.PENDING);
        reservation
                .setDepositAmount(dto.getDepositAmount() != null ? dto.getDepositAmount() : java.math.BigDecimal.ZERO);
        reservation.setIsDepositPaid(dto.getIsDepositPaid() != null ? dto.getIsDepositPaid() : false);

        if (customerId != null) {
            User customer = userRepository.findById(customerId).orElse(null);
            reservation.setCustomer(customer);
        }

        Reservation saved = reservationRepository.save(reservation);
        return convertToDTO(saved);
    }

    // Cập nhật đặt bàn
    @Transactional
    public ReservationDTO updateReservation(Long id, ReservationDTO dto) {
        Reservation reservation = reservationRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy đặt bàn"));

        if (!reservation.isModifiable()) {
            throw new RuntimeException("Không thể chỉnh sửa đặt bàn trong vòng 1 giờ trước giờ đặt");
        }

        if (dto.getCustomerName() != null)
            reservation.setCustomerName(dto.getCustomerName());
        if (dto.getCustomerPhone() != null)
            reservation.setCustomerPhone(dto.getCustomerPhone());
        if (dto.getCustomerEmail() != null)
            reservation.setCustomerEmail(dto.getCustomerEmail());
        if (dto.getNumberOfGuests() != null)
            reservation.setNumberOfGuests(dto.getNumberOfGuests());
        if (dto.getNotes() != null)
            reservation.setNotes(dto.getNotes());

        Reservation saved = reservationRepository.save(reservation);
        return convertToDTO(saved);
    }

    // Xác nhận đặt bàn
    @Transactional
    public ReservationDTO confirmReservation(Long id) {
        Reservation reservation = reservationRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy đặt bàn"));
        reservation.confirm();
        return convertToDTO(reservationRepository.save(reservation));
    }

    // Hủy đặt bàn
    @Transactional
    public ReservationDTO cancelReservation(Long id, String reason) {
        Reservation reservation = reservationRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy đặt bàn"));
        reservation.cancel(reason);
        return convertToDTO(reservationRepository.save(reservation));
    }

    // Check-in
    @Transactional
    public ReservationDTO checkIn(Long id) {
        Reservation reservation = reservationRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy đặt bàn"));
        reservation.checkIn();
        return convertToDTO(reservationRepository.save(reservation));
    }

    // Xóa đặt bàn
    @Transactional
    public void deleteReservation(Long id) {
        Reservation reservation = reservationRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy đặt bàn"));
        reservationRepository.delete(reservation);
    }
}