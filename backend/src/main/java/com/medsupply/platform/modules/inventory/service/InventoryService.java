package com.medsupply.platform.modules.inventory.service;

import com.medsupply.platform.modules.inventory.dto.*;
import com.medsupply.platform.modules.inventory.model.Category;
import com.medsupply.platform.modules.inventory.model.Brand;
import java.util.List;
import java.util.UUID;

public interface InventoryService {
    
    // Categories & Brands
    List<Category> getAllCategories();
    Category createCategory(String name, String description);
    List<Brand> getAllBrands();
    Brand createBrand(String name, String description);

    // Products
    List<ProductResponse> getAllProducts();
    List<ProductResponse> searchProducts(String query);
    ProductResponse getProductById(UUID id);
    ProductResponse createProduct(ProductRequest request);
    void deleteProduct(UUID id);

    // Batches
    List<BatchResponse> getAllBatches();
    List<BatchResponse> getFefoBatchesForProduct(UUID productId);
    BatchResponse createBatch(BatchRequest request);
    BatchResponse updateBatchStock(UUID batchId, int newQuantity);
    BatchResponse quarantineBatch(UUID batchId);
}
