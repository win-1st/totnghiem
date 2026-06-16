package thang.bida.controllers;

import thang.bida.dto.ReservationDTO;
import thang.bida.services.ReservationService;
import thang.bida.services.UserDetailsImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/reservations")
@CrossOrigin(origins = "*")
public class ReservationController {

    @Autowired
    private ReservationService reservationService;

    private ResponseEntity<Map<String, Object>> successResponse(String message, Object data) {
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("message", message);
        response.put("data", data);
        return ResponseEntity.ok(response);
    }

    private ResponseEntity<Map<String, Object>> errorResponse(String message) {
        Map<String, Object> response = new HashMap<>();
        response.put("success", false);
        response.put("message", message);
        return ResponseEntity.badRequest().body(response);
    }

    // Thêm vào ReservationController.java

    // Lấy lịch sử đặt bàn của khách hàng hiện tại
    @GetMapping("/my-reservations")
    @PreAuthorize("hasAnyRole('CUSTOMER', 'ADMIN', 'STAFF', 'MANAGER')")
    public ResponseEntity<?> getMyReservations() {
        try {
            Object principal = SecurityContextHolder.getContext().getAuthentication().getPrincipal();
            if (principal instanceof UserDetailsImpl) {
                Long customerId = ((UserDetailsImpl) principal).getId();
                List<ReservationDTO> reservations = reservationService.getReservationsByCustomerId(customerId);
                return successResponse("Lấy lịch sử đặt bàn thành công", reservations);
            }
            return errorResponse("Không tìm thấy thông tin người dùng");
        } catch (Exception e) {
            return errorResponse(e.getMessage());
        }
    }

    // Lấy lịch sử đặt bàn theo customerId (cho admin/staff)
    @GetMapping("/customer/{customerId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'STAFF', 'MANAGER')")
    public ResponseEntity<?> getReservationsByCustomerId(@PathVariable Long customerId) {
        try {
            List<ReservationDTO> reservations = reservationService.getReservationsByCustomerId(customerId);
            return successResponse("Lấy lịch sử đặt bàn thành công", reservations);
        } catch (Exception e) {
            return errorResponse(e.getMessage());
        }
    }

    // Lấy tất cả đặt bàn
    @GetMapping("/all")
    @PreAuthorize("hasAnyRole('ADMIN', 'STAFF', 'MANAGER')")
    public ResponseEntity<?> getAllReservations() {
        List<ReservationDTO> reservations = reservationService.getAllReservations();
        return successResponse("Lấy danh sách đặt bàn thành công", reservations);
    }

