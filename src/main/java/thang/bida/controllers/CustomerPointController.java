package thang.bida.controllers;

import thang.bida.dto.CustomerPointDTO;
import thang.bida.dto.TransactionDTO;
import thang.bida.services.CustomerPointService;
import thang.bida.services.UserDetailsImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
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

    // ========== API CHO CUSTOMER (dùng token) ==========

    @GetMapping("/me")
    @PreAuthorize("hasAnyRole('CUSTOMER', 'ADMIN', 'STAFF')")
    public ResponseEntity<Map<String, Object>> getMyPoints() {
        try {
            Object principal = SecurityContextHolder.getContext().getAuthentication().getPrincipal();
            if (principal instanceof UserDetailsImpl) {
                String phone = ((UserDetailsImpl) principal).getUsername();
                CustomerPointDTO customerPoint = customerPointService.getCustomerByPhone(phone);

                if (customerPoint == null) {
                    customerPoint = customerPointService.createCustomerForUser(phone);
                }
                return successResponse("Lấy thông tin điểm thành công", customerPoint);
            }
            return errorResponse("Không tìm thấy thông tin người dùng", HttpStatus.UNAUTHORIZED);
        } catch (Exception e) {
            return errorResponse(e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @GetMapping("/transactions")
    @PreAuthorize("hasAnyRole('CUSTOMER', 'ADMIN', 'STAFF')")
    public ResponseEntity<Map<String, Object>> getMyTransactions() {
        try {
            Object principal = SecurityContextHolder.getContext().getAuthentication().getPrincipal();
            if (principal instanceof UserDetailsImpl) {
                String phone = ((UserDetailsImpl) principal).getUsername();
                List<TransactionDTO> transactions = customerPointService.getTransactionsByPhone(phone);
                return successResponse("Lấy lịch sử giao dịch thành công", transactions);
            }
            return errorResponse("Không tìm thấy thông tin người dùng", HttpStatus.UNAUTHORIZED);
        } catch (Exception e) {
            return errorResponse(e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    // ========== THÊM MỚI: API ĐỔI SẢN PHẨM CHO CUSTOMER ==========

    /**
     * API: Customer xem danh sách sản phẩm có thể đổi bằng điểm
     * GET /api/customer-points/redeemable-products
     */
    @GetMapping("/redeemable-products")
    @PreAuthorize("hasAnyRole('CUSTOMER', 'ADMIN', 'STAFF')")
    public ResponseEntity<Map<String, Object>> getRedeemableProducts() {
        try {
            Object principal = SecurityContextHolder.getContext().getAuthentication().getPrincipal();
            if (principal instanceof UserDetailsImpl) {
                String phone = ((UserDetailsImpl) principal).getUsername();
                Map<String, Object> result = customerPointService.getRedeemableProducts(phone);
                return successResponse("Lấy danh sách sản phẩm đổi điểm thành công", result);
            }
            return errorResponse("Không xác định được người dùng", HttpStatus.UNAUTHORIZED);
        } catch (Exception e) {
            return errorResponse(e.getMessage(), HttpStatus.BAD_REQUEST);
        }
    }

    /**
     * API: Customer tự đổi sản phẩm
     * POST /api/customer-points/redeem-product
     * Body: { "productId": 1, "quantity": 2 }
     */
    @PostMapping("/redeem-product")
    @PreAuthorize("hasAnyRole('CUSTOMER', 'ADMIN', 'STAFF')")
    public ResponseEntity<Map<String, Object>> redeemProduct(@RequestBody Map<String, Object> request) {
        try {
            Object principal = SecurityContextHolder.getContext().getAuthentication().getPrincipal();
            if (principal instanceof UserDetailsImpl) {
                String phone = ((UserDetailsImpl) principal).getUsername();
                Long productId = Long.valueOf(request.get("productId").toString());
                Integer quantity = Integer.valueOf(request.get("quantity").toString());

                Map<String, Object> result = customerPointService.redeemProduct(phone, productId, quantity);
                return ResponseEntity.ok(result);
            }
            return errorResponse("Không xác định được người dùng", HttpStatus.UNAUTHORIZED);
        } catch (Exception e) {
            return errorResponse(e.getMessage(), HttpStatus.BAD_REQUEST);
        }
    }

    /**
     * API: Customer xem lịch sử đổi sản phẩm
     * GET /api/customer-points/redeem-history
     */
    @GetMapping("/redeem-history")
    @PreAuthorize("hasAnyRole('CUSTOMER', 'ADMIN', 'STAFF')")
    public ResponseEntity<Map<String, Object>> getRedeemHistory() {
        try {
            Object principal = SecurityContextHolder.getContext().getAuthentication().getPrincipal();
            if (principal instanceof UserDetailsImpl) {
                String phone = ((UserDetailsImpl) principal).getUsername();
                List<Map<String, Object>> history = customerPointService.getRedeemHistory(phone);
                return successResponse("Lấy lịch sử đổi sản phẩm thành công", history);
            }
            return errorResponse("Không xác định được người dùng", HttpStatus.UNAUTHORIZED);
        } catch (Exception e) {
            return errorResponse(e.getMessage(), HttpStatus.BAD_REQUEST);
        }
    }

    // ========== API CHO ADMIN/STAFF ==========

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

    // ========== THÊM MỚI: API CHO ADMIN/STAFF ĐỔI SẢN PHẨM ==========

    /**
     * API: Admin/Staff xem sản phẩm khách có thể đổi
     * GET /api/customer-points/redeemable-products/{phone}
     */
    @GetMapping("/redeemable-products/{phone}")
    @PreAuthorize("hasAnyRole('ADMIN', 'STAFF')")
    public ResponseEntity<Map<String, Object>> getRedeemableProductsByPhone(@PathVariable String phone) {
        try {
            Map<String, Object> result = customerPointService.getRedeemableProducts(phone);
            return successResponse("Lấy danh sách sản phẩm đổi điểm thành công", result);
        } catch (Exception e) {
            return errorResponse(e.getMessage(), HttpStatus.BAD_REQUEST);
        }
    }

    /**
     * API: Admin/Staff đổi sản phẩm cho khách
     * POST /api/customer-points/redeem-product/{phone}
     * Body: { "productId": 1, "quantity": 2 }
     */
    @PostMapping("/redeem-product/{phone}")
    @PreAuthorize("hasAnyRole('ADMIN', 'STAFF')")
    public ResponseEntity<Map<String, Object>> redeemProductForCustomer(
            @PathVariable String phone,
            @RequestBody Map<String, Object> request) {
        try {
            Long productId = Long.valueOf(request.get("productId").toString());
            Integer quantity = Integer.valueOf(request.get("quantity").toString());

            Map<String, Object> result = customerPointService.redeemProduct(phone, productId, quantity);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            return errorResponse(e.getMessage(), HttpStatus.BAD_REQUEST);
        }
    }

    /**
     * API: Admin/Staff xem lịch sử đổi sản phẩm của khách
     * GET /api/customer-points/redeem-history/{phone}
     */
    @GetMapping("/redeem-history/{phone}")
    @PreAuthorize("hasAnyRole('ADMIN', 'STAFF')")
    public ResponseEntity<Map<String, Object>> getRedeemHistoryByPhone(@PathVariable String phone) {
        try {
            List<Map<String, Object>> history = customerPointService.getRedeemHistory(phone);
            return successResponse("Lấy lịch sử đổi sản phẩm thành công", history);
        } catch (Exception e) {
            return errorResponse(e.getMessage(), HttpStatus.BAD_REQUEST);
        }
    }

    // ========== CÁC API CŨ GIỮ NGUYÊN ==========

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
}