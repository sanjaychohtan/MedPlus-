package com.medsupply.platform.modules.inventory.controller;

import com.medsupply.platform.common.dto.ApiResponse;
import com.medsupply.platform.modules.inventory.dto.*;
import com.medsupply.platform.modules.inventory.model.Category;
import com.medsupply.platform.modules.inventory.model.Brand;
import com.medsupply.platform.modules.inventory.service.InventoryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/inventory")
@RequiredArgsConstructor
@Tag(name = "Inventory Management", description = "Endpoints handling products, FEFO batches, categories, and pharmaceutical brands.")
public class InventoryController {

    private final InventoryService inventoryService;

    // Categories
    @GetMapping("/categories")
    @Operation(summary = "Get all categories")
    public ResponseEntity<ApiResponse<List<Category>>> getAllCategories() {
        return ResponseEntity.ok(ApiResponse.success(inventoryService.getAllCategories(), "Categories retrieved"));
    }

    @PostMapping("/categories")
    @Operation(summary = "Create a new category")
    public ResponseEntity<ApiResponse<Category>> createCategory(@RequestParam String name, @RequestParam(required = false) String description) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(inventoryService.createCategory(name, description), "Category created"));
    }

    // Brands
    @GetMapping("/brands")
    @Operation(summary = "Get all brands")
    public ResponseEntity<ApiResponse<List<Brand>>> getAllBrands() {
        return ResponseEntity.ok(ApiResponse.success(inventoryService.getAllBrands(), "Brands retrieved"));
    }

    @PostMapping("/brands")
    @Operation(summary = "Create a new brand")
    public ResponseEntity<ApiResponse<Brand>> createBrand(@RequestParam String name, @RequestParam(required = false) String description) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(inventoryService.createBrand(name, description), "Brand created"));
    }

    // Products
    @GetMapping("/products")
    @Operation(summary = "Get all products")
    public ResponseEntity<ApiResponse<List<ProductResponse>>> getAllProducts() {
        return ResponseEntity.ok(ApiResponse.success(inventoryService.getAllProducts(), "Products retrieved"));
    }

    @GetMapping("/products/search")
    @Operation(summary = "Search products by SKU, name, or HSN")
    public ResponseEntity<ApiResponse<List<ProductResponse>>> searchProducts(@RequestParam String q) {
        return ResponseEntity.ok(ApiResponse.success(inventoryService.searchProducts(q), "Search results retrieved"));
    }

    @GetMapping("/products/{id}")
    @Operation(summary = "Get product by ID")
    public ResponseEntity<ApiResponse<ProductResponse>> getProductById(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.success(inventoryService.getProductById(id), "Product retrieved"));
    }

    @PostMapping("/products")
    @Operation(summary = "Register a new medical product")
    public ResponseEntity<ApiResponse<ProductResponse>> createProduct(@Valid @RequestBody ProductRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(inventoryService.createProduct(request), "Product created successfully"));
    }

    @DeleteMapping("/products/{id}")
    @Operation(summary = "Delete product")
    public ResponseEntity<ApiResponse<Void>> deleteProduct(@PathVariable UUID id) {
        inventoryService.deleteProduct(id);
        return ResponseEntity.ok(ApiResponse.success(null, "Product deleted successfully"));
    }

    // Batches
    @GetMapping("/batches")
    @Operation(summary = "Get all FEFO inventory batches")
    public ResponseEntity<ApiResponse<List<BatchResponse>>> getAllBatches() {
        return ResponseEntity.ok(ApiResponse.success(inventoryService.getAllBatches(), "Batches retrieved"));
    }

    @GetMapping("/batches/fefo/{productId}")
    @Operation(summary = "Get active FEFO batches sorted by expiry for a product")
    public ResponseEntity<ApiResponse<List<BatchResponse>>> getFefoBatchesForProduct(@PathVariable UUID productId) {
        return ResponseEntity.ok(ApiResponse.success(inventoryService.getFefoBatchesForProduct(productId), "FEFO batches retrieved"));
    }

    @PostMapping("/batches")
    @Operation(summary = "Register a new batch lot")
    public ResponseEntity<ApiResponse<BatchResponse>> createBatch(@Valid @RequestBody BatchRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(inventoryService.createBatch(request), "Batch registered successfully"));
    }

    @PutMapping("/batches/{id}/stock")
    @Operation(summary = "Update batch quantity on hand")
    public ResponseEntity<ApiResponse<BatchResponse>> updateBatchStock(@PathVariable UUID id, @RequestParam int quantity) {
        return ResponseEntity.ok(ApiResponse.success(inventoryService.updateBatchStock(id, quantity), "Batch stock updated"));
    }

    @PutMapping("/batches/{id}/quarantine")
    @Operation(summary = "Quarantine active batch for safety violations")
    public ResponseEntity<ApiResponse<BatchResponse>> quarantineBatch(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.success(inventoryService.quarantineBatch(id), "Batch quarantined"));
    }
}