    // Lấy đặt bàn theo ID
    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'STAFF', 'MANAGER', 'CUSTOMER')")
    public ResponseEntity<?> getReservationById(@PathVariable Long id) {
        try {
            ReservationDTO reservation = reservationService.getReservationById(id);
            return successResponse("Lấy thông tin đặt bàn thành công", reservation);
        } catch (Exception e) {
            return errorResponse(e.getMessage());
        }
    }

    // Lấy đặt bàn theo số điện thoại
    @GetMapping("/phone/{phone}")
    @PreAuthorize("hasAnyRole('ADMIN', 'STAFF', 'MANAGER')")
    public ResponseEntity<?> getReservationsByPhone(@PathVariable String phone) {
        List<ReservationDTO> reservations = reservationService.getReservationsByPhone(phone);
        return successResponse("Lấy danh sách đặt bàn thành công", reservations);
    }

    // Lấy đặt bàn theo ngày
    @GetMapping("/date/{date}")
    @PreAuthorize("hasAnyRole('ADMIN', 'STAFF', 'MANAGER')")
    public ResponseEntity<?> getReservationsByDate(
            @PathVariable @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        List<ReservationDTO> reservations = reservationService.getReservationsByDate(date);
        return successResponse("Lấy danh sách đặt bàn theo ngày thành công", reservations);
    }

    // Lấy đặt bàn hôm nay
    @GetMapping("/today")
    @PreAuthorize("hasAnyRole('ADMIN', 'STAFF', 'MANAGER')")
    public ResponseEntity<?> getTodaysReservations() {
        List<ReservationDTO> reservations = reservationService.getTodaysReservations();
        return successResponse("Lấy danh sách đặt bàn hôm nay thành công", reservations);
    }

    // Lấy đặt bàn sắp tới
    @GetMapping("/upcoming")
    @PreAuthorize("hasAnyRole('ADMIN', 'STAFF', 'MANAGER')")
    public ResponseEntity<?> getUpcomingReservations() {
        List<ReservationDTO> reservations = reservationService.getUpcomingReservations();
        return successResponse("Lấy danh sách đặt bàn sắp tới thành công", reservations);
    }

    // Kiểm tra bàn có trống không
    @GetMapping("/check-availability")
    @PreAuthorize("hasAnyRole('ADMIN', 'STAFF', 'MANAGER', 'CUSTOMER')")
    public ResponseEntity<?> checkAvailability(
            @RequestParam Long tableId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.TIME) LocalTime time,
            @RequestParam(defaultValue = "2") int durationHours) {

        boolean available = reservationService.isTableAvailable(tableId, date, time, durationHours);
        Map<String, Object> response = new HashMap<>();
        response.put("available", available);
        response.put("tableId", tableId);
        response.put("date", date);
        response.put("time", time);
        return successResponse("Kiểm tra thành công", response);
    }

    // Tạo đặt bàn mới
    @PostMapping("/create")
    @PreAuthorize("permitAll()")
    public ResponseEntity<?> createReservation(@RequestBody ReservationDTO dto) {
        try {
            Long customerId = null;
            try {
                Object principal = SecurityContextHolder.getContext().getAuthentication().getPrincipal();
                if (principal instanceof UserDetailsImpl) {
                    customerId = ((UserDetailsImpl) principal).getId();
                }
            } catch (Exception e) {
                // Không có user đăng nhập
            }

            ReservationDTO reservation = reservationService.createReservation(dto, customerId);
            return successResponse("Đặt bàn thành công", reservation);
        } catch (Exception e) {
            return errorResponse(e.getMessage());
        }
    }

    // Cập nhật đặt bàn
    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'STAFF', 'MANAGER')")
    public ResponseEntity<?> updateReservation(@PathVariable Long id, @RequestBody ReservationDTO dto) {
        try {
            ReservationDTO reservation = reservationService.updateReservation(id, dto);
            return successResponse("Cập nhật đặt bàn thành công", reservation);
        } catch (Exception e) {
            return errorResponse(e.getMessage());
        }
    }

    // Xác nhận đặt bàn
    @PatchMapping("/{id}/confirm")
    @PreAuthorize("hasAnyRole('ADMIN', 'STAFF', 'MANAGER')")
    public ResponseEntity<?> confirmReservation(@PathVariable Long id) {
        try {
            ReservationDTO reservation = reservationService.confirmReservation(id);
            return successResponse("Xác nhận đặt bàn thành công", reservation);
        } catch (Exception e) {
            return errorResponse(e.getMessage());
        }
    }

    // Hủy đặt bàn
    @PatchMapping("/{id}/cancel")
    @PreAuthorize("hasAnyRole('ADMIN', 'STAFF', 'MANAGER', 'CUSTOMER')")
    public ResponseEntity<?> cancelReservation(
            @PathVariable Long id,
            @RequestParam(required = false) String reason) {
        try {
            ReservationDTO reservation = reservationService.cancelReservation(id, reason);
            return successResponse("Hủy đặt bàn thành công", reservation);
        } catch (Exception e) {
            return errorResponse(e.getMessage());
        }
    }

    // Check-in
    @PatchMapping("/{id}/checkin")
    @PreAuthorize("hasAnyRole('ADMIN', 'STAFF', 'MANAGER')")
    public ResponseEntity<?> checkIn(@PathVariable Long id) {
        try {
            ReservationDTO reservation = reservationService.checkIn(id);
            return successResponse("Check-in thành công", reservation);
        } catch (Exception e) {
            return errorResponse(e.getMessage());
        }
    }

    // Xóa đặt bàn
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
    public ResponseEntity<?> deleteReservation(@PathVariable Long id) {
        try {
            reservationService.deleteReservation(id);
            return successResponse("Xóa đặt bàn thành công", null);
        } catch (Exception e) {
            return errorResponse(e.getMessage());
        }
    }
}