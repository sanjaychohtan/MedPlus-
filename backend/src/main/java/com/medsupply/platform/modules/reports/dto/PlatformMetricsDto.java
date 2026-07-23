package com.medsupply.platform.modules.reports.dto;

import lombok.Builder;
import lombok.Getter;
import java.math.BigDecimal;

@Getter
@Builder
public class PlatformMetricsDto {
    private long totalProducts;
    private long totalOrders;
    private long totalUsers;
    private BigDecimal totalInventoryValuation;
    private long pendingOrders;
    private long completedOrders;
}
