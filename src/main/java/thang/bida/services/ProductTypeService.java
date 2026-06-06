// ProductTypeService.java
package thang.bida.services;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import thang.bida.dto.ProductTypeDTO;
import thang.bida.model.ProductType;
import thang.bida.repository.ProductTypeRepository;

import jakarta.annotation.PostConstruct;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@Service
@Transactional
public class ProductTypeService {

    private final ProductTypeRepository productTypeRepository;

    public ProductTypeService(ProductTypeRepository productTypeRepository) {
        this.productTypeRepository = productTypeRepository;
    }

    @PostConstruct
    public void initDefaultProductTypes() {
        if (productTypeRepository.count() == 0) {
            List<ProductType> defaultTypes = Arrays.asList(
                    createProductType("FOOD", "Đồ ăn", "🍽️", "Các món ăn, thức ăn", 1),
                    createProductType("DRINK", "Đồ uống", "🍹", "Nước giải khát, bia, rượu", 2),
                    createProductType("OTHER", "Linh tinh", "📦", "Các sản phẩm khác", 3),
                    createProductType("TIME_BASED", "Tính giờ (Tiền bàn)", "⏱️", "Dịch vụ tính tiền theo thời gian",
                            4));

            productTypeRepository.saveAll(defaultTypes);
            System.out.println("✅ Đã khởi tạo 4 loại sản phẩm mặc định!");
        }
    }

    private ProductType createProductType(String code, String name, String icon, String description, int sortOrder) {
        ProductType type = new ProductType();
        type.setCode(code);
        type.setName(name);
        type.setIcon(icon);
        type.setDescription(description);
        type.setSortOrder(sortOrder);
        type.setIsActive(true);
        return type;
    }

    public List<ProductTypeDTO> getAllActiveProductTypes() {
        return productTypeRepository.findByIsActiveTrueOrderBySortOrderAsc()
                .stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    public List<ProductTypeDTO> getAllProductTypes() {
        return productTypeRepository.findAll()
                .stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    public ProductTypeDTO getByCode(String code) {
        ProductType productType = productTypeRepository.findByCode(code)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy loại sản phẩm với code: " + code));
        return convertToDTO(productType);
    }

    private ProductTypeDTO convertToDTO(ProductType productType) {
        ProductTypeDTO dto = new ProductTypeDTO();
        dto.setId(productType.getId());
        dto.setCode(productType.getCode());
        dto.setName(productType.getName());
        dto.setIcon(productType.getIcon());
        dto.setDescription(productType.getDescription());
        dto.setSortOrder(productType.getSortOrder());
        dto.setIsActive(productType.getIsActive());
        return dto;
    }
}