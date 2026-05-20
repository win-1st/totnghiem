package thang.bida.controllers;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import thang.bida.model.Promotion;
import thang.bida.model.Product;
import thang.bida.model.PromotionProduct;
import thang.bida.services.PromotionService;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/promotions")
@CrossOrigin(origins = "*")
public class PromotionController {

    private final PromotionService promotionService;

    public PromotionController(PromotionService promotionService) {
        this.promotionService = promotionService;
    }

    // ========== CRUD PROMOTIONS ==========

    // Lấy tất cả khuyến mãi
    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER')")
    public ResponseEntity<?> getAllPromotions() {
        List<Promotion> promotions = promotionService.getAllPromotions();

        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("message", "Lấy danh sách khuyến mãi thành công");
        response.put("data", promotions);
        response.put("count", promotions.size());

        return ResponseEntity.ok(response);
    }

    // Lấy khuyến mãi đang hoạt động
    @GetMapping("/active")
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER','STAFF','CUSTOMER')")
    public ResponseEntity<?> getActivePromotions() {
        List<Promotion> promotions = promotionService.getActivePromotions();

        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("message", "Lấy danh sách khuyến mãi đang hoạt động thành công");
        response.put("data", promotions);
        response.put("count", promotions.size());

        return ResponseEntity.ok(response);
    }

    // Lấy khuyến mãi hiện tại (đang trong thời gian áp dụng)
    @GetMapping("/current")
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER','STAFF','CUSTOMER')")
    public ResponseEntity<?> getCurrentPromotions() {
        List<Promotion> promotions = promotionService.getCurrentPromotions();

        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("message", "Lấy danh sách khuyến mãi hiện tại thành công");
        response.put("data", promotions);
        response.put("count", promotions.size());

        return ResponseEntity.ok(response);
    }

    // Lấy chi tiết khuyến mãi theo ID
    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER')")
    public ResponseEntity<?> getPromotionById(@PathVariable Long id) {
        try {
            Promotion promotion = promotionService.getPromotionById(id);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Lấy thông tin khuyến mãi thành công");
            response.put("data", promotion);

            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", e.getMessage());
            return ResponseEntity.status(404).body(response);
        }
    }

    // Tìm kiếm khuyến mãi theo tên
    @GetMapping("/search")
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER')")
    public ResponseEntity<?> searchPromotions(@RequestParam String keyword) {
        List<Promotion> promotions = promotionService.searchPromotionsByName(keyword);

        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("message", "Tìm kiếm khuyến mãi thành công");
        response.put("data", promotions);
        response.put("count", promotions.size());

        return ResponseEntity.ok(response);
    }

