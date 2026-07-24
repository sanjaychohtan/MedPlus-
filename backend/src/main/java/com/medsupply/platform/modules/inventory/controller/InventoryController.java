package com.medsupply.platform.modules.inventory.controller;

import com.medsupply.platform.common.dto.ApiResponse;
import com.medsupply.platform.modules.inventory.dto.*;
import com.medsupply.platform.modules.inventory.model.Category;
import com.medsupply.platform.modules.inventory.model.Brand;
import com.medsupply.platform.modules.inventory.service.InventoryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
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
@RequestMapping("/api/inventory")
@RequiredArgsConstructor
@Validated
@Tag(name = "Inventory Management", description = "Endpoints handling products, FEFO batches, categories, and pharmaceutical brands.")
public class InventoryController {

    private final InventoryService inventoryService;

    // Categories
    @GetMapping("/categories")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN', 'WAREHOUSE_STAFF', 'SALESMAN', 'DELIVERY_BOY', 'B2B_CUSTOMER', 'B2C_CUSTOMER')")
    @Operation(summary = "Get all categories")
    public ResponseEntity<ApiResponse<List<Category>>> getAllCategories() {
        return ResponseEntity.ok(ApiResponse.success(inventoryService.getAllCategories(), "Categories retrieved"));
    }

    @PostMapping("/categories")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN', 'WAREHOUSE_STAFF')")
    @Operation(summary = "Create a new category")
    public ResponseEntity<ApiResponse<Category>> createCategory(
            @RequestParam @NotBlank String name,
            @RequestParam(required = false) String description) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(inventoryService.createCategory(name, description), "Category created"));
    }

    // Brands
    @GetMapping("/brands")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN', 'WAREHOUSE_STAFF', 'SALESMAN', 'DELIVERY_BOY', 'B2B_CUSTOMER', 'B2C_CUSTOMER')")
    @Operation(summary = "Get all brands")
    public ResponseEntity<ApiResponse<List<Brand>>> getAllBrands() {
        return ResponseEntity.ok(ApiResponse.success(inventoryService.getAllBrands(), "Brands retrieved"));
    }

    @PostMapping("/brands")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN', 'WAREHOUSE_STAFF')")
    @Operation(summary = "Create a new brand")
    public ResponseEntity<ApiResponse<Brand>> createBrand(
            @RequestParam @NotBlank String name,
            @RequestParam(required = false) String description) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(inventoryService.createBrand(name, description), "Brand created"));
    }

    // Products
    @GetMapping("/products")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN', 'WAREHOUSE_STAFF', 'SALESMAN', 'DELIVERY_BOY', 'B2B_CUSTOMER', 'B2C_CUSTOMER')")
    @Operation(summary = "Get all products")
    public ResponseEntity<ApiResponse<List<ProductResponse>>> getAllProducts() {
        return ResponseEntity.ok(ApiResponse.success(inventoryService.getAllProducts(), "Products retrieved"));
    }

    @GetMapping("/products/search")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN', 'WAREHOUSE_STAFF', 'SALESMAN', 'DELIVERY_BOY', 'B2B_CUSTOMER', 'B2C_CUSTOMER')")
    @Operation(summary = "Search products by SKU, name, or HSN")
    public ResponseEntity<ApiResponse<List<ProductResponse>>> searchProducts(@RequestParam @NotBlank String q) {
        return ResponseEntity.ok(ApiResponse.success(inventoryService.searchProducts(q), "Search results retrieved"));
    }

    @GetMapping("/products/{id}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN', 'WAREHOUSE_STAFF', 'SALESMAN', 'DELIVERY_BOY', 'B2B_CUSTOMER', 'B2C_CUSTOMER')")
    @Operation(summary = "Get product by ID")
    public ResponseEntity<ApiResponse<ProductResponse>> getProductById(@PathVariable @NotNull UUID id) {
        return ResponseEntity.ok(ApiResponse.success(inventoryService.getProductById(id), "Product retrieved"));
    }

    @PostMapping("/products")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN', 'WAREHOUSE_STAFF')")
    @Operation(summary = "Register a new medical product")
    public ResponseEntity<ApiResponse<ProductResponse>> createProduct(@Valid @RequestBody ProductRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(inventoryService.createProduct(request), "Product created successfully"));
    }

    @DeleteMapping("/products/{id}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN')")
    @Operation(summary = "Delete product")
    public ResponseEntity<ApiResponse<Void>> deleteProduct(@PathVariable @NotNull UUID id) {
        inventoryService.deleteProduct(id);
        return ResponseEntity.ok(ApiResponse.success(null, "Product deleted successfully"));
    }

    // Batches
    @GetMapping("/batches")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN', 'WAREHOUSE_STAFF', 'SALESMAN')")
    @Operation(summary = "Get all FEFO inventory batches")
    public ResponseEntity<ApiResponse<List<BatchResponse>>> getAllBatches() {
        return ResponseEntity.ok(ApiResponse.success(inventoryService.getAllBatches(), "Batches retrieved"));
    }

    @GetMapping("/batches/fefo/{productId}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN', 'WAREHOUSE_STAFF', 'SALESMAN', 'B2B_CUSTOMER', 'B2C_CUSTOMER')")
    @Operation(summary = "Get active FEFO batches sorted by expiry for a product")
    public ResponseEntity<ApiResponse<List<BatchResponse>>> getFefoBatchesForProduct(@PathVariable @NotNull UUID productId) {
        return ResponseEntity.ok(ApiResponse.success(inventoryService.getFefoBatchesForProduct(productId), "FEFO batches retrieved"));
    }

    @PostMapping("/batches")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN', 'WAREHOUSE_STAFF')")
    @Operation(summary = "Register a new batch lot")
    public ResponseEntity<ApiResponse<BatchResponse>> createBatch(@Valid @RequestBody BatchRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(inventoryService.createBatch(request), "Batch registered successfully"));
    }

    @PutMapping("/batches/{id}/stock")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN', 'WAREHOUSE_STAFF')")
    @Operation(summary = "Update batch quantity on hand")
    public ResponseEntity<ApiResponse<BatchResponse>> updateBatchStock(
            @PathVariable @NotNull UUID id,
            @RequestParam @Min(0) int quantity) {
        return ResponseEntity.ok(ApiResponse.success(inventoryService.updateBatchStock(id, quantity), "Batch stock updated"));
    }

    @PutMapping("/batches/{id}/quarantine")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN', 'WAREHOUSE_STAFF')")
    @Operation(summary = "Quarantine active batch for safety violations")
    public ResponseEntity<ApiResponse<BatchResponse>> quarantineBatch(@PathVariable @NotNull UUID id) {
        return ResponseEntity.ok(ApiResponse.success(inventoryService.quarantineBatch(id), "Batch quarantined"));
    }
}
