package com.medsupply.platform.modules.warehouse.controller;

import com.medsupply.platform.common.dto.ApiResponse;
import com.medsupply.platform.modules.warehouse.model.Warehouse;
import com.medsupply.platform.modules.warehouse.model.StockTransfer;
import com.medsupply.platform.modules.warehouse.service.WarehouseService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/warehouses")
@RequiredArgsConstructor
@Tag(name = "Warehouse Management", description = "Endpoints handling stock storage nodes and FEFO inter-depot transfers.")
public class WarehouseController {

    private final WarehouseService warehouseService;

    @GetMapping
    @Operation(summary = "Get all warehouses")
    public ResponseEntity<ApiResponse<List<Warehouse>>> getAllWarehouses() {
        return ResponseEntity.ok(ApiResponse.success(warehouseService.getAllWarehouses(), "Warehouses retrieved"));
    }

    @PostMapping
    @Operation(summary = "Register a new warehouse node")
    public ResponseEntity<ApiResponse<Warehouse>> createWarehouse(@RequestBody Warehouse warehouse) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(warehouseService.createWarehouse(warehouse), "Warehouse registered"));
    }

    @GetMapping("/transfers")
    @Operation(summary = "Get all inter-depot stock transfers")
    public ResponseEntity<ApiResponse<List<StockTransfer>>> getAllStockTransfers() {
        return ResponseEntity.ok(ApiResponse.success(warehouseService.getAllStockTransfers(), "Stock transfers retrieved"));
    }

    @PostMapping("/transfers")
    @Operation(summary = "Propose a new stock transfer")
    public ResponseEntity<ApiResponse<StockTransfer>> createStockTransfer(
            @RequestParam UUID fromId,
            @RequestParam UUID toId,
            @RequestParam UUID productId,
            @RequestParam UUID batchId,
            @RequestParam int quantity,
            @RequestParam String requestedBy,
            @RequestParam(required = false) String notes) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(warehouseService.createStockTransfer(fromId, toId, productId, batchId, quantity, requestedBy, notes), "Transfer proposed"));
    }

    @PutMapping("/transfers/{id}/approve")
    @Operation(summary = "Approve and execute stock transfer")
    public ResponseEntity<ApiResponse<StockTransfer>> approveStockTransfer(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.success(warehouseService.approveStockTransfer(id), "Transfer executed successfully"));
    }
}
