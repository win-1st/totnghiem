package thang.bida.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "promotion_products", uniqueConstraints = {
        @UniqueConstraint(columnNames = { "promotion_id", "product_id" })
})
@Getter
@Setter
@NoArgsConstructor
@JsonIgnoreProperties({ "hibernateLazyInitializer", "handler" })
public class PromotionProduct {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "promotion_id")
    private Promotion promotion;

    @ManyToOne
    @JoinColumn(name = "product_id")
    private Product product;

    public PromotionProduct(Promotion promotion, Product product) {
        this.promotion = promotion;
        this.product = product;
    }
}