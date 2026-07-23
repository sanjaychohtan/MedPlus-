package com.medsupply.platform.modules.warehouse.repository;

import com.medsupply.platform.modules.warehouse.model.StockTransfer;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.UUID;

@Repository
public interface StockTransferRepository extends JpaRepository<StockTransfer, UUID> {
    List<StockTransfer> findByFromWarehouseIdOrToWarehouseIdAndIsDeletedFalse(UUID fromWarehouseId, UUID toWarehouseId);
}
