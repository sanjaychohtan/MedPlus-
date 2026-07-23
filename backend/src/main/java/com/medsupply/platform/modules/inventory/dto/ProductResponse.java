package com.medsupply.platform.modules.inventory.dto;

import lombok.*;
import java.math.BigDecimal;
import java.util.UUID;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProductResponse {
    private UUID id;
    private String name;
    private String sku;
    private String hsnCode;
    private String description;
    private UUID categoryId;
    private String categoryName;
    private UUID brandId;
    private String brandName;
    private String unitOfMeasure;
    private BigDecimal b2cPrice;
    private BigDecimal b2bPriceTier1;
    private BigDecimal b2bPriceTier2;
    private BigDecimal mrp;
    private BigDecimal taxRatePercent;
    private boolean prescriptionRequired;
    private int minStockAlert;
    private String storageCondition;
    private String imageUrl;
}
