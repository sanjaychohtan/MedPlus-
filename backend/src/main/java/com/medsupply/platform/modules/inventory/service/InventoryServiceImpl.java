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
    private final com.medsupply.platform.modules.audit.service.AuditLogService auditLogService;

    @org.springframework.beans.factory.annotation.Autowired(required = false)
    private com.medsupply.platform.modules.auth.repository.UserRepository userRepository;

    @org.springframework.beans.factory.annotation.Autowired(required = false)
    private jakarta.servlet.http.HttpServletRequest httpServletRequest;

    private com.medsupply.platform.modules.auth.model.User getCurrentUser() {
        if (userRepository == null) return null;
        try {
            org.springframework.security.core.Authentication auth = org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication();
            if (auth != null && auth.isAuthenticated() && !"anonymousUser".equals(auth.getName())) {
                return userRepository.findByEmail(auth.getName()).orElse(null);
            }
        } catch (Exception e) {
            // handle exception gracefully
        }
        return null;
    }

    private String getCurrentUserRole(com.medsupply.platform.modules.auth.model.User user) {
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

    @Override
    @Transactional(readOnly = true)
    public List<Category> getAllCategories() {
        return categoryRepository.findAll();
    }

    @Override
    public Category createCategory(String name, String description) {
        if (categoryRepository.existsByNameIgnoreCaseAndIsDeletedFalse(name)) {
            throw new com.medsupply.platform.common.exception.DomainException("DUPLICATE_CATEGORY", "Category with name already exists: " + name);
        }
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
        if (brandRepository.existsByNameIgnoreCaseAndIsDeletedFalse(name)) {
            throw new com.medsupply.platform.common.exception.DomainException("DUPLICATE_BRAND", "Brand with name already exists: " + name);
        }
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
    public org.springframework.data.domain.Page<ProductResponse> getAllProducts(org.springframework.data.domain.Pageable pageable) {
        return productRepository.findByIsDeletedFalse(pageable)
                .map(this::mapToProductResponse);
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
                .orElseThrow(() -> new com.medsupply.platform.common.exception.DomainException("PRODUCT_NOT_FOUND", "Product not found with ID: " + id, org.springframework.http.HttpStatus.NOT_FOUND));
        return mapToProductResponse(product);
    }

    @Override
    public ProductResponse createProduct(ProductRequest request) {
        Category category = categoryRepository.findById(request.getCategoryId())
                .orElseThrow(() -> new com.medsupply.platform.common.exception.DomainException("CATEGORY_NOT_FOUND", "Category not found with ID: " + request.getCategoryId(), org.springframework.http.HttpStatus.NOT_FOUND));
        if (category.isDeleted()) {
            throw new com.medsupply.platform.common.exception.DomainException("DELETED_CATEGORY", "Cannot create product. Category is deleted/inactive.");
        }

        Brand brand = brandRepository.findById(request.getBrandId())
                .orElseThrow(() -> new com.medsupply.platform.common.exception.DomainException("BRAND_NOT_FOUND", "Brand not found with ID: " + request.getBrandId(), org.springframework.http.HttpStatus.NOT_FOUND));
        if (brand.isDeleted()) {
            throw new com.medsupply.platform.common.exception.DomainException("DELETED_BRAND", "Cannot create product. Brand is deleted/inactive.");
        }

        if (productRepository.existsBySkuIgnoreCaseAndIsDeletedFalse(request.getSku())) {
            throw new com.medsupply.platform.common.exception.DomainException("DUPLICATE_SKU", "Product SKU already exists and is active: " + request.getSku());
        }

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

        Product savedProduct = productRepository.save(product);

        // Audit Logging
        com.medsupply.platform.modules.auth.model.User currentUser = getCurrentUser();
        String currentRole = getCurrentUserRole(currentUser);
        UUID executorId = currentUser != null ? currentUser.getId() : null;
        auditLogService.log(executorId, currentRole, "PRODUCT_CREATED", "INVENTORY", 
                "Product '" + product.getName() + "' with SKU '" + product.getSku() + "' created successfully", getClientIp());

        return mapToProductResponse(savedProduct);
    }

    @Override
    public void deleteProduct(UUID id) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new com.medsupply.platform.common.exception.DomainException("PRODUCT_NOT_FOUND", "Product not found with ID: " + id, org.springframework.http.HttpStatus.NOT_FOUND));
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
    public org.springframework.data.domain.Page<BatchResponse> getAllBatches(org.springframework.data.domain.Pageable pageable) {
        return batchRepository.findByIsDeletedFalse(pageable)
                .map(this::mapToBatchResponse);
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
                .orElseThrow(() -> new com.medsupply.platform.common.exception.DomainException("PRODUCT_NOT_FOUND", "Product not found with ID: " + request.getProductId(), org.springframework.http.HttpStatus.NOT_FOUND));
        if (product.isDeleted()) {
            throw new com.medsupply.platform.common.exception.DomainException("DELETED_PRODUCT", "Cannot create batch. Product is deleted/inactive.");
        }

        Warehouse warehouse = warehouseRepository.findById(request.getWarehouseId())
                .orElseThrow(() -> new com.medsupply.platform.common.exception.DomainException("WAREHOUSE_NOT_FOUND", "Warehouse not found with ID: " + request.getWarehouseId(), org.springframework.http.HttpStatus.NOT_FOUND));
        if (warehouse.isDeleted()) {
            throw new com.medsupply.platform.common.exception.DomainException("DELETED_WAREHOUSE", "Cannot create batch. Warehouse is deleted/inactive.");
        }

        if (batchRepository.existsByBatchNumberIgnoreCaseAndIsDeletedFalse(request.getBatchNumber())) {
            throw new com.medsupply.platform.common.exception.DomainException("DUPLICATE_BATCH", "Batch number already exists: " + request.getBatchNumber());
        }

        if (request.getManufacturingDate().isAfter(request.getExpiryDate()) || request.getManufacturingDate().equals(request.getExpiryDate())) {
            throw new com.medsupply.platform.common.exception.DomainException("INVALID_DATES", "Manufacturing date must be before expiry date");
        }

        if (request.isColdChainMonitored()) {
            if (request.getTempReadingCelsius() == null) {
                throw new com.medsupply.platform.common.exception.DomainException("INVALID_TEMPERATURE", "Cold-chain monitored batch requires a temperature reading");
            }
        }

        if (request.getQuantityOnHand() < 0) {
            throw new com.medsupply.platform.common.exception.DomainException("INVALID_STOCK", "Quantity on hand cannot be negative");
        }

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
        Batch savedBatch = batchRepository.save(batch);

        // Audit Logging
        com.medsupply.platform.modules.auth.model.User currentUser = getCurrentUser();
        String currentRole = getCurrentUserRole(currentUser);
        UUID executorId = currentUser != null ? currentUser.getId() : null;
        auditLogService.log(executorId, currentRole, "BATCH_CREATED", "INVENTORY", 
                "Batch number '" + batch.getBatchNumber() + "' for product '" + product.getName() + "' created in warehouse '" + warehouse.getName() + "' with stock " + batch.getQuantityOnHand(), getClientIp());

        return mapToBatchResponse(savedBatch);
    }

    @Override
    public BatchResponse updateBatchStock(UUID batchId, int newQuantity) {
        Batch batch = batchRepository.findById(batchId)
                .orElseThrow(() -> new com.medsupply.platform.common.exception.DomainException("BATCH_NOT_FOUND", "Batch not found with ID: " + batchId, org.springframework.http.HttpStatus.NOT_FOUND));
        
        if (newQuantity < 0) {
            throw new com.medsupply.platform.common.exception.DomainException("INVALID_STOCK", "Quantity on hand cannot be negative");
        }
        if (newQuantity < batch.getQuantityReserved()) {
            throw new com.medsupply.platform.common.exception.DomainException("INSUFFICIENT_STOCK", "Quantity on hand (" + newQuantity + ") cannot be less than quantity reserved (" + batch.getQuantityReserved() + ")");
        }

        int oldQty = batch.getQuantityOnHand();
        batch.setQuantityOnHand(newQuantity);
        batch.calculateAvailableQuantity();
        Batch savedBatch = batchRepository.save(batch);

        // Audit Logging
        com.medsupply.platform.modules.auth.model.User currentUser = getCurrentUser();
        String currentRole = getCurrentUserRole(currentUser);
        UUID executorId = currentUser != null ? currentUser.getId() : null;
        auditLogService.log(executorId, currentRole, "STOCK_UPDATED", "INVENTORY", 
                "Batch '" + batch.getBatchNumber() + "' stock updated from " + oldQty + " to " + newQuantity, getClientIp());

        return mapToBatchResponse(savedBatch);
    }

    @Override
    public BatchResponse quarantineBatch(UUID batchId) {
        Batch batch = batchRepository.findById(batchId)
                .orElseThrow(() -> new com.medsupply.platform.common.exception.DomainException("BATCH_NOT_FOUND", "Batch not found with ID: " + batchId, org.springframework.http.HttpStatus.NOT_FOUND));
        
        batch.setStatus("QUARANTINED");
        batch.calculateAvailableQuantity(); // Sets available quantity to 0 as status is QUARANTINED
        Batch savedBatch = batchRepository.save(batch);

        // Audit Logging
        com.medsupply.platform.modules.auth.model.User currentUser = getCurrentUser();
        String currentRole = getCurrentUserRole(currentUser);
        UUID executorId = currentUser != null ? currentUser.getId() : null;
        auditLogService.log(executorId, currentRole, "BATCH_QUARANTINED", "INVENTORY", 
                "Batch '" + batch.getBatchNumber() + "' quarantined successfully. Available quantity is now 0.", getClientIp());

        return mapToBatchResponse(savedBatch);
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
