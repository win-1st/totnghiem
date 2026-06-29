package thang.bida.controllers;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import thang.bida.dto.ProductRequest;
import thang.bida.model.Product;
import thang.bida.services.ProductService;

@RestController
@RequestMapping("/api/products")
@CrossOrigin(origins = "*")
public class ProductController {

    @Autowired
    private ProductService productService;

    // === TẠO SẢN PHẨM MỚI ===
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER')")
    @PostMapping
    public ResponseEntity<?> createProductForm(@ModelAttribute ProductRequest request) {
        try {
            System.out.println("=== CREATE PRODUCT DEBUG ===");
            System.out.println("Name: " + request.getName());
            System.out.println("ProductType: " + request.getProductType());
            System.out.println("PricePerMinute: " + request.getPricePerMinute());

            Product product = productService.createProduct(request);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Tạo sản phẩm thành công");
            response.put("data", product);

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            e.printStackTrace();
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "Lỗi: " + e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    // === LẤY TẤT CẢ SẢN PHẨM ===
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER','STAFF','CUSTOMER')")
    @GetMapping
    public ResponseEntity<?> getAllProducts() {
        List<Product> products = productService.getAllProducts();

        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("message", "Lấy danh sách sản phẩm thành công");
        response.put("data", products);
        response.put("count", products.size());

        return ResponseEntity.ok(response);
    }

    // === LẤY SẢN PHẨM TIME_BASED ===
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER','STAFF','CUSTOMER')")
    @GetMapping("/time-based")
    public ResponseEntity<?> getTimeBasedProduct() {
        try {
            Product product = productService.getTimeBasedProduct();

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("data", product);
            response.put("pricePerMinute", product.getPricePerMinute());
            response.put("pricePerHour", product.getPricePerMinute().multiply(BigDecimal.valueOf(60)));

            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", e.getMessage());
            return ResponseEntity.status(404).body(response);
        }
    }

    // === LẤY SẢN PHẨM ĐANG HOẠT ĐỘNG ===
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER','STAFF','CUSTOMER')")
    @GetMapping("/active")
    public ResponseEntity<?> getActiveProducts() {
        List<Product> products = productService.getActiveProducts();

        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("message", "Lấy danh sách sản phẩm đang hoạt động thành công");
        response.put("data", products);
        response.put("count", products.size());

        return ResponseEntity.ok(response);
    }

    // === LẤY SẢN PHẨM THEO ID ===
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER','STAFF','CUSTOMER')")
    @GetMapping("/{id}")
    public ResponseEntity<?> getProductById(@PathVariable Long id) {
        Optional<Product> productOpt = productService.getProductById(id);

        if (productOpt.isEmpty()) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "Không tìm thấy sản phẩm với ID: " + id);
            return ResponseEntity.status(404).body(response);
        }

        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("message", "Lấy thông tin sản phẩm thành công");
        response.put("data", productOpt.get());

        return ResponseEntity.ok(response);
    }

    // === LẤY SẢN PHẨM THEO DANH MỤC ===
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER','STAFF','CUSTOMER')")
    @GetMapping("/category/{categoryId}")
    public ResponseEntity<?> getProductsByCategory(@PathVariable Long categoryId) {
        List<Product> products = productService.getProductsByCategory(categoryId);

        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("message", "Lấy danh sách sản phẩm theo danh mục thành công");
        response.put("data", products);
        response.put("count", products.size());

        return ResponseEntity.ok(response);
    }

    // === TÌM KIẾM SẢN PHẨM ===
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER','STAFF','CUSTOMER')")
    @GetMapping("/search")
    public ResponseEntity<?> searchProducts(@RequestParam String keyword) {
        List<Product> products = productService.searchProducts(keyword);

        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("message", "Tìm kiếm sản phẩm thành công");
        response.put("data", products);
        response.put("count", products.size());

        return ResponseEntity.ok(response);
    }

