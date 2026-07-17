package thang.bida.services;

import thang.bida.dto.ReservationDTO;
import thang.bida.model.BidaTable;
import thang.bida.model.Reservation;
import thang.bida.model.User;
import thang.bida.repository.BidaTableRepository;
import thang.bida.repository.ReservationRepository;
import thang.bida.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
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

    @Autowired
    private WebSocketService webSocketService;

    @Autowired
    private OrderService orderService;

    // ========== CONVERT TO DTO ==========
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
        dto.setTableType(reservation.getTable().getType());
        return dto;
    }

    // ========== QUERY METHODS ==========

    // Lấy đặt bàn theo customerId
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

    // ========== CRUD METHODS ==========

    // Tạo đặt bàn mới
    @Transactional
    public ReservationDTO createReservation(ReservationDTO dto, Long customerId) {
        // Kiểm tra tableId có được gửi lên không
        if (dto.getTableId() == null) {
            // Nếu không chọn bàn, tự động tìm bàn trống
            BidaTable availableTable = findAvailableTable(dto.getReservationDate(), dto.getReservationTime(),
                    dto.getNumberOfGuests());
            if (availableTable == null) {
                throw new RuntimeException("Không có bàn trống vào khung giờ này. Vui lòng chọn giờ khác.");
            }
            dto.setTableId(availableTable.getId());
        }

        BidaTable table = bidaTableRepository.findById(dto.getTableId())
                .orElseThrow(() -> new RuntimeException("Không tìm thấy bàn"));

        // Kiểm tra bàn có trống không
        int duration = 2;
        if (dto.getEndTime() != null) {
            duration = dto.getEndTime().getHour() - dto.getReservationTime().getHour();
            if (duration <= 0)
                duration = 2;
        }

        if (!isTableAvailable(dto.getTableId(), dto.getReservationDate(), dto.getReservationTime(), duration)) {
            throw new RuntimeException("Bàn đã được đặt vào khung giờ này. Vui lòng chọn bàn khác hoặc đổi giờ.");
        }

        Reservation reservation = new Reservation();
        reservation.setTable(table);
        reservation.setCustomerName(dto.getCustomerName());
        reservation.setCustomerPhone(dto.getCustomerPhone());
        reservation.setCustomerEmail(dto.getCustomerEmail());
        reservation.setNumberOfGuests(dto.getNumberOfGuests() != null ? dto.getNumberOfGuests() : table.getCapacity());
        reservation.setReservationDate(dto.getReservationDate());
        reservation.setReservationTime(dto.getReservationTime());

        // Tính endTime mặc định (2 tiếng sau)
        if (dto.getEndTime() == null) {
            reservation.setEndTime(dto.getReservationTime().plusHours(2));
        } else {
            reservation.setEndTime(dto.getEndTime());
        }

        reservation.setNotes(dto.getNotes());
        reservation.setStatus(Reservation.ReservationStatus.PENDING);
        reservation
                .setDepositAmount(dto.getDepositAmount() != null ? dto.getDepositAmount() : java.math.BigDecimal.ZERO);
        reservation.setIsDepositPaid(dto.getIsDepositPaid() != null ? dto.getIsDepositPaid() : false);

        if (customerId != null) {
            User customer = userRepository.findById(customerId).orElse(null);
            reservation.setCustomer(customer);
            // Nếu có customer, cập nhật thông tin từ customer
            if (customer != null) {
                if (dto.getCustomerName() == null || dto.getCustomerName().isEmpty()) {
                    reservation.setCustomerName(
                            customer.getFullName() != null ? customer.getFullName() : customer.getPhone());
                }
                if (dto.getCustomerPhone() == null || dto.getCustomerPhone().isEmpty()) {
                    reservation.setCustomerPhone(customer.getPhone());
                }
                if (dto.getCustomerEmail() == null || dto.getCustomerEmail().isEmpty()) {
                    reservation.setCustomerEmail(customer.getEmail());
                }
            }
        }

        Reservation saved = reservationRepository.save(reservation);

        // ========== CẬP NHẬT TRẠNG THÁI BÀN ==========
        // ✅ CHỈ cập nhật khi đặt cho HÔM NAY và bàn đang FREE
        LocalDate today = LocalDate.now();
        LocalDate reservationDate = dto.getReservationDate();

        // Kiểm tra: Đặt cho hôm nay
        if (reservationDate.equals(today)) {
            BidaTable currentTable = bidaTableRepository.findById(dto.getTableId()).orElse(null);

            if (currentTable != null) {
                // Kiểm tra bàn đang FREE
                if (currentTable.getStatus() == BidaTable.TableStatus.FREE) {
                    // Kiểm tra không có order active
                    boolean hasActiveOrder = orderService.hasActiveOrder(currentTable.getId());
                    if (!hasActiveOrder) {
                        currentTable.setStatus(BidaTable.TableStatus.RESERVED);
                        bidaTableRepository.save(currentTable);
                        System.out.println("✅ Table " + currentTable.getNumber() + " -> RESERVED (đặt hôm nay)");
                    } else {
                        System.out.println("⚠️ Table " + currentTable.getNumber() + " đang có order, không thể đặt");
                        throw new RuntimeException("Bàn đang có khách, không thể đặt!");
                    }
                } else if (currentTable.getStatus() == BidaTable.TableStatus.OCCUPIED) {
                    System.out.println("❌ Table " + currentTable.getNumber() + " đang có khách, không thể đặt hôm nay");
                    throw new RuntimeException("Bàn đang có khách, không thể đặt cho hôm nay!");
                } else if (currentTable.getStatus() == BidaTable.TableStatus.RESERVED) {
                    System.out.println("❌ Table " + currentTable.getNumber() + " đã được đặt");
                    throw new RuntimeException("Bàn đã được đặt trước!");
                } else if (currentTable.getStatus() == BidaTable.TableStatus.MAINTENANCE) {
                    System.out.println("❌ Table " + currentTable.getNumber() + " đang bảo trì");
                    throw new RuntimeException("Bàn đang bảo trì, không thể đặt!");
                }
            }
        } else {
            // ✅ Đặt cho ngày khác (ngày mai, ngày kia...)
            // KHÔNG cập nhật trạng thái bàn
            System.out.println("📅 Đặt cho ngày " + reservationDate + " - KHÔNG cập nhật bàn");
        }

        // Gửi WebSocket notification
        sendTableStatusNotification(saved, "RESERVED");

        return convertToDTO(saved);
    }

    // Tìm bàn trống tự động
    private BidaTable findAvailableTable(LocalDate date, LocalTime time, Integer numberOfGuests) {
        List<BidaTable> allTables = bidaTableRepository.findAll();

        for (BidaTable table : allTables) {
            // Kiểm tra sức chứa
            if (numberOfGuests != null && table.getCapacity() < numberOfGuests) {
                continue;
            }
            // Kiểm tra bàn có trống không
            if (isTableAvailable(table.getId(), date, time, 2)) {
                return table;
            }
        }
        return null;
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

    // ========== XÁC NHẬN ĐẶT BÀN ==========
    @Transactional
    public ReservationDTO confirmReservation(Long id) {
        System.out.println("=== CONFIRM RESERVATION ===");
        System.out.println("Reservation ID: " + id);

        Reservation reservation = reservationRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy đặt bàn"));

        System.out.println("Current status: " + reservation.getStatus());

        // ✅ CHỈ CẬP NHẬT RESERVATION, KHÔNG CẬP NHẬT BÀN
        reservation.confirm();
        Reservation saved = reservationRepository.save(reservation);

        System.out.println("✅ Reservation " + id + " status -> CONFIRMED");
        System.out.println("Table " + reservation.getTable().getNumber() + " status remains "
                + reservation.getTable().getStatus());

        return convertToDTO(saved);
    }

    // ========== HỦY ĐẶT BÀN ==========
    @Transactional
    public ReservationDTO cancelReservation(Long id, String reason) {
        Reservation reservation = reservationRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy đặt bàn"));

        reservation.cancel(reason);
        Reservation saved = reservationRepository.save(reservation);

        // ========== CẬP NHẬT TRẠNG THÁI BÀN KHI HỦY ==========
        // ✅ CHỈ cập nhật nếu đặt cho HÔM NAY và bàn đang RESERVED
        LocalDate today = LocalDate.now();
        LocalDate reservationDate = reservation.getReservationDate();

        if (reservationDate.equals(today)) {
            BidaTable table = reservation.getTable();
            if (table.getStatus() == BidaTable.TableStatus.RESERVED) {
                table.setStatus(BidaTable.TableStatus.FREE);
                bidaTableRepository.save(table);
                System.out.println("✅ Table " + table.getNumber() + " -> FREE (hủy đặt hôm nay)");
            }
        } else {
            System.out.println("📅 Hủy đặt cho ngày " + reservationDate + " - KHÔNG cập nhật bàn");
        }

        // Gửi WebSocket notification
        sendTableStatusNotification(saved, "FREE");

        return convertToDTO(saved);
    }

    // ========== CHECK-IN ==========
    @Transactional
    public ReservationDTO checkIn(Long id) {
        Reservation reservation = reservationRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy đặt bàn với ID: " + id));

        System.out.println("=== CHECK-IN ===");
        System.out.println("Reservation ID: " + id);
        System.out.println("Current status: " + reservation.getStatus());

        // ✅ Nếu đã CHECKED_IN, trả về luôn
        if (reservation.getStatus() == Reservation.ReservationStatus.CHECKED_IN) {
            System.out.println("⚠️ Reservation already checked in!");
            return convertToDTO(reservation);
        }

        // ✅ Cho phép check-in từ PENDING hoặc CONFIRMED
        if (reservation.getStatus() == Reservation.ReservationStatus.PENDING) {
            System.out.println("⚠️ Reservation is PENDING, auto-confirming...");
            reservation.confirm();
            reservationRepository.save(reservation);
            System.out.println("✅ Auto-confirmed to CONFIRMED");
        } else if (reservation.getStatus() != Reservation.ReservationStatus.CONFIRMED) {
            throw new RuntimeException("Đặt bàn không thể check-in! Trạng thái hiện tại: " + reservation.getStatus());
        }

        // 1. Cập nhật reservation thành CHECKED_IN
        reservation.checkIn();
        Reservation saved = reservationRepository.save(reservation);
        System.out.println("✅ Reservation status updated to CHECKED_IN");

        // 2. Cập nhật trạng thái bàn thành OCCUPIED
        BidaTable table = reservation.getTable();
        table.setStatus(BidaTable.TableStatus.OCCUPIED);
        bidaTableRepository.save(table);
        System.out.println("✅ Table " + table.getNumber() + " status -> OCCUPIED");

        // ❌ KHÔNG TẠO ORDER, KHÔNG THÊM SP GÌ CẢ
        // Staff sẽ tự vào bàn và thêm món sau

        // 3. Gửi WebSocket notification
        sendTableStatusNotification(saved, "OCCUPIED");

        return convertToDTO(saved);
    }

    // ========== XÓA ĐẶT BÀN ==========
    @Transactional
    public void deleteReservation(Long id) {
        Reservation reservation = reservationRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy đặt bàn"));

        Long tableId = reservation.getTable().getId();
        LocalDate reservationDate = reservation.getReservationDate();
        LocalDate today = LocalDate.now();

        reservationRepository.delete(reservation);

        // ========== CẬP NHẬT TRẠNG THÁI BÀN KHI XÓA ==========
        // ✅ CHỈ cập nhật nếu đặt cho HÔM NAY
        if (reservationDate.equals(today)) {
            BidaTable table = bidaTableRepository.findById(tableId).orElse(null);
            if (table != null && table.getStatus() == BidaTable.TableStatus.RESERVED) {
                table.setStatus(BidaTable.TableStatus.FREE);
                bidaTableRepository.save(table);
                System.out.println("✅ Table " + table.getNumber() + " -> FREE (xóa đặt hôm nay)");
            }
        } else {
            System.out.println("📅 Xóa đặt cho ngày " + reservationDate + " - KHÔNG cập nhật bàn");
        }

        // Gửi WebSocket notification
        try {
            webSocketService.notifyTableStatus(tableId, "FREE");
            System.out.println("✅ WebSocket sent: Table " + tableId + " status -> FREE");
        } catch (Exception e) {
            System.err.println("⚠️ WebSocket notification failed: " + e.getMessage());
        }
    }

    // ========== PRIVATE HELPER: GỬI WEBSOCKET NOTIFICATION ==========
    private void sendTableStatusNotification(Reservation reservation, String status) {
        try {
            Long tableId = reservation.getTable().getId();
            String tableNumber = reservation.getTable().getNumber() != null
                    ? reservation.getTable().getNumber().toString()
                    : "?";
            LocalDate reservationDate = reservation.getReservationDate();

            // Tạo message với thông tin ngày đặt
            WebSocketService.TableStatusMessage message = new WebSocketService.TableStatusMessage(tableId, status);
            message.setCustomerName(reservation.getCustomerName());
            message.setReservationDate(reservationDate);

            webSocketService.sendTableStatusMessage(message);
            System.out.println("✅ WebSocket sent: Table " + tableNumber + " (ID:" + tableId + ") status -> " + status
                    + " for date " + reservationDate);
        } catch (Exception e) {
            System.err.println("⚠️ WebSocket notification failed: " + e.getMessage());
        }
    }

    // ========== PRIVATE HELPER: LẤY USER HIỆN TẠI ==========
    private User getCurrentUser() {
        try {
            Object principal = SecurityContextHolder.getContext().getAuthentication().getPrincipal();
            if (principal instanceof UserDetailsImpl) {
                Long userId = ((UserDetailsImpl) principal).getId();
                return userRepository.findById(userId).orElse(null);
            }
        } catch (Exception e) {
            System.err.println("⚠️ Cannot get current user: " + e.getMessage());
        }
        return null;
    }
}