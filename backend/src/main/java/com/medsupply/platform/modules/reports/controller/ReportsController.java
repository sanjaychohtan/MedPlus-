package com.medsupply.platform.modules.reports.controller;

import com.medsupply.platform.common.dto.ApiResponse;
import com.medsupply.platform.modules.reports.dto.LowStockAlertDto;
import com.medsupply.platform.modules.reports.dto.NearExpiryLotDto;
import com.medsupply.platform.modules.reports.dto.PlatformMetricsDto;
import com.medsupply.platform.modules.reports.dto.SalesSummaryDto;
import com.medsupply.platform.modules.reports.service.ReportsService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/reports")
@PreAuthorize("hasAnyRole('ADMIN', 'SALESMAN')")
@Tag(name = "Platform Reports & Analytics", description = "Endpoints for retrieving high-performance, cached platform business intelligence.")
public class ReportsController {

    private final ReportsService reportsService;

    public ReportsController(ReportsService reportsService) {
        this.reportsService = reportsService;
    }

    @GetMapping("/metrics")
    @Operation(summary = "Get general platform metrics", description = "Retrieves high-level counts and total inventory valuations.")
    public ResponseEntity<ApiResponse<PlatformMetricsDto>> getPlatformMetrics() {
        return ResponseEntity.ok(ApiResponse.success(reportsService.getPlatformMetrics(), "Platform metrics loaded successfully"));
    }

    @GetMapping("/near-expiry")
    @Operation(summary = "Get near-expiry lots", description = "Retrieves physical batch lots expiring within the next 90 days for FEFO prevention.")
    public ResponseEntity<ApiResponse<List<NearExpiryLotDto>>> getNearExpiryLots() {
        return ResponseEntity.ok(ApiResponse.success(reportsService.getNearExpiryLots(), "Near-expiry lot report loaded successfully"));
    }

    @GetMapping("/low-stock")
    @Operation(summary = "Get low-stock products", description = "Retrieves products whose total available quantities are below the safety threshold (50 units).")
    public ResponseEntity<ApiResponse<List<LowStockAlertDto>>> getLowStockAlerts() {
        return ResponseEntity.ok(ApiResponse.success(reportsService.getLowStockAlerts(), "Low-stock alerts loaded successfully"));
    }

    @GetMapping("/sales")
    @Operation(summary = "Get sales aggregates summary", description = "Retrieves revenue aggregates and order status metrics.")
    public ResponseEntity<ApiResponse<SalesSummaryDto>> getSalesSummary() {
        return ResponseEntity.ok(ApiResponse.success(reportsService.getSalesSummary(), "Sales summary report loaded successfully"));
    }
}
