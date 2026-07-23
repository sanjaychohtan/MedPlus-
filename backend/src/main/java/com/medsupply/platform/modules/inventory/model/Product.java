package com.medsupply.platform.modules.inventory.model;

import com.medsupply.platform.common.model.BaseEntity;
import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;

/**
 * JPA Entity representing a medical supply product in the catalog.
 */
@Entity
@Table(name = "products")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Product extends BaseEntity {

    @Column(nullable = false)
    private String name;

    @Column(nullable = false, unique = true, length = 100)
    private String sku;

    @Column(name = "hsn_code", nullable = false, length = 20)
    private String hsnCode;

    @Column(columnDefinition = "TEXT")
    private String description;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id", nullable = false)
    private Category category;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "brand_id", nullable = false)
    private Brand brand;

    @Builder.Default
    @Column(name = "unit_of_measure", nullable = false, length = 20)
    private String unitOfMeasure = "BOX";

    @Builder.Default
    @Column(name = "b2c_price", nullable = false, precision = 12, scale = 2)
    private BigDecimal b2cPrice = BigDecimal.ZERO;

    @Builder.Default
    @Column(name = "b2b_price_tier1", nullable = false, precision = 12, scale = 2)
    private BigDecimal b2bPriceTier1 = BigDecimal.ZERO;

    @Builder.Default
    @Column(name = "b2b_price_tier2", nullable = false, precision = 12, scale = 2)
    private BigDecimal b2bPriceTier2 = BigDecimal.ZERO;

    @Builder.Default
    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal mrp = BigDecimal.ZERO;

    @Builder.Default
    @Column(name = "tax_rate_percent", nullable = false, precision = 5, scale = 2)
    private BigDecimal taxRatePercent = new BigDecimal("12.00");

    @Builder.Default
    @Column(name = "prescription_required", nullable = false)
    private boolean prescriptionRequired = false;

    @Builder.Default
    @Column(name = "min_stock_alert", nullable = false)
    private int minStockAlert = 100;

    @Builder.Default
    @Column(name = "storage_condition", nullable = false, length = 50)
    private String storageCondition = "ROOM_TEMP";

    @Column(name = "image_url", length = 500)
    private String imageUrl;
}
