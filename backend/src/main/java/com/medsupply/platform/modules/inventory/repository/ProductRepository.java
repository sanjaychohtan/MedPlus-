package com.medsupply.platform.modules.inventory.repository;

import com.medsupply.platform.modules.inventory.model.Product;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.UUID;

@Repository
public interface ProductRepository extends JpaRepository<Product, UUID> {
    boolean existsBySkuIgnoreCaseAndIsDeletedFalse(String sku);

    org.springframework.data.domain.Page<Product> findByIsDeletedFalse(org.springframework.data.domain.Pageable pageable);

    @Query("SELECT p FROM Product p WHERE p.isDeleted = false AND " +
           "(LOWER(p.name) LIKE LOWER(CONCAT('%', :q, '%')) OR " +
           "LOWER(p.sku) LIKE LOWER(CONCAT('%', :q, '%')) OR " +
           "LOWER(p.hsnCode) LIKE LOWER(CONCAT('%', :q, '%')))")
    List<Product> searchProducts(@Param("q") String query);

    List<Product> findByCategoryIdAndIsDeletedFalse(UUID categoryId);
}
