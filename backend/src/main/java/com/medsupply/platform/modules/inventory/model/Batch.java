package com.medsupply.platform.modules.inventory.model;

import com.medsupply.platform.common.model.BaseEntity;
import com.medsupply.platform.modules.warehouse.model.Warehouse;
import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * JPA Entity representing a physical medical batch lot in the inventory.
 * Designed for First-Expired-First-Out (FEFO) allocation.
 */
@Entity
@Table(name = "batches")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Batch extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "warehouse_id", nullable = false)
    private Warehouse warehouse;

    @Column(name = "batch_number", nullable = false, length = 100)
    private String batchNumber;

    @Column(name = "manufacturing_date", nullable = false)
    private LocalDate manufacturingDate;

    @Column(name = "expiry_date", nullable = false)
    private LocalDate expiryDate;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal mrp;

    @Column(name = "b2b_price", nullable = false, precision = 12, scale = 2)
    private BigDecimal b2bPrice;

    @Builder.Default
    @Column(name = "quantity_on_hand", nullable = false)
    private int quantityOnHand = 0;

    @Builder.Default
    @Column(name = "quantity_reserved", nullable = false)
    private int quantityReserved = 0;

    @Builder.Default
    @Column(name = "quantity_available", nullable = false)
    private int quantityAvailable = 0;

    @Builder.Default
    @Column(name = "cold_chain_monitored", nullable = false)
    private boolean coldChainMonitored = false;

    @Column(name = "temp_reading_celsius", precision = 5, scale = 2)
    private BigDecimal tempReadingCelsius;

    @Builder.Default
    @Column(nullable = false, length = 50)
    private String status = "ACTIVE";

    @PrePersist
    @PreUpdate
    public void calculateAvailableQuantity() {
        if ("QUARANTINED".equals(this.status)) {
            this.quantityAvailable = 0;
        } else {
            this.quantityAvailable = this.quantityOnHand - this.quantityReserved;
        }
    }
}
