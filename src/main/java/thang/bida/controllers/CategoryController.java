package thang.bida.controllers;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import thang.bida.dto.CategoryRequest;
import thang.bida.model.Category;
import thang.bida.services.CategoryService;
import jakarta.validation.Valid;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/categories")
@CrossOrigin(origins = "*")
public class CategoryController {

    @Autowired
    private CategoryService categoryService;

    // XEM TẤT CẢ DANH MỤC
    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN','STAFF','CUSTOMER')")
    public ResponseEntity<?> getAllCategories() {
        List<Category> categories = categoryService.getAllCategories();

        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("message", "Lấy danh sách categories thành công");
        response.put("data", categories);
        response.put("count", categories.size());

        return ResponseEntity.ok(response);
    }

    // XEM DANH MỤC ĐANG ACTIVE
    @GetMapping("/active")
    @PreAuthorize("hasAnyRole('ADMIN','STAFF','CUSTOMER')")
    public ResponseEntity<?> getActiveCategories() {
        List<Category> categories = categoryService.getActiveCategories();

        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("message", "Lấy danh sách categories đang active thành công");
        response.put("data", categories);
        response.put("count", categories.size());

        return ResponseEntity.ok(response);
    }

    // XEM CHI TIẾT DANH MỤC
    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','STAFF','CUSTOMER')")
    public ResponseEntity<?> getCategoryById(@PathVariable Long id) {
        Optional<Category> categoryOpt = categoryService.getCategoryById(id);

        if (categoryOpt.isEmpty()) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "Không tìm thấy category với ID: " + id);
            return ResponseEntity.status(404).body(response);
        }

        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("message", "Lấy thông tin category thành công");
        response.put("data", categoryOpt.get());

        return ResponseEntity.ok(response);
    }

    // TÌM KIẾM DANH MỤC THEO TÊN
    @GetMapping("/search")
    @PreAuthorize("hasAnyRole('ADMIN','STAFF','CUSTOMER')")
    public ResponseEntity<?> searchCategories(@RequestParam String keyword) {
        List<Category> categories = categoryService.searchCategories(keyword);

        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("message", "Tìm kiếm categories thành công");
        response.put("data", categories);
        response.put("count", categories.size());

        return ResponseEntity.ok(response);
    }

    // 🆕 THÊM DANH MỤC BẰNG FORM-DATA (CHỈ 1 POST DUY NHẤT)
    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN')")
    public ResponseEntity<?> createCategory(
            @RequestParam("name") String name,
            @RequestParam(value = "description", required = false) String description,
            @RequestParam(value = "imageUrl", required = false) String imageUrl,
            @RequestParam(value = "image", required = false) MultipartFile image) {

        try {
            CategoryRequest request = new CategoryRequest();
            request.setName(name);
            request.setDescription(description);

            // Xử lý ảnh: nếu upload file thì lưu, nếu không thì dùng imageUrl
            if (image != null && !image.isEmpty()) {
                String uploadedImageUrl = saveImage(image);
                request.setImageUrl(uploadedImageUrl);
            } else if (imageUrl != null && !imageUrl.isEmpty()) {
                request.setImageUrl(imageUrl);
            }

            Category category = categoryService.createCategory(request);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Tạo category thành công");
            response.put("data", category);

            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    // Phương thức lưu ảnh (tạm thời)
    private String saveImage(MultipartFile image) {
        return "/uploads/" + image.getOriginalFilename();
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN')")
    public ResponseEntity<?> updateCategory(
            @PathVariable Long id,
            @RequestParam("name") String name,
            @RequestParam(value = "description", required = false) String description,
            @RequestParam(value = "imageUrl", required = false) String imageUrl,
            @RequestParam(value = "image", required = false) MultipartFile image) {

        try {
            CategoryRequest request = new CategoryRequest();
            request.setName(name);
            request.setDescription(description);

            // Xử lý ảnh: nếu upload file thì lưu, nếu không thì dùng imageUrl
            if (image != null && !image.isEmpty()) {
                String uploadedImageUrl = saveImage(image);
                request.setImageUrl(uploadedImageUrl);
            } else if (imageUrl != null && !imageUrl.isEmpty()) {
                request.setImageUrl(imageUrl);
            }

            Category category = categoryService.updateCategory(id, request);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Cập nhật category thành công");
            response.put("data", category);

            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    // XÓA VĨNH VIỄN DANH MỤC
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> permanentDeleteCategory(@PathVariable Long id) {
        boolean deleted = categoryService.permanentDeleteCategory(id);

        if (deleted) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Xóa vĩnh viễn category thành công");
            return ResponseEntity.ok(response);
        } else {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "Không tìm thấy category với ID: " + id);
            return ResponseEntity.status(404).body(response);
        }
    }

    // THAY ĐỔI TRẠNG THÁI ACTIVE/INACTIVE
    @PatchMapping("/{id}/status")
    @PreAuthorize("hasAnyRole('ADMIN')")
    public ResponseEntity<?> toggleCategoryStatus(
            @PathVariable Long id,
            @RequestParam Boolean active) {
        try {
            Category category = categoryService.toggleCategoryStatus(id, active);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", active ? "Kích hoạt category thành công" : "Deactivate category thành công");
            response.put("data", category);

            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }
}