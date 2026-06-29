// ProductTypeController.java
package thang.bida.controllers;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import thang.bida.dto.ProductTypeDTO;
import thang.bida.services.ProductTypeService;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/product-types")
@CrossOrigin(origins = "*")
public class ProductTypeController {

    private final ProductTypeService productTypeService;

    public ProductTypeController(ProductTypeService productTypeService) {
        this.productTypeService = productTypeService;
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN','STAFF')")
    public ResponseEntity<?> getAllProductTypes() {
        List<ProductTypeDTO> productTypes = productTypeService.getAllActiveProductTypes();

        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("message", "Lấy danh sách loại sản phẩm thành công");
        response.put("data", productTypes);
        response.put("count", productTypes.size());

        return ResponseEntity.ok(response);
    }

    @GetMapping("/non-time-based")
    @PreAuthorize("hasAnyRole('ADMIN','STAFF','CUSTOMER')")
    public ResponseEntity<?> getNonTimeBasedTypes() {
        List<ProductTypeDTO> productTypes = productTypeService.getAllActiveProductTypes()
                .stream()
                .filter(type -> !"TIME_BASED".equals(type.getCode()))
                .collect(Collectors.toList());

        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("data", productTypes);
        response.put("count", productTypes.size());

        return ResponseEntity.ok(response);
    }

    @GetMapping("/{code}")
    @PreAuthorize("hasAnyRole('ADMIN','STAFF')")
    public ResponseEntity<?> getProductTypeByCode(@PathVariable String code) {
        try {
            ProductTypeDTO productType = productTypeService.getByCode(code);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("data", productType);

            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", e.getMessage());
            return ResponseEntity.status(404).body(response);
        }
    }

}