    // === CẬP NHẬT SẢN PHẨM ===
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER')")
    @PutMapping("/{id}")
    public ResponseEntity<?> updateProductForm(
            @PathVariable Long id,
            @RequestParam("name") String name,
            @RequestParam(value = "description", required = false) String description,
            @RequestParam(value = "price", required = false) BigDecimal price,
            @RequestParam("categoryId") Long categoryId,
            @RequestParam(value = "stockQuantity", required = false, defaultValue = "0") Integer stockQuantity,
            @RequestParam(value = "imageUrl", required = false) String imageUrl,
            @RequestParam(value = "active", required = false, defaultValue = "true") Boolean active,
            @RequestParam(value = "image", required = false) MultipartFile image,
            @RequestParam(value = "productType", required = false, defaultValue = "FOOD") String productType,
            @RequestParam(value = "pricePerMinute", required = false) BigDecimal pricePerMinute) {

        try {
            System.out.println("=== UPDATE PRODUCT DEBUG ===");
            System.out.println("ID: " + id);
            System.out.println("ProductType: " + productType);
            System.out.println("PricePerMinute: " + pricePerMinute);

            Product product = productService.updateProductFromForm(
                    id, name, description, price, categoryId, stockQuantity,
                    imageUrl, active, image, productType, pricePerMinute);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Cập nhật sản phẩm thành công");
            response.put("data", product);

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            e.printStackTrace();
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "Lỗi: " + e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    // === XÓA VĨNH VIỄN SẢN PHẨM ===
    @PreAuthorize("hasRole('ADMIN')")
    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteProduct(@PathVariable Long id) {
        try {
            productService.deleteProduct(id);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Xóa sản phẩm thành công");

            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    // === THAY ĐỔI TRẠNG THÁI SẢN PHẨM ===
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER')")
    @PatchMapping("/{id}/status")
    public ResponseEntity<?> toggleProductStatus(
            @PathVariable Long id,
            @RequestParam Boolean active) {
        try {
            Product product = productService.toggleProductStatus(id, active);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", active ? "Kích hoạt sản phẩm thành công" : "Tắt sản phẩm thành công");
            response.put("data", product);

            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    // === CẬP NHẬT SỐ LƯỢNG TỒN KHO ===
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER')")
    @PatchMapping("/{id}/stock")
    public ResponseEntity<?> updateStockQuantity(
            @PathVariable Long id,
            @RequestParam Integer quantity) {
        try {
            Product product = productService.updateStockQuantity(id, quantity);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Cập nhật số lượng tồn kho thành công");
            response.put("data", product);

            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    // === LẤY SẢN PHẨM KHÔNG PHẢI TIME_BASED ===
    @GetMapping("/non-time-based")
    public ResponseEntity<?> getNonTimeBasedProducts() {
        List<Product> products = productService.getAllNonTimeBasedProducts();

        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("data", products);
        response.put("count", products.size());

        return ResponseEntity.ok(response);
    }

    // ========== API MỚI: CẤU HÌNH ĐỔI ĐIỂM ==========

    /**
     * API: Cấu hình đổi điểm cho sản phẩm
     * PATCH /api/products/{id}/redeem-config
     * Body: { "isRedeemable": true, "pointsRequired": 100 }
     */
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER')")
    @PatchMapping("/{id}/redeem-config")
    public ResponseEntity<?> updateRedeemConfig(
            @PathVariable Long id,
            @RequestBody Map<String, Object> request) {
        try {
            Boolean isRedeemable = null;
            Integer pointsRequired = null;

            if (request.containsKey("isRedeemable")) {
                isRedeemable = (Boolean) request.get("isRedeemable");
            }
            if (request.containsKey("pointsRequired")) {
                pointsRequired = (Integer) request.get("pointsRequired");
            }

            // ✅ Gọi ProductService thay vì gọi trực tiếp Repository
            Product product = productService.updateRedeemConfig(id, isRedeemable, pointsRequired);

            Map<String, Object> data = new HashMap<>();
            data.put("id", product.getId());
            data.put("name", product.getName());
            data.put("isRedeemable", product.getIsRedeemable());
            data.put("pointsRequired", product.getPointsRequired());

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Cấu hình đổi điểm thành công");
            response.put("data", data);

            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    /**
     * API: Lấy danh sách sản phẩm có thể đổi điểm
     * GET /api/products/redeemable
     */
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER','STAFF','CUSTOMER')")
    @GetMapping("/redeemable")
    public ResponseEntity<?> getRedeemableProducts() {
        try {
            // ✅ Gọi ProductService thay vì gọi trực tiếp Repository
            List<Product> products = productService.getRedeemableProducts();

            List<Map<String, Object>> formattedProducts = products.stream()
                    .map(p -> {
                        Map<String, Object> item = new HashMap<>();
                        item.put("id", p.getId());
                        item.put("name", p.getName());
                        item.put("imageUrl", p.getImageUrl());
                        item.put("pointsRequired", p.getPointsRequired());
                        item.put("stockQuantity", p.getStockQuantity());
                        item.put("unit", p.getUnit());
                        item.put("price", p.getPrice());
                        item.put("salePrice", p.getSalePrice());
                        item.put("categoryName", p.getCategory() != null ? p.getCategory().getName() : null);
                        return item;
                    })
                    .collect(java.util.stream.Collectors.toList());

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Lấy danh sách sản phẩm đổi điểm thành công");
            response.put("data", formattedProducts);
            response.put("count", formattedProducts.size());

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "Lỗi: " + e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }
}