package thang.bida.controllers;

import thang.bida.dto.CustomerPointDTO;
import thang.bida.services.CustomerPointService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/customer-points")
@CrossOrigin(origins = "*")
public class CustomerPointController {

    @Autowired
    private CustomerPointService customerPointService;

    private ResponseEntity<Map<String, Object>> successResponse(String message, Object data) {
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("message", message);
        response.put("data", data);
        return ResponseEntity.ok(response);
    }

    private ResponseEntity<Map<String, Object>> errorResponse(String message, HttpStatus status) {
        Map<String, Object> response = new HashMap<>();
        response.put("success", false);
        response.put("message", message);
        return ResponseEntity.status(status).body(response);
    }

    @GetMapping("/all")
    @PreAuthorize("hasAnyRole('ADMIN', 'STAFF')")
    public ResponseEntity<Map<String, Object>> getAllCustomers() {
        try {
            List<CustomerPointDTO> customers = customerPointService.getAllCustomers();
            return successResponse("Lấy danh sách thành công", customers);
        } catch (Exception e) {
            return errorResponse(e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @GetMapping("/id/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'STAFF')")
    public ResponseEntity<Map<String, Object>> getCustomerById(@PathVariable Long id) {
        try {
            CustomerPointDTO customer = customerPointService.getCustomerById(id);
            return successResponse("Tìm thấy khách hàng", customer);
        } catch (Exception e) {
            return errorResponse(e.getMessage(), HttpStatus.NOT_FOUND);
        }
    }

    @GetMapping("/{phone}")
    @PreAuthorize("hasAnyRole('ADMIN', 'STAFF')")
    public ResponseEntity<Map<String, Object>> getCustomerByPhone(@PathVariable String phone) {
        try {
            CustomerPointDTO customer = customerPointService.getCustomerByPhone(phone);
            return successResponse("Tìm thấy khách hàng", customer);
        } catch (Exception e) {
            return errorResponse(e.getMessage(), HttpStatus.NOT_FOUND);
        }
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'STAFF')")
    public ResponseEntity<Map<String, Object>> createCustomer(@RequestBody CustomerPointDTO dto) {
        try {
            CustomerPointDTO created = customerPointService.createCustomer(dto);
            return successResponse("Thêm khách hàng thành công", created);
        } catch (Exception e) {
            return errorResponse(e.getMessage(), HttpStatus.BAD_REQUEST);
        }
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'STAFF')")
    public ResponseEntity<Map<String, Object>> updateCustomer(@PathVariable Long id,
            @RequestBody CustomerPointDTO dto) {
        try {
            CustomerPointDTO updated = customerPointService.updateCustomer(id, dto);
            return successResponse("Cập nhật khách hàng thành công", updated);
        } catch (Exception e) {
            return errorResponse(e.getMessage(), HttpStatus.BAD_REQUEST);
        }
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, Object>> deleteCustomer(@PathVariable Long id) {
        try {
            customerPointService.deleteCustomer(id);
            return successResponse("Xóa khách hàng thành công", null);
        } catch (Exception e) {
            return errorResponse(e.getMessage(), HttpStatus.NOT_FOUND);
        }
    }

    @PostMapping("/add-points")
    @PreAuthorize("hasAnyRole('ADMIN', 'STAFF')")
    public ResponseEntity<Map<String, Object>> addPoints(@RequestBody Map<String, Object> request) {
        try {
            String phone = (String) request.get("phone");
            Integer points = (Integer) request.get("points");
            CustomerPointDTO updated = customerPointService.addPoints(phone, points);
            return successResponse("Cộng " + points + " điểm thành công", updated);
        } catch (Exception e) {
            return errorResponse(e.getMessage(), HttpStatus.BAD_REQUEST);
        }
    }

    @PostMapping("/redeem-points")
    @PreAuthorize("hasAnyRole('ADMIN', 'STAFF')")
    public ResponseEntity<Map<String, Object>> redeemPoints(@RequestBody Map<String, Object> request) {
        try {
            String phone = (String) request.get("phone");
            Integer points = (Integer) request.get("points");
            CustomerPointDTO updated = customerPointService.redeemPoints(phone, points);
            return successResponse("Đổi " + points + " điểm thành công", updated);
        } catch (Exception e) {
            return errorResponse(e.getMessage(), HttpStatus.BAD_REQUEST);
        }
    }

    @PostMapping("/add-hours")
    @PreAuthorize("hasAnyRole('ADMIN', 'STAFF')")
    public ResponseEntity<Map<String, Object>> addHoursPlayed(@RequestBody Map<String, Object> request) {
        try {
            String phone = (String) request.get("phone");
            Integer hours = (Integer) request.get("hours");
            CustomerPointDTO updated = customerPointService.addHoursPlayed(phone, hours);
            return successResponse("Cộng " + hours + " giờ chơi thành công", updated);
        } catch (Exception e) {
            return errorResponse(e.getMessage(), HttpStatus.BAD_REQUEST);
        }
    }
}