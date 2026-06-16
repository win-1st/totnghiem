package thang.bida.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "products", indexes = {
        @Index(name = "idx_product_category", columnList = "category_id"),
        @Index(name = "idx_product_type", columnList = "product_type"),
        @Index(name = "idx_product_active", columnList = "active"),
        @Index(name = "idx_product_name", columnList = "name")
})
@JsonIgnoreProperties({ "hibernateLazyInitializer", "handler" })
public class Product {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "name", length = 100, nullable = false)
    private String name;

    @Column(name = "slug", length = 100)
    private String slug;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Column(name = "price", precision = 10, scale = 2)
    private BigDecimal price;

    @Column(name = "sale_price", precision = 10, scale = 2)
    private BigDecimal salePrice;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id")
    @JsonIgnoreProperties({ "hibernateLazyInitializer", "handler" })
    private Category category;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_type_id")
    @JsonIgnoreProperties({ "hibernateLazyInitializer", "handler" })
    private ProductType productType;

    @Column(name = "product_type")
    private String productTypeCode = "FOOD";

    @Column(name = "image_url", length = 255)
    private String imageUrl;

    @Column(name = "image_public_id", length = 255)
    private String imagePublicId;

    @Column(name = "stock_quantity")
    private Integer stockQuantity = 0;

    @Column(name = "min_stock")
    private Integer minStock = 0;

    @Column(name = "unit", length = 20)
    private String unit = "cái";

    @Column(name = "active")
    private Boolean active = true;

    @Column(name = "featured")
    private Boolean featured = false;

    @Column(name = "price_per_minute", precision = 10, scale = 2)
    private BigDecimal pricePerMinute;

    @Column(name = "sort_order")
    private Integer sortOrder = 0;

    @OneToMany(mappedBy = "product", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @JsonIgnoreProperties({ "hibernateLazyInitializer", "handler" })
    private List<OrderItem> orderItems = new ArrayList<>();

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @OneToMany(mappedBy = "product", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<InventoryTransaction> inventoryTransactions = new ArrayList<>();

    // Constructor với các field cần thiết
    public Product(String name, BigDecimal price, Category category) {
        this.name = name;
        this.price = price;
        this.category = category;
        this.slug = generateSlug(name);
    }

    public Product(String name, String description, BigDecimal price, Category category) {
        this.name = name;
        this.description = description;
        this.price = price;
        this.category = category;
        this.slug = generateSlug(name);
    }

    // Helper methods
    private String generateSlug(String name) {
        if (name == null)
            return null;
        return name.toLowerCase()
                .trim()
                .replaceAll("[^a-z0-9\\s-]", "")
                .replaceAll("\\s+", "-")
                .replaceAll("-+", "-");
    }

    public boolean isTimeBased() {
        return "TIME_BASED".equals(this.productTypeCode);
    }

    public boolean hasDiscount() {
        return salePrice != null && salePrice.compareTo(BigDecimal.ZERO) > 0
                && salePrice.compareTo(price) < 0;
    }

    public BigDecimal getCurrentPrice() {
        if (hasDiscount()) {
            return salePrice;
        }
        return price;
    }

    public BigDecimal getDiscountPercent() {
        if (!hasDiscount() || price == null || price.compareTo(BigDecimal.ZERO) == 0) {
            return BigDecimal.ZERO;
        }
        return BigDecimal.valueOf(100)
                .subtract(salePrice.multiply(BigDecimal.valueOf(100))
                        .divide(price, 2, java.math.RoundingMode.HALF_UP));
    }

    public boolean isInStock() {
        if (isTimeBased())
            return true;
        return stockQuantity != null && stockQuantity > 0;
    }

    public boolean isLowStock() {
        if (isTimeBased())
            return false;
        return stockQuantity != null && minStock != null && stockQuantity <= minStock;
    }

    public void decreaseStock(int quantity) {
        if (isTimeBased())
            return;
        if (this.stockQuantity != null && this.stockQuantity >= quantity) {
            this.stockQuantity -= quantity;
        } else {
            throw new RuntimeException("Sản phẩm '" + name + "' không đủ số lượng tồn kho");
        }
    }

    public void increaseStock(int quantity) {
        if (!isTimeBased() && this.stockQuantity != null) {
            this.stockQuantity += quantity;
        }
    }

    // Setter cho name để tự động tạo slug
    public void setName(String name) {
        this.name = name;
        this.slug = generateSlug(name);
    }

    // Setter cho productType để tự động cập nhật productTypeCode
    public void setProductType(ProductType productType) {
        this.productType = productType;
        if (productType != null) {
            this.productTypeCode = productType.getCode();
        }
    }
}