package com.medsupply.platform.modules.salesman.controller;

import com.medsupply.platform.common.dto.ApiResponse;
import com.medsupply.platform.modules.salesman.model.SalesmanLead;
import com.medsupply.platform.modules.salesman.service.SalesmanService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/salesman")
@RequiredArgsConstructor
@Tag(name = "B2B Sales CRM pipeline", description = "Endpoints handling salesman pipelines, lead tracking, and merchant onboarding stages.")
public class SalesmanController {

    private final SalesmanService salesmanService;

    @GetMapping("/leads")
    @Operation(summary = "Get all CRM leads")
    public ResponseEntity<ApiResponse<List<SalesmanLead>>> getAllLeads() {
        return ResponseEntity.ok(ApiResponse.success(salesmanService.getAllLeads(), "CRM leads retrieved"));
    }

    @GetMapping("/leads/salesman/{salesmanId}")
    @Operation(summary = "Get leads assigned to a specific salesman")
    public ResponseEntity<ApiResponse<List<SalesmanLead>>> getLeadsBySalesman(@PathVariable UUID salesmanId) {
        return ResponseEntity.ok(ApiResponse.success(salesmanService.getLeadsBySalesman(salesmanId), "Assigned leads retrieved"));
    }

    @PostMapping("/leads")
    @Operation(summary = "Log a new prospective customer onboarding lead")
    public ResponseEntity<ApiResponse<SalesmanLead>> createLead(
            @RequestParam UUID salesmanId,
            @RequestParam String name,
            @RequestParam String email,
            @RequestParam String phone,
            @RequestParam(required = false) String company,
            @RequestParam(required = false) BigDecimal pipeValue,
            @RequestParam(required = false) String source,
            @RequestParam(required = false) String notes) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(salesmanService.createLead(salesmanId, name, email, phone, company, pipeValue, source, notes), "Prospect lead logged successfully"));
    }

    @PutMapping("/leads/{id}/status")
    @Operation(summary = "Update onboarding pipeline status")
    public ResponseEntity<ApiResponse<SalesmanLead>> updateLeadStatus(@PathVariable UUID id, @RequestParam String status) {
        return ResponseEntity.ok(ApiResponse.success(salesmanService.updateLeadStatus(id, status), "Pipeline updated successfully"));
    }
}
