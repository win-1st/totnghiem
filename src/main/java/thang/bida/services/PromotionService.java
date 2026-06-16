package thang.bida.services;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import thang.bida.model.Product;
import thang.bida.model.Promotion;
import thang.bida.model.PromotionProduct;
import thang.bida.repository.ProductRepository;
import thang.bida.repository.PromotionProductRepository;
import thang.bida.repository.PromotionRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class PromotionService {

    private final PromotionRepository promotionRepository;
    private final PromotionProductRepository promotionProductRepository;
    private final ProductRepository productRepository;

    public PromotionService(PromotionRepository promotionRepository,
            PromotionProductRepository promotionProductRepository,
            ProductRepository productRepository) {
        this.promotionRepository = promotionRepository;
        this.promotionProductRepository = promotionProductRepository;
        this.productRepository = productRepository;
    }

    // === CRUD OPERATIONS ===
    public Promotion createPromotion(Promotion promotion) {
        // Validate dates
        if (promotion.getStartDate().isAfter(promotion.getEndDate())) {
            throw new RuntimeException("Ngày bắt đầu không thể sau ngày kết thúc");
        }

        // Validate discount values
        if (promotion.getDiscountPercentage() != null && promotion.getDiscountAmount() != null) {
            throw new RuntimeException("Chỉ có thể chọn một loại giảm giá: phần trăm hoặc số tiền");
        }

        return promotionRepository.save(promotion);
    }

    public Promotion updatePromotion(Long id, Promotion promotionDetails) {
        Promotion promotion = promotionRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Khuyến mãi không tồn tại"));

        // Validate dates
        if (promotionDetails.getStartDate().isAfter(promotionDetails.getEndDate())) {
            throw new RuntimeException("Ngày bắt đầu không thể sau ngày kết thúc");
        }

        // Validate discount values
        if (promotionDetails.getDiscountPercentage() != null && promotionDetails.getDiscountAmount() != null) {
            throw new RuntimeException("Chỉ có thể chọn một loại giảm giá: phần trăm hoặc số tiền");
        }

        promotion.setName(promotionDetails.getName());
        promotion.setDescription(promotionDetails.getDescription()); // ✅ THÊM DÒNG NÀY
        promotion.setDiscountPercentage(promotionDetails.getDiscountPercentage());
        promotion.setDiscountAmount(promotionDetails.getDiscountAmount());
        promotion.setStartDate(promotionDetails.getStartDate());
        promotion.setEndDate(promotionDetails.getEndDate());
        promotion.setIsActive(promotionDetails.getIsActive());

        return promotionRepository.save(promotion);
    }

    @Transactional
    public boolean deletePromotion(Long id) {
        try {
            if (promotionRepository.existsById(id)) {
                // Xóa tất cả các product liên kết với promotion
                promotionProductRepository.deleteByPromotionId(id);
                // Sau đó xóa promotion
                promotionRepository.deleteById(id);
                return true;
            }
            return false;
        } catch (Exception e) {
            throw new RuntimeException("Lỗi khi xóa khuyến mãi: " + e.getMessage());
        }
    }

    public Promotion getPromotionById(Long id) {
        return promotionRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Khuyến mãi không tồn tại"));
    }

    public List<Promotion> getAllPromotions() {
        return promotionRepository.findAll();
    }

    public List<Promotion> getActivePromotions() {
        return promotionRepository.findByIsActiveTrue();
    }

    public List<Promotion> getCurrentPromotions() {
        return promotionRepository.findActivePromotions(LocalDate.now());
    }

    // === STATUS MANAGEMENT ===
    public Promotion togglePromotionStatus(Long id, Boolean isActive) {
        Promotion promotion = promotionRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Khuyến mãi không tồn tại"));

        promotion.setIsActive(isActive);
        return promotionRepository.save(promotion);
    }

    // === PRODUCT MANAGEMENT ===
    public boolean addProductToPromotion(Long promotionId, Long productId) {
        Optional<Promotion> promotion = promotionRepository.findById(promotionId);
        Optional<Product> product = productRepository.findById(productId);

        if (promotion.isPresent() && product.isPresent()) {
            // Kiểm tra xem đã tồn tại chưa
            boolean exists = promotionProductRepository.existsByPromotionIdAndProductId(promotionId, productId);
            if (!exists) {
                PromotionProduct promotionProduct = new PromotionProduct(promotion.get(), product.get());
                promotionProductRepository.save(promotionProduct);
                return true;
            }
        }
        return false;
    }

    @Transactional
    public boolean addMultipleProductsToPromotion(Long promotionId, List<Long> productIds) {
        Optional<Promotion> promotion = promotionRepository.findById(promotionId);
        if (!promotion.isPresent()) {
            return false;
        }

        for (Long productId : productIds) {
            Optional<Product> product = productRepository.findById(productId);
            if (product.isPresent()) {
                // Kiểm tra xem đã tồn tại chưa
                boolean exists = promotionProductRepository.existsByPromotionIdAndProductId(promotionId, productId);
                if (!exists) {
                    PromotionProduct promotionProduct = new PromotionProduct(promotion.get(), product.get());
                    promotionProductRepository.save(promotionProduct);
                }
            }
        }
        return true;
    }

    public boolean removeProductFromPromotion(Long promotionId, Long productId) {
        promotionProductRepository.deleteByPromotionIdAndProductId(promotionId, productId);
        return true;
    }

    @Transactional
    public boolean removeMultipleProductsFromPromotion(Long promotionId, List<Long> productIds) {
        for (Long productId : productIds) {
            promotionProductRepository.deleteByPromotionIdAndProductId(promotionId, productId);
        }
        return true;
    }

    @Transactional
    public void clearAllProductsFromPromotion(Long promotionId) {
        promotionProductRepository.deleteByPromotionId(promotionId);
    }

    public List<PromotionProduct> getPromotionProducts(Long promotionId) {
        return promotionProductRepository.findByPromotionId(promotionId);
    }

    public List<Product> getProductsInPromotion(Long promotionId) {
        List<PromotionProduct> promotionProducts = promotionProductRepository.findByPromotionId(promotionId);
        return promotionProducts.stream()
                .map(PromotionProduct::getProduct)
                .collect(Collectors.toList());
    }

    public List<Product> getProductsNotInPromotion(Long promotionId) {
        // Lấy tất cả sản phẩm
        List<Product> allProducts = productRepository.findAll();

        // Lấy sản phẩm đã trong khuyến mãi
        List<Long> productIdsInPromotion = getProductsInPromotion(promotionId).stream()
                .map(Product::getId)
                .collect(Collectors.toList());

        // Lọc ra sản phẩm chưa trong khuyến mãi
        return allProducts.stream()
                .filter(product -> !productIdsInPromotion.contains(product.getId()))
                .collect(Collectors.toList());
    }

    // === VALIDATION METHODS ===
    public boolean isPromotionActive(Long promotionId) {
        Optional<Promotion> promotion = promotionRepository.findById(promotionId);
        if (promotion.isPresent()) {
            Promotion promo = promotion.get();
            LocalDate today = LocalDate.now();
            return promo.getIsActive() &&
                    !today.isBefore(promo.getStartDate()) &&
                    !today.isAfter(promo.getEndDate());
        }
        return false;
    }

    // === SEARCH METHODS ===
    public List<Promotion> searchPromotionsByName(String name) {
        return promotionRepository.findByNameContainingIgnoreCase(name);
    }
}