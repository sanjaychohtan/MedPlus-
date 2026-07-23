package com.medsupply.platform.modules.inventory.dto;

import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BatchResponse {
    private UUID id;
    private UUID productId;
    private String productName;
    private String productSku;
    private UUID warehouseId;
    private String warehouseName;
    private String batchNumber;
    private LocalDate manufacturingDate;
    private LocalDate expiryDate;
    private BigDecimal mrp;
    private BigDecimal b2bPrice;
    private int quantityOnHand;
    private int quantityReserved;
    private int quantityAvailable;
    private boolean coldChainMonitored;
    private BigDecimal tempReadingCelsius;
    private String status;
}