    // Tạo khuyến mãi mới
    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> createPromotion(@RequestBody Promotion promotion) {
        try {
            Promotion created = promotionService.createPromotion(promotion);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Tạo khuyến mãi thành công");
            response.put("data", created);

            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    // Cập nhật khuyến mãi
    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> updatePromotion(@PathVariable Long id, @RequestBody Promotion promotion) {
        try {
            Promotion updated = promotionService.updatePromotion(id, promotion);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Cập nhật khuyến mãi thành công");
            response.put("data", updated);

            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    // Xóa khuyến mãi
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> deletePromotion(@PathVariable Long id) {
        try {
            boolean deleted = promotionService.deletePromotion(id);

            if (deleted) {
                Map<String, Object> response = new HashMap<>();
                response.put("success", true);
                response.put("message", "Xóa khuyến mãi thành công");
                return ResponseEntity.ok(response);
            } else {
                Map<String, Object> response = new HashMap<>();
                response.put("success", false);
                response.put("message", "Không tìm thấy khuyến mãi với ID: " + id);
                return ResponseEntity.status(404).body(response);
            }
        } catch (RuntimeException e) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    // Thay đổi trạng thái khuyến mãi
    @PatchMapping("/{id}/status")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> togglePromotionStatus(
            @PathVariable Long id,
            @RequestParam Boolean isActive) {
        try {
            Promotion promotion = promotionService.togglePromotionStatus(id, isActive);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", isActive ? "Kích hoạt khuyến mãi thành công" : "Tắt khuyến mãi thành công");
            response.put("data", promotion);

            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    // ========== PRODUCT MANAGEMENT IN PROMOTION ==========

    // Lấy danh sách sản phẩm trong khuyến mãi
    @GetMapping("/{promotionId}/products")
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER')")
    public ResponseEntity<?> getProductsInPromotion(@PathVariable Long promotionId) {
        try {
            List<Product> products = promotionService.getProductsInPromotion(promotionId);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Lấy danh sách sản phẩm trong khuyến mãi thành công");
            response.put("data", products);
            response.put("count", products.size());

            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    // Lấy danh sách sản phẩm chưa trong khuyến mãi
    @GetMapping("/{promotionId}/available-products")
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER')")
    public ResponseEntity<?> getProductsNotInPromotion(@PathVariable Long promotionId) {
        try {
            List<Product> products = promotionService.getProductsNotInPromotion(promotionId);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Lấy danh sách sản phẩm chưa trong khuyến mãi thành công");
            response.put("data", products);
            response.put("count", products.size());

            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    // Thêm sản phẩm vào khuyến mãi
    @PostMapping("/{promotionId}/products")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> addProductToPromotion(
            @PathVariable Long promotionId,
            @RequestBody Map<String, Long> request) {
        try {
            Long productId = request.get("productId");
            boolean added = promotionService.addProductToPromotion(promotionId, productId);

            if (added) {
                Map<String, Object> response = new HashMap<>();
                response.put("success", true);
                response.put("message", "Thêm sản phẩm vào khuyến mãi thành công");
                return ResponseEntity.ok(response);
            } else {
                Map<String, Object> response = new HashMap<>();
                response.put("success", false);
                response.put("message", "Không thể thêm sản phẩm (có thể đã tồn tại hoặc không tìm thấy)");
                return ResponseEntity.badRequest().body(response);
            }
        } catch (RuntimeException e) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    // Thêm nhiều sản phẩm vào khuyến mãi
    @PostMapping("/{promotionId}/products/batch")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> addMultipleProductsToPromotion(
            @PathVariable Long promotionId,
            @RequestBody Map<String, List<Long>> request) {
        try {
            List<Long> productIds = request.get("productIds");
            boolean added = promotionService.addMultipleProductsToPromotion(promotionId, productIds);

            if (added) {
                Map<String, Object> response = new HashMap<>();
                response.put("success", true);
                response.put("message", "Thêm " + productIds.size() + " sản phẩm vào khuyến mãi thành công");
                return ResponseEntity.ok(response);
            } else {
                Map<String, Object> response = new HashMap<>();
                response.put("success", false);
                response.put("message", "Không tìm thấy khuyến mãi");
                return ResponseEntity.badRequest().body(response);
            }
        } catch (RuntimeException e) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    // Xóa sản phẩm khỏi khuyến mãi
    @DeleteMapping("/{promotionId}/products/{productId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> removeProductFromPromotion(
            @PathVariable Long promotionId,
            @PathVariable Long productId) {
        try {
            promotionService.removeProductFromPromotion(promotionId, productId);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Xóa sản phẩm khỏi khuyến mãi thành công");
            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    // Xóa nhiều sản phẩm khỏi khuyến mãi
    @DeleteMapping("/{promotionId}/products/batch")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> removeMultipleProductsFromPromotion(
            @PathVariable Long promotionId,
            @RequestBody Map<String, List<Long>> request) {
        try {
            List<Long> productIds = request.get("productIds");
            promotionService.removeMultipleProductsFromPromotion(promotionId, productIds);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Xóa " + productIds.size() + " sản phẩm khỏi khuyến mãi thành công");
            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    // Xóa tất cả sản phẩm khỏi khuyến mãi
    @DeleteMapping("/{promotionId}/products/all")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> clearAllProductsFromPromotion(@PathVariable Long promotionId) {
        try {
            promotionService.clearAllProductsFromPromotion(promotionId);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Xóa tất cả sản phẩm khỏi khuyến mãi thành công");
            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    // ========== VALIDATION ==========

    // Kiểm tra khuyến mãi có đang hoạt động không
    @GetMapping("/{id}/check-active")
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER','STAFF')")
    public ResponseEntity<?> isPromotionActive(@PathVariable Long id) {
        boolean isActive = promotionService.isPromotionActive(id);

        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("isActive", isActive);

        return ResponseEntity.ok(response);
    }
}