package com.medsupply.platform.modules.reports.dto;

import lombok.Builder;
import lombok.Getter;
import java.math.BigDecimal;
import java.util.Map;

@Getter
@Builder
public class SalesSummaryDto {
    private BigDecimal totalRevenue;
    private long totalOrdersCount;
    private Map<String, Long> ordersByStatus;
    private Map<String, BigDecimal> revenueByPaymentMethod;
}
