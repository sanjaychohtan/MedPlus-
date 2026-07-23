package com.medsupply.platform.modules.warehouse.service;

import com.medsupply.platform.modules.inventory.model.Batch;
import com.medsupply.platform.modules.inventory.model.Product;
import com.medsupply.platform.modules.inventory.repository.BatchRepository;
import com.medsupply.platform.modules.inventory.repository.ProductRepository;
import com.medsupply.platform.modules.warehouse.model.Warehouse;
import com.medsupply.platform.modules.warehouse.model.StockTransfer;
import com.medsupply.platform.modules.warehouse.repository.WarehouseRepository;
import com.medsupply.platform.modules.warehouse.repository.StockTransferRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional
public class WarehouseServiceImpl implements WarehouseService {

    private final WarehouseRepository warehouseRepository;
    private final StockTransferRepository stockTransferRepository;
    private final ProductRepository productRepository;
    private final BatchRepository batchRepository;

    @Override
    @Transactional(readOnly = true)
    public List<Warehouse> getAllWarehouses() {
        return warehouseRepository.findAll();
    }

    @Override
    @Transactional(readOnly = true)
    public Warehouse getWarehouseById(UUID id) {
        return warehouseRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Warehouse not found: " + id));
    }

    @Override
    public Warehouse createWarehouse(Warehouse warehouse) {
        return warehouseRepository.save(warehouse);
    }

    @Override
    @Transactional(readOnly = true)
    public List<StockTransfer> getAllStockTransfers() {
        return stockTransferRepository.findAll();
    }

    @Override
    public StockTransfer createStockTransfer(UUID fromId, UUID toId, UUID productId, UUID batchId, int quantity, String requestedBy, String notes) {
        Warehouse from = getWarehouseById(fromId);
        Warehouse to = getWarehouseById(toId);
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new IllegalArgumentException("Product not found"));
        Batch batch = batchRepository.findById(batchId)
                .orElseThrow(() -> new IllegalArgumentException("Batch not found"));

        if (batch.getQuantityAvailable() < quantity) {
            throw new IllegalArgumentException("Insufficient inventory available in this batch lot.");
        }

        // Generate unique transfer number
        String transferNumber = "TRF-" + System.currentTimeMillis();

        StockTransfer transfer = StockTransfer.builder()
                .transferNumber(transferNumber)
                .fromWarehouse(from)
                .toWarehouse(to)
                .product(product)
                .batch(batch)
                .quantity(quantity)
                .requestedBy(requestedBy)
                .notes(notes)
                .status("PENDING")
                .build();

        return stockTransferRepository.save(transfer);
    }

    @Override
    public StockTransfer approveStockTransfer(UUID id) {
        StockTransfer transfer = stockTransferRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Transfer not found"));

        if (!"PENDING".equals(transfer.getStatus())) {
            throw new IllegalStateException("Transfer is already in status: " + transfer.getStatus());
        }

        Batch srcBatch = transfer.getBatch();
        if (srcBatch.getQuantityAvailable() < transfer.getQuantity()) {
            throw new IllegalStateException("Source batch has insufficient stock to fulfill transfer.");
        }

        // Deduct from source batch
        srcBatch.setQuantityOnHand(srcBatch.getQuantityOnHand() - transfer.getQuantity());
        srcBatch.calculateAvailableQuantity();
        batchRepository.save(srcBatch);

        // Find or create a matching destination batch
        Batch destBatch = batchRepository.findByWarehouseIdAndIsDeletedFalse(transfer.getToWarehouse().getId()).stream()
                .filter(b -> b.getProduct().getId().equals(transfer.getProduct().getId()) && b.getBatchNumber().equals(srcBatch.getBatchNumber()))
                .findFirst()
                .orElse(null);

        if (destBatch == null) {
            destBatch = Batch.builder()
                    .product(transfer.getProduct())
                    .warehouse(transfer.getToWarehouse())
                    .batchNumber(srcBatch.getBatchNumber())
                    .manufacturingDate(srcBatch.getManufacturingDate())
                    .expiryDate(srcBatch.getExpiryDate())
                    .mrp(srcBatch.getMrp())
                    .b2bPrice(srcBatch.getB2bPrice())
                    .quantityOnHand(transfer.getQuantity())
                    .quantityReserved(0)
                    .coldChainMonitored(srcBatch.isColdChainMonitored())
                    .tempReadingCelsius(srcBatch.getTempReadingCelsius())
                    .status("ACTIVE")
                    .build();
        } else {
            destBatch.setQuantityOnHand(destBatch.getQuantityOnHand() + transfer.getQuantity());
        }
        destBatch.calculateAvailableQuantity();
        batchRepository.save(destBatch);

        transfer.setStatus("APPROVED");
        return stockTransferRepository.save(transfer);
    }
}
