package com.medsupply.platform.modules.inventory.dto;

import jakarta.validation.constraints.*;
import lombok.*;
import java.math.BigDecimal;
import java.util.UUID;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProductRequest {

    @NotBlank(message = "Product name is required")
    private String name;

    @NotBlank(message = "SKU code is required")
    private String sku;

    @NotBlank(message = "HSN tax code is required")
    private String hsnCode;

    private String description;

    @NotNull(message = "Category ID is required")
    private UUID categoryId;

    @NotNull(message = "Brand ID is required")
    private UUID brandId;

    @Builder.Default
    private String unitOfMeasure = "BOX";

    @NotNull(message = "B2C price is required")
    @DecimalMin(value = "0.00", message = "Price cannot be negative")
    private BigDecimal b2cPrice;

    @NotNull(message = "B2B Tier 1 price is required")
    @DecimalMin(value = "0.00", message = "Price cannot be negative")
    private BigDecimal b2bPriceTier1;

    @NotNull(message = "B2B Tier 2 price is required")
    @DecimalMin(value = "0.00", message = "Price cannot be negative")
    private BigDecimal b2bPriceTier2;

    @NotNull(message = "MRP is required")
    @DecimalMin(value = "0.00", message = "MRP cannot be negative")
    private BigDecimal mrp;

    @Builder.Default
    private BigDecimal taxRatePercent = new BigDecimal("12.00");

    private boolean prescriptionRequired;

    @Min(value = 0, message = "Minimum stock alert cannot be negative")
    private int minStockAlert;

    @Builder.Default
    private String storageCondition = "ROOM_TEMP";

    private String imageUrl;
}
