package com.medsupply.platform.modules.inventory.repository;

import com.medsupply.platform.modules.inventory.model.Batch;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.UUID;

@Repository
public interface BatchRepository extends JpaRepository<Batch, UUID> {

    @Query("SELECT b FROM Batch b WHERE b.isDeleted = false AND b.product.id = :productId " +
           "AND b.status = 'ACTIVE' AND b.quantityAvailable > 0 ORDER BY b.expiryDate ASC")
    List<Batch> findFefoBatchesForProduct(@Param("productId") UUID productId);

    List<Batch> findByWarehouseIdAndIsDeletedFalse(UUID warehouseId);

    List<Batch> findByProductIdAndIsDeletedFalse(UUID productId);

    List<Batch> findByStatusAndIsDeletedFalse(String status);
}
