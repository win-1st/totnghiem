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

import thang.bida.model.Product;
import thang.bida.payload.request.ProductRequest;
import thang.bida.services.ProductService;

@RestController
@RequestMapping("/api/products")
@CrossOrigin(origins = "*")
public class ProductController {

    @Autowired
    private ProductService productService;

    @PreAuthorize("hasAnyRole('ADMIN','MANAGER')")
    @PostMapping
    public ResponseEntity<?> createProductForm(@ModelAttribute ProductRequest request) {
        try {
            System.out.println("DEBUG - Image: " + request.getImage());
            System.out.println("DEBUG - Image name: " +
                    (request.getImage() != null ? request.getImage().getOriginalFilename() : "null"));

            Product product = productService.createProduct(request);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Tạo sản phẩm từ form-data thành công");
            response.put("data", product);

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "Lỗi: " + e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    // === LẤY TẤT CẢ PRODUCTS ===
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

    // === LẤY ACTIVE PRODUCTS ===
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

    // === LẤY PRODUCT THEO ID ===
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

    // === LẤY PRODUCTS THEO CATEGORY ===
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER','STAFF','CUSTOMER')")
    @GetMapping("/category/{categoryId}")
    public ResponseEntity<?> getProductsByCategory(@PathVariable Long categoryId) {
        List<Product> products = productService.getProductsByCategory(categoryId);

        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("message", "Lấy danh sách sản phẩm theo category thành công");
        response.put("data", products);
        response.put("count", products.size());

        return ResponseEntity.ok(response);
    }

    // === TÌM KIẾM PRODUCT ===
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

    // === CẬP NHẬT PRODUCT (FORM-DATA) ===
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER')")
    @PutMapping("/{id}")
    public ResponseEntity<?> updateProductForm(
            @PathVariable Long id,
            @RequestParam("name") String name,
            @RequestParam(value = "description", required = false) String description,
            @RequestParam("price") BigDecimal price,
            @RequestParam("categoryId") Long categoryId,
            @RequestParam(value = "stockQuantity", required = false, defaultValue = "0") Integer stockQuantity, // THÊM
                                                                                                                // DÒNG
                                                                                                                // NÀY
            @RequestParam(value = "imageUrl", required = false) String imageUrl,
            @RequestParam(value = "active", required = false, defaultValue = "true") Boolean active,
            @RequestParam(value = "image", required = false) MultipartFile image) {

        try {
            System.out.println("=== UPDATE DEBUG ===");
            System.out.println("Image param: " + image);
            System.out.println("Image filename: " + (image != null ? image.getOriginalFilename() : "null"));
            System.out.println("Stock quantity: " + stockQuantity); // DEBUG thêm

            Product product = productService.updateProductFromForm(
                    id, name, description, price, categoryId, stockQuantity, imageUrl, active, image); // THÊM
                                                                                                       // stockQuantity

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Cập nhật sản phẩm từ form-data thành công");
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

    // === XÓA VĨNH VIỄN PRODUCT ===
    @PreAuthorize("hasRole('ADMIN')")
    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteProduct(@PathVariable Long id) {
        try {
            productService.deleteProduct(id);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Xóa vĩnh viễn sản phẩm thành công");

            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    // === TOGGLE PRODUCT STATUS ===
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER')")
    @PatchMapping("/{id}/status")
    public ResponseEntity<?> toggleProductStatus(
            @PathVariable Long id,
            @RequestParam Boolean active) {
        try {
            Product product = productService.toggleProductStatus(id, active);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", active ? "Kích hoạt sản phẩm thành công" : "Deactivate sản phẩm thành công");
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
}