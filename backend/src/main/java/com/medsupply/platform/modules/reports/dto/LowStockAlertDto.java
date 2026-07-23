package com.medsupply.platform.modules.reports.dto;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class LowStockAlertDto {
    private String productId;
    private String productName;
    private String sku;
    private int availableQuantity;
    private int safetyThreshold;
}
