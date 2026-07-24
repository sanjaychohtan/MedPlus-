package com.medsupply.platform.modules.warehouse.service;

import com.medsupply.platform.common.exception.DomainException;
import com.medsupply.platform.modules.inventory.model.Batch;
import com.medsupply.platform.modules.inventory.model.Product;
import com.medsupply.platform.modules.inventory.repository.BatchRepository;
import com.medsupply.platform.modules.inventory.repository.ProductRepository;
import com.medsupply.platform.modules.warehouse.model.Warehouse;
import com.medsupply.platform.modules.warehouse.model.StockTransfer;
import com.medsupply.platform.modules.warehouse.model.WarehouseTransferStatus;
import com.medsupply.platform.modules.warehouse.repository.WarehouseRepository;
import com.medsupply.platform.modules.warehouse.repository.StockTransferRepository;
import com.medsupply.platform.modules.auth.model.User;
import com.medsupply.platform.modules.auth.repository.UserRepository;
import com.medsupply.platform.modules.audit.service.AuditLogService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
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
    private final UserRepository userRepository;
    private final AuditLogService auditLogService;
    private final jakarta.servlet.http.HttpServletRequest httpServletRequest;

    @Override
    @Transactional(readOnly = true)
    public List<Warehouse> getAllWarehouses() {
        return warehouseRepository.findAll();
    }

    @Override
    @Transactional(readOnly = true)
    public Warehouse getWarehouseById(UUID id) {
        Warehouse warehouse = warehouseRepository.findById(id)
                .orElseThrow(() -> new DomainException("WAREHOUSE_NOT_FOUND", "Warehouse not found: " + id, HttpStatus.NOT_FOUND));
        if (warehouse.isDeleted()) {
            throw new DomainException("WAREHOUSE_DELETED", "Warehouse is deleted/inactive: " + id, HttpStatus.BAD_REQUEST);
        }
        return warehouse;
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
        // Reject negative or zero quantity
        if (quantity <= 0) {
            throw new DomainException("INVALID_QUANTITY", "Stock transfer quantity must be greater than zero.", HttpStatus.BAD_REQUEST);
        }

        // Reject self-transfers
        if (fromId.equals(toId)) {
            throw new DomainException("SAME_WAREHOUSE_TRANSFER", "Source and destination warehouses must be different.", HttpStatus.BAD_REQUEST);
        }

        // Retrieve and validate active/non-deleted elements
        Warehouse from = getWarehouseById(fromId);
        Warehouse to = getWarehouseById(toId);

        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new DomainException("PRODUCT_NOT_FOUND", "Product not found: " + productId, HttpStatus.NOT_FOUND));
        if (product.isDeleted()) {
            throw new DomainException("PRODUCT_INACTIVE", "Product is deleted/inactive: " + productId, HttpStatus.BAD_REQUEST);
        }

        // Lock batch lot pessimistically to prevent race conditions during proposal/reservation
        Batch batch = batchRepository.findByIdWithLock(batchId)
                .orElseThrow(() -> new DomainException("BATCH_NOT_FOUND", "Batch not found: " + batchId, HttpStatus.NOT_FOUND));
        if (batch.isDeleted() || !"ACTIVE".equals(batch.getStatus())) {
            throw new DomainException("BATCH_INACTIVE", "Batch is deleted or inactive: " + batchId, HttpStatus.BAD_REQUEST);
        }

        if (batch.getQuantityAvailable() < quantity) {
            throw new DomainException("INSUFFICIENT_STOCK", "Insufficient available inventory in batch lot. Requested: " + quantity + ", Available: " + batch.getQuantityAvailable(), HttpStatus.BAD_REQUEST);
        }

        // Reserve inventory at transfer creation to prevent double allocation
        batch.setQuantityReserved(batch.getQuantityReserved() + quantity);
        batch.calculateAvailableQuantity();
        batchRepository.save(batch);

        // Generate unique enterprise-grade transfer number
        String transferNumber = "TRF-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();

        StockTransfer transfer = StockTransfer.builder()
                .transferNumber(transferNumber)
                .fromWarehouse(from)
                .toWarehouse(to)
                .product(product)
                .batch(batch)
                .quantity(quantity)
                .requestedBy(requestedBy)
                .notes(notes)
                .status(WarehouseTransferStatus.PENDING)
                .build();

        StockTransfer savedTransfer = stockTransferRepository.save(transfer);

        // Audit Logging
        User executor = getCurrentUser();
        UUID executorId = executor != null ? executor.getId() : null;
        String executorRole = getCurrentUserRole(executor);
        auditLogService.log(executorId, executorRole, "TRANSFER_CREATED", "WAREHOUSE",
                "Stock transfer " + transferNumber + " proposed from warehouse " + from.getName() + " to " + to.getName() + " for " + quantity + " units", getClientIp());

        return savedTransfer;
    }

    @Override
    public StockTransfer approveStockTransfer(UUID id) {
        // Fetch and lock StockTransfer to prevent duplicate concurrent approvals
        StockTransfer transfer = stockTransferRepository.findByIdWithLock(id)
                .orElseThrow(() -> new DomainException("TRANSFER_NOT_FOUND", "Transfer not found: " + id, HttpStatus.NOT_FOUND));

        if (transfer.getStatus() != WarehouseTransferStatus.PENDING) {
            throw new DomainException("DUPLICATE_APPROVAL", "Transfer is already finalized. Current status: " + transfer.getStatus(), HttpStatus.BAD_REQUEST);
        }

        // Lock source batch lot pessimistically to avoid stock underflow race conditions
        Batch srcBatch = batchRepository.findByIdWithLock(transfer.getBatch().getId())
                .orElseThrow(() -> new DomainException("BATCH_NOT_FOUND", "Source batch not found: " + transfer.getBatch().getId(), HttpStatus.NOT_FOUND));

        if (srcBatch.getQuantityOnHand() < transfer.getQuantity()) {
            throw new DomainException("INSUFFICIENT_STOCK", "Source batch has insufficient stock. OnHand: " + srcBatch.getQuantityOnHand(), HttpStatus.BAD_REQUEST);
        }

        // Deduct from source batch (releasing the reserved units)
        srcBatch.setQuantityReserved(srcBatch.getQuantityReserved() - transfer.getQuantity());
        srcBatch.setQuantityOnHand(srcBatch.getQuantityOnHand() - transfer.getQuantity());
        srcBatch.calculateAvailableQuantity();
        batchRepository.save(srcBatch);

        // Find or create matching destination batch (preserving manufacturing/expiry for FEFO compatibility)
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

        transfer.setStatus(WarehouseTransferStatus.APPROVED);
        StockTransfer savedTransfer = stockTransferRepository.save(transfer);

        // Audit Logging
        User executor = getCurrentUser();
        UUID executorId = executor != null ? executor.getId() : null;
        String executorRole = getCurrentUserRole(executor);
        auditLogService.log(executorId, executorRole, "TRANSFER_APPROVED", "WAREHOUSE",
                "Stock transfer " + transfer.getTransferNumber() + " approved and executed", getClientIp());

        return savedTransfer;
    }

    @Override
    public StockTransfer rejectStockTransfer(UUID id) {
        StockTransfer transfer = stockTransferRepository.findByIdWithLock(id)
                .orElseThrow(() -> new DomainException("TRANSFER_NOT_FOUND", "Transfer not found: " + id, HttpStatus.NOT_FOUND));

        if (transfer.getStatus() != WarehouseTransferStatus.PENDING) {
            throw new DomainException("INVALID_TRANSFER_STATE", "Only PENDING transfers can be rejected. Current status: " + transfer.getStatus(), HttpStatus.BAD_REQUEST);
        }

        // Release reserved inventory
        Batch srcBatch = batchRepository.findByIdWithLock(transfer.getBatch().getId())
                .orElseThrow(() -> new DomainException("BATCH_NOT_FOUND", "Source batch not found: " + transfer.getBatch().getId(), HttpStatus.NOT_FOUND));

        srcBatch.setQuantityReserved(Math.max(0, srcBatch.getQuantityReserved() - transfer.getQuantity()));
        srcBatch.calculateAvailableQuantity();
        batchRepository.save(srcBatch);

        transfer.setStatus(WarehouseTransferStatus.REJECTED);
        StockTransfer savedTransfer = stockTransferRepository.save(transfer);

        // Audit Logging
        User executor = getCurrentUser();
        UUID executorId = executor != null ? executor.getId() : null;
        String executorRole = getCurrentUserRole(executor);
        auditLogService.log(executorId, executorRole, "TRANSFER_REJECTED", "WAREHOUSE",
                "Stock transfer " + transfer.getTransferNumber() + " rejected", getClientIp());

        return savedTransfer;
    }

    @Override
    public StockTransfer cancelStockTransfer(UUID id) {
        StockTransfer transfer = stockTransferRepository.findByIdWithLock(id)
                .orElseThrow(() -> new DomainException("TRANSFER_NOT_FOUND", "Transfer not found: " + id, HttpStatus.NOT_FOUND));

        if (transfer.getStatus() != WarehouseTransferStatus.PENDING) {
            throw new DomainException("INVALID_TRANSFER_STATE", "Only PENDING transfers can be cancelled. Current status: " + transfer.getStatus(), HttpStatus.BAD_REQUEST);
        }

        // Release reserved inventory
        Batch srcBatch = batchRepository.findByIdWithLock(transfer.getBatch().getId())
                .orElseThrow(() -> new DomainException("BATCH_NOT_FOUND", "Source batch not found: " + transfer.getBatch().getId(), HttpStatus.NOT_FOUND));

        srcBatch.setQuantityReserved(Math.max(0, srcBatch.getQuantityReserved() - transfer.getQuantity()));
        srcBatch.calculateAvailableQuantity();
        batchRepository.save(srcBatch);

        transfer.setStatus(WarehouseTransferStatus.CANCELLED);
        StockTransfer savedTransfer = stockTransferRepository.save(transfer);

        // Audit Logging
        User executor = getCurrentUser();
        UUID executorId = executor != null ? executor.getId() : null;
        String executorRole = getCurrentUserRole(executor);
        auditLogService.log(executorId, executorRole, "TRANSFER_CANCELLED", "WAREHOUSE",
                "Stock transfer " + transfer.getTransferNumber() + " cancelled", getClientIp());

        return savedTransfer;
    }

    private User getCurrentUser() {
        if (userRepository == null) return null;
        try {
            org.springframework.security.core.Authentication auth = org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication();
            if (auth != null && auth.isAuthenticated() && !"anonymousUser".equals(auth.getName())) {
                return userRepository.findByEmail(auth.getName()).orElse(null);
            }
        } catch (Exception e) {
            // Log or ignore gracefully
        }
        return null;
    }

    private String getCurrentUserRole(User user) {
        if (user == null || user.getRoles() == null || user.getRoles().isEmpty()) {
            return "ANONYMOUS";
        }
        return "ROLE_" + user.getRoles().iterator().next().getName().name();
    }

    private String getClientIp() {
        if (httpServletRequest == null) return "127.0.0.1";
        try {
            String ipList = httpServletRequest.getHeader("X-Forwarded-For");
            if (ipList != null && !ipList.isEmpty()) {
                return ipList.split(",")[0].trim();
            }
            return httpServletRequest.getRemoteAddr();
        } catch (Exception e) {
            return "127.0.0.1";
        }
    }
}
