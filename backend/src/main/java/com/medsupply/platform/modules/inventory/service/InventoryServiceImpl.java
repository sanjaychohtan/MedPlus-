package com.medsupply.platform.modules.inventory.service;

import com.medsupply.platform.modules.inventory.dto.*;
import com.medsupply.platform.modules.inventory.model.*;
import com.medsupply.platform.modules.inventory.repository.*;
import com.medsupply.platform.modules.warehouse.model.Warehouse;
import com.medsupply.platform.modules.warehouse.repository.WarehouseRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class InventoryServiceImpl implements InventoryService {

    private final CategoryRepository categoryRepository;
    private final BrandRepository brandRepository;
    private final ProductRepository productRepository;
    private final BatchRepository batchRepository;
    private final WarehouseRepository warehouseRepository;

    @Override
    @Transactional(readOnly = true)
    public List<Category> getAllCategories() {
        return categoryRepository.findAll();
    }

    @Override
    public Category createCategory(String name, String description) {
        Category category = Category.builder()
                .name(name)
                .description(description)
                .build();
        return categoryRepository.save(category);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Brand> getAllBrands() {
        return brandRepository.findAll();
    }

    @Override
    public Brand createBrand(String name, String description) {
        Brand brand = Brand.builder()
                .name(name)
                .description(description)
                .build();
        return brandRepository.save(brand);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ProductResponse> getAllProducts() {
        return productRepository.findAll().stream()
                .filter(p -> !p.isDeleted())
                .map(this::mapToProductResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<ProductResponse> searchProducts(String query) {
        return productRepository.searchProducts(query).stream()
                .map(this::mapToProductResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public ProductResponse getProductById(UUID id) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Product not found with ID: " + id));
        return mapToProductResponse(product);
    }

    @Override
    public ProductResponse createProduct(ProductRequest request) {
        Category category = categoryRepository.findById(request.getCategoryId())
                .orElseThrow(() -> new IllegalArgumentException("Category not found with ID: " + request.getCategoryId()));
        Brand brand = brandRepository.findById(request.getBrandId())
                .orElseThrow(() -> new IllegalArgumentException("Brand not found with ID: " + request.getBrandId()));

        Product product = Product.builder()
                .name(request.getName())
                .sku(request.getSku())
                .hsnCode(request.getHsnCode())
                .description(request.getDescription())
                .category(category)
                .brand(brand)
                .unitOfMeasure(request.getUnitOfMeasure())
                .b2cPrice(request.getB2cPrice())
                .b2bPriceTier1(request.getB2bPriceTier1())
                .b2bPriceTier2(request.getB2bPriceTier2())
                .mrp(request.getMrp())
                .taxRatePercent(request.getTaxRatePercent())
                .prescriptionRequired(request.isPrescriptionRequired())
                .minStockAlert(request.getMinStockAlert())
                .storageCondition(request.getStorageCondition())
                .imageUrl(request.getImageUrl())
                .build();

        return mapToProductResponse(productRepository.save(product));
    }

    @Override
    public void deleteProduct(UUID id) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Product not found with ID: " + id));
        product.softDelete();
        productRepository.save(product);
    }

    @Override
    @Transactional(readOnly = true)
    public List<BatchResponse> getAllBatches() {
        return batchRepository.findAll().stream()
                .filter(b -> !b.isDeleted())
                .map(this::mapToBatchResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<BatchResponse> getFefoBatchesForProduct(UUID productId) {
        return batchRepository.findFefoBatchesForProduct(productId).stream()
                .map(this::mapToBatchResponse)
                .collect(Collectors.toList());
    }

    @Override
    public BatchResponse createBatch(BatchRequest request) {
        Product product = productRepository.findById(request.getProductId())
                .orElseThrow(() -> new IllegalArgumentException("Product not found with ID: " + request.getProductId()));
        Warehouse warehouse = warehouseRepository.findById(request.getWarehouseId())
                .orElseThrow(() -> new IllegalArgumentException("Warehouse not found with ID: " + request.getWarehouseId()));

        Batch batch = Batch.builder()
                .product(product)
                .warehouse(warehouse)
                .batchNumber(request.getBatchNumber())
                .manufacturingDate(request.getManufacturingDate())
                .expiryDate(request.getExpiryDate())
                .mrp(request.getMrp())
                .b2bPrice(request.getB2bPrice())
                .quantityOnHand(request.getQuantityOnHand())
                .quantityReserved(0)
                .coldChainMonitored(request.isColdChainMonitored())
                .tempReadingCelsius(request.getTempReadingCelsius())
                .status(request.getStatus() != null ? request.getStatus() : "ACTIVE")
                .build();

        batch.calculateAvailableQuantity();
        return mapToBatchResponse(batchRepository.save(batch));
    }

    @Override
    public BatchResponse updateBatchStock(UUID batchId, int newQuantity) {
        Batch batch = batchRepository.findById(batchId)
                .orElseThrow(() -> new IllegalArgumentException("Batch not found with ID: " + batchId));
        batch.setQuantityOnHand(newQuantity);
        batch.calculateAvailableQuantity();
        return mapToBatchResponse(batchRepository.save(batch));
    }

    @Override
    public BatchResponse quarantineBatch(UUID batchId) {
        Batch batch = batchRepository.findById(batchId)
                .orElseThrow(() -> new IllegalArgumentException("Batch not found with ID: " + batchId));
        batch.setStatus("QUARANTINED");
        return mapToBatchResponse(batchRepository.save(batch));
    }

    private ProductResponse mapToProductResponse(Product p) {
        return ProductResponse.builder()
                .id(p.getId())
                .name(p.getName())
                .sku(p.getSku())
                .hsnCode(p.getHsnCode())
                .description(p.getDescription())
                .categoryId(p.getCategory().getId())
                .categoryName(p.getCategory().getName())
                .brandId(p.getBrand().getId())
                .brandName(p.getBrand().getName())
                .unitOfMeasure(p.getUnitOfMeasure())
                .b2cPrice(p.getB2cPrice())
                .b2bPriceTier1(p.getB2bPriceTier1())
                .b2bPriceTier2(p.getB2bPriceTier2())
                .mrp(p.getMrp())
                .taxRatePercent(p.getTaxRatePercent())
                .prescriptionRequired(p.isPrescriptionRequired())
                .minStockAlert(p.getMinStockAlert())
                .storageCondition(p.getStorageCondition())
                .imageUrl(p.getImageUrl())
                .build();
    }

    private BatchResponse mapToBatchResponse(Batch b) {
        return BatchResponse.builder()
                .id(b.getId())
                .productId(b.getProduct().getId())
                .productName(b.getProduct().getName())
                .productSku(b.getProduct().getSku())
                .warehouseId(b.getWarehouse().getId())
                .warehouseName(b.getWarehouse().getName())
                .batchNumber(b.getBatchNumber())
                .manufacturingDate(b.getManufacturingDate())
                .expiryDate(b.getExpiryDate())
                .mrp(b.getMrp())
                .b2bPrice(b.getB2bPrice())
                .quantityOnHand(b.getQuantityOnHand())
                .quantityReserved(b.getQuantityReserved())
                .quantityAvailable(b.getQuantityAvailable())
                .coldChainMonitored(b.isColdChainMonitored())
                .tempReadingCelsius(b.getTempReadingCelsius())
                .status(b.getStatus())
                .build();
    }
}
