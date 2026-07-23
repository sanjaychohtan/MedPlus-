package com.medsupply.platform.modules.reports.service;

import com.medsupply.platform.modules.reports.dto.LowStockAlertDto;
import com.medsupply.platform.modules.reports.dto.NearExpiryLotDto;
import com.medsupply.platform.modules.reports.dto.PlatformMetricsDto;
import com.medsupply.platform.modules.reports.dto.SalesSummaryDto;

import java.util.List;

public interface ReportsService {

    /**
     * Retrieves key platform metrics.
     */
    PlatformMetricsDto getPlatformMetrics();

    /**
     * Retrieves near-expiry lot reports (lots expiring within 90 days).
     */
    List<NearExpiryLotDto> getNearExpiryLots();

    /**
     * Retrieves products with quantities below standard safety threshold (50 units).
     */
    List<LowStockAlertDto> getLowStockAlerts();

    /**
     * Retrieves sales performance and order breakdowns.
     */
    SalesSummaryDto getSalesSummary();
}
