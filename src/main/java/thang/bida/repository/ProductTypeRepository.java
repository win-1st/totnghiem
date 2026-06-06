// ProductTypeRepository.java
package thang.bida.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import thang.bida.model.ProductType;
import java.util.List;
import java.util.Optional;

@Repository
public interface ProductTypeRepository extends JpaRepository<ProductType, Long> {
    Optional<ProductType> findByCode(String code);

    List<ProductType> findByIsActiveTrueOrderBySortOrderAsc();

    boolean existsByCode(String code);
}