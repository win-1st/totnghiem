package thang.bida.services;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import thang.bida.dto.ProductRequest;
import thang.bida.model.Category;
import thang.bida.model.Product;
import thang.bida.repository.CategoryRepository;
import thang.bida.repository.ProductRepository;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

@Service
@Transactional
public class ProductService {

    private final ProductRepository productRepository;
    private final CategoryRepository categoryRepository;
    private final CloudinaryService cloudinaryService;

    public ProductService(ProductRepository productRepository,
            CategoryRepository categoryRepository,
            CloudinaryService cloudinaryService) {
        this.productRepository = productRepository;
        this.categoryRepository = categoryRepository;
        this.cloudinaryService = cloudinaryService;
    }

    // === TẠO SẢN PHẨM MỚI ===
    public Product createProduct(ProductRequest request) throws IOException {
        // SỬA: dùng existsByProductTypeCode
        if ("TIME_BASED".equals(request.getProductType())) {
            if (productRepository.existsByProductTypeCode("TIME_BASED")) {
                throw new RuntimeException("Sản phẩm tính giờ đã tồn tại! Chỉ được có 1 sản phẩm tính giờ.");
            }
        }

        String savedImageUrl = request.getImageUrl();
        MultipartFile imageFile = request.getImage();

        if (imageFile != null && !imageFile.isEmpty()) {
            Category category = categoryRepository.findById(request.getCategoryId())
                    .orElseThrow(() -> new RuntimeException("Không tìm thấy category"));

            Product tempProduct = new Product();
            tempProduct.setName(request.getName());
            tempProduct.setDescription(request.getDescription());
            tempProduct.setCategory(category);
            tempProduct.setProductTypeCode(request.getProductType() != null ? request.getProductType() : "FOOD");

            Product savedTemp = productRepository.save(tempProduct);
            String cloudinaryUrl = cloudinaryService.uploadProductImage(imageFile, savedTemp.getId());
            savedImageUrl = cloudinaryUrl;
            productRepository.delete(savedTemp);
        }

        Category category = categoryRepository.findById(request.getCategoryId())
                .orElseThrow(() -> new RuntimeException("Không tìm thấy category"));

        Product product = new Product();
        product.setName(request.getName());
        product.setDescription(request.getDescription());
        product.setCategory(category);
        product.setImageUrl(savedImageUrl);
        product.setActive(request.getActive() != null ? request.getActive() : true);
        product.setProductTypeCode(request.getProductType() != null ? request.getProductType() : "FOOD");

        if ("TIME_BASED".equals(product.getProductTypeCode())) {
            product.setPrice(BigDecimal.ZERO);
            product.setStockQuantity(0);
            product.setPricePerMinute(
                    request.getPricePerMinute() != null ? request.getPricePerMinute() : new BigDecimal("666"));
        } else {
            product.setPrice(request.getPrice());
            product.setStockQuantity(request.getStockQuantity() != null ? request.getStockQuantity() : 0);
            product.setPricePerMinute(null);
        }

        return productRepository.save(product);
    }

    // === CẬP NHẬT SẢN PHẨM ===
    public Product updateProductFromForm(Long id, String name, String description, BigDecimal price,
            Long categoryId, Integer stockQuantity, String imageUrl, Boolean active,
            MultipartFile imageFile, String productType, BigDecimal pricePerMinute) throws IOException {

        Product product = productRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy product với ID: " + id));

        if (product.isTimeBased() && !"TIME_BASED".equals(productType)) {
            System.out.println("⚠️ Đang đổi sản phẩm TIME_BASED sang loại: " + productType);
        }

        // SỬA: dùng existsByProductTypeCode
        if (!product.isTimeBased() && "TIME_BASED".equals(productType)) {
            if (productRepository.existsByProductTypeCode("TIME_BASED")) {
                throw new RuntimeException("Đã có sản phẩm tính giờ! Không thể tạo thêm.");
            }
        }

        String savedImageUrl = product.getImageUrl();
        if (imageFile != null && !imageFile.isEmpty()) {
            if (savedImageUrl != null && !savedImageUrl.isEmpty()) {
                cloudinaryService.deleteProductImage(savedImageUrl);
            }
            String newImageUrl = cloudinaryService.uploadProductImage(imageFile, product.getId());
            savedImageUrl = newImageUrl;
        } else if (imageUrl != null && !imageUrl.trim().isEmpty()) {
            savedImageUrl = imageUrl;
        }

        product.setName(name);
        product.setDescription(description);
        product.setImageUrl(savedImageUrl);
        product.setActive(active);

        if (productType != null) {
            product.setProductTypeCode(productType);
        }

        if ("TIME_BASED".equals(product.getProductTypeCode())) {
            product.setPrice(BigDecimal.ZERO);
            product.setStockQuantity(0);
            product.setPricePerMinute(pricePerMinute != null ? pricePerMinute : new BigDecimal("666"));
        } else {
            product.setPrice(price != null ? price : BigDecimal.ZERO);
            product.setStockQuantity(stockQuantity != null ? stockQuantity : 0);
            product.setPricePerMinute(null);
        }

        if (categoryId != null) {
            Category category = categoryRepository.findById(categoryId)
                    .orElseThrow(() -> new RuntimeException("Không tìm thấy category với ID: " + categoryId));
            product.setCategory(category);
        }

        return productRepository.save(product);
    }

    // === XÓA VĨNH VIỄN SẢN PHẨM ===
    public void deleteProduct(Long id) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy product với ID: " + id));

        if (product.isTimeBased()) {
            throw new RuntimeException("Không thể xóa sản phẩm tính giờ!");
        }

        if (product.getImageUrl() != null && !product.getImageUrl().isEmpty()) {
            try {
                cloudinaryService.deleteProductImage(product.getImageUrl());
            } catch (Exception e) {
                System.out.println("⚠️ Không thể xóa ảnh trên Cloudinary: " + e.getMessage());
            }
        }

        productRepository.delete(product);
    }

    // === CÁC METHOD KHÁC ===
    public List<Product> getAllProducts() {
        return productRepository.findAll();
    }

    public List<Product> getActiveProducts() {
        return productRepository.findByActiveTrue();
    }

    public Optional<Product> getProductById(Long id) {
        return productRepository.findById(id);
    }

    public List<Product> getProductsByCategory(Long categoryId) {
        return productRepository.findByCategoryId(categoryId);
    }

    public List<Product> searchProducts(String keyword) {
        return productRepository.findByNameContainingIgnoreCase(keyword);
    }

    // SỬA: dùng findFirstByProductTypeCode
    public Product getTimeBasedProduct() {
        return productRepository.findFirstByProductTypeCode("TIME_BASED")
                .orElseThrow(() -> new RuntimeException("Chưa cấu hình sản phẩm tính giờ!"));
    }

    public List<Product> getAllNonTimeBasedProducts() {
        return productRepository.findAllNonTimeBased();
    }

    public List<Product> getOutOfStockProducts() {
        return productRepository.findByStockQuantityLessThan(1);
    }

    public Product updateStockQuantity(Long id, Integer quantity) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy product với ID: " + id));

        if (product.isTimeBased()) {
            throw new RuntimeException("Sản phẩm tính giờ không có tồn kho!");
        }

        product.setStockQuantity(quantity);
        return productRepository.save(product);
    }

    public Product toggleProductStatus(Long id, Boolean active) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy product"));

        product.setActive(active);
        return productRepository.save(product);
    }
}