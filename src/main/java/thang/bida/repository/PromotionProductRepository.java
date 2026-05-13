package thang.bida.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional; // THÊM IMPORT NÀY

import thang.bida.model.PromotionProduct;

import java.util.List;

@Repository
public interface PromotionProductRepository extends JpaRepository<PromotionProduct, Long> {

        List<PromotionProduct> findByPromotionId(Long promotionId);

        List<PromotionProduct> findByProductId(Long productId);

        @Query("SELECT pp FROM PromotionProduct pp WHERE pp.promotion.id = :promotionId")
        List<PromotionProduct> findProductsByPromotionId(@Param("promotionId") Long promotionId);

        @Modifying
        @Transactional // THÊM DÒNG NÀY
        @Query("DELETE FROM PromotionProduct pp WHERE pp.promotion.id = :promotionId AND pp.product.id = :productId")
        void deleteByPromotionIdAndProductId(@Param("promotionId") Long promotionId,
                        @Param("productId") Long productId);

        @Modifying
        @Transactional // THÊM DÒNG NÀY - QUAN TRỌNG NHẤT
        @Query("DELETE FROM PromotionProduct pp WHERE pp.promotion.id = :promotionId")
        void deleteByPromotionId(@Param("promotionId") Long promotionId);

        @Query("SELECT CASE WHEN COUNT(pp) > 0 THEN true ELSE false END FROM PromotionProduct pp WHERE pp.promotion.id = :promotionId AND pp.product.id = :productId")
        boolean existsByPromotionIdAndProductId(@Param("promotionId") Long promotionId,
                        @Param("productId") Long productId);
}