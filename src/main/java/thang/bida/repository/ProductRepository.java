package thang.bida.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import thang.bida.model.Product;
import java.util.List;
import java.util.Optional;

@Repository
public interface ProductRepository extends JpaRepository<Product, Long> {

    List<Product> findByCategoryId(Long categoryId);

    List<Product> findByActiveTrue();

    List<Product> findByNameContainingIgnoreCase(String name);

    List<Product> findByStockQuantityLessThan(Integer quantity);

    Long countByActiveTrue();

    // SỬA: Dùng productTypeCode thay vì productType
    List<Product> findByProductTypeCode(String productTypeCode);

    // SỬA: Dùng productTypeCode
    Optional<Product> findFirstByProductTypeCode(String productTypeCode);

    // SỬA: Dùng productTypeCode
    boolean existsByProductTypeCode(String productTypeCode);

    // SỬA: So sánh với productTypeCode thay vì productType
    @Query("SELECT p FROM Product p WHERE p.productTypeCode != 'TIME_BASED'")
    List<Product> findAllNonTimeBased();

    // SỬA: So sánh với productTypeCode
    @Query("SELECT p FROM Product p WHERE p.productTypeCode = 'TIME_BASED' AND p.active = true")
    Optional<Product> findActiveTimeBasedProduct();

    // Thêm method tìm theo productTypeCode và active
    List<Product> findByProductTypeCodeAndActiveTrue(String productTypeCode);

    // Thêm method tìm featured products
    List<Product> findByFeaturedTrueAndActiveTrue();
}