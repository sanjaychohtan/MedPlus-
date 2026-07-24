package com.medsupply.platform.modules.warehouse.controller;

import com.medsupply.platform.common.dto.ApiResponse;
import com.medsupply.platform.modules.warehouse.model.Warehouse;
import com.medsupply.platform.modules.warehouse.model.StockTransfer;
import com.medsupply.platform.modules.warehouse.service.WarehouseService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/warehouses")
@RequiredArgsConstructor
@Validated
@Tag(name = "Warehouse Management", description = "Endpoints handling stock storage nodes and FEFO inter-depot transfers.")
public class WarehouseController {

    private final WarehouseService warehouseService;

    @GetMapping
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN', 'WAREHOUSE_STAFF', 'SALESMAN')")
    @Operation(summary = "Get all warehouses")
    public ResponseEntity<ApiResponse<List<Warehouse>>> getAllWarehouses() {
        return ResponseEntity.ok(ApiResponse.success(warehouseService.getAllWarehouses(), "Warehouses retrieved"));
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN')")
    @Operation(summary = "Register a new warehouse node")
    public ResponseEntity<ApiResponse<Warehouse>> createWarehouse(@RequestBody Warehouse warehouse) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(warehouseService.createWarehouse(warehouse), "Warehouse registered"));
    }

    @GetMapping("/transfers")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN', 'WAREHOUSE_STAFF')")
    @Operation(summary = "Get all inter-depot stock transfers")
    public ResponseEntity<ApiResponse<List<StockTransfer>>> getAllStockTransfers() {
        return ResponseEntity.ok(ApiResponse.success(warehouseService.getAllStockTransfers(), "Stock transfers retrieved"));
    }

    @PostMapping("/transfers")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN', 'WAREHOUSE_STAFF')")
    @Operation(summary = "Propose a new stock transfer")
    public ResponseEntity<ApiResponse<StockTransfer>> createStockTransfer(
            @RequestParam @NotNull UUID fromId,
            @RequestParam @NotNull UUID toId,
            @RequestParam @NotNull UUID productId,
            @RequestParam @NotNull UUID batchId,
            @RequestParam @Min(1) int quantity,
            @RequestParam(required = false) String notes) {
        
        String requestedBy = "anonymous";
        org.springframework.security.core.Authentication auth = org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.isAuthenticated()) {
            requestedBy = auth.getName();
        }

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(warehouseService.createStockTransfer(fromId, toId, productId, batchId, quantity, requestedBy, notes), "Transfer proposed"));
    }

    @PutMapping("/transfers/{id}/approve")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN', 'WAREHOUSE_STAFF')")
    @Operation(summary = "Approve and execute stock transfer")
    public ResponseEntity<ApiResponse<StockTransfer>> approveStockTransfer(@PathVariable @NotNull UUID id) {
        return ResponseEntity.ok(ApiResponse.success(warehouseService.approveStockTransfer(id), "Transfer executed successfully"));
    }

    @PutMapping("/transfers/{id}/reject")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN', 'WAREHOUSE_STAFF')")
    @Operation(summary = "Reject stock transfer")
    public ResponseEntity<ApiResponse<StockTransfer>> rejectStockTransfer(@PathVariable @NotNull UUID id) {
        return ResponseEntity.ok(ApiResponse.success(warehouseService.rejectStockTransfer(id), "Transfer rejected successfully"));
    }

    @PutMapping("/transfers/{id}/cancel")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN', 'WAREHOUSE_STAFF')")
    @Operation(summary = "Cancel stock transfer")
    public ResponseEntity<ApiResponse<StockTransfer>> cancelStockTransfer(@PathVariable @NotNull UUID id) {
        return ResponseEntity.ok(ApiResponse.success(warehouseService.cancelStockTransfer(id), "Transfer cancelled successfully"));
    }
}
