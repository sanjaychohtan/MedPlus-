package com.medsupply.platform.modules.warehouse.service;

import com.medsupply.platform.modules.warehouse.model.Warehouse;
import com.medsupply.platform.modules.warehouse.model.StockTransfer;
import java.util.List;
import java.util.UUID;

public interface WarehouseService {
    List<Warehouse> getAllWarehouses();
    Warehouse getWarehouseById(UUID id);
    Warehouse createWarehouse(Warehouse warehouse);
    
    List<StockTransfer> getAllStockTransfers();
    StockTransfer createStockTransfer(UUID fromId, UUID toId, UUID productId, UUID batchId, int quantity, String requestedBy, String notes);
    StockTransfer approveStockTransfer(UUID id);
}
