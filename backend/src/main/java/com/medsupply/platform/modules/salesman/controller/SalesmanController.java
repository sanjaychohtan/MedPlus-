package com.medsupply.platform.modules.salesman.controller;

import com.medsupply.platform.common.dto.ApiResponse;
import com.medsupply.platform.modules.auth.repository.UserRepository;
import com.medsupply.platform.modules.salesman.model.SalesmanLead;
import com.medsupply.platform.modules.salesman.model.LeadStatus;
import com.medsupply.platform.modules.salesman.service.SalesmanService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/salesman")
@RequiredArgsConstructor
@Validated
@Tag(name = "B2B Sales CRM pipeline", description = "Endpoints handling salesman pipelines, lead tracking, and merchant onboarding stages.")
public class SalesmanController {

    private final SalesmanService salesmanService;
    private final UserRepository userRepository;

    @GetMapping("/leads")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN', 'SALESMAN')")
    @Operation(summary = "Get all CRM leads with optional pagination")
    public ResponseEntity<ApiResponse<Object>> getAllLeads(
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size,
            @RequestParam(required = false) String sortBy) {
        
        if (page != null) {
            int limitSize = (size != null) ? Math.min(size, 100) : 20;
            String sortField = "createdAt";
            if (sortBy != null && (sortBy.equals("createdAt") || sortBy.equals("pipeValue") || sortBy.equals("name") || sortBy.equals("company"))) {
                sortField = sortBy;
            }
            org.springframework.data.domain.Pageable pageable = org.springframework.data.domain.PageRequest.of(
                    page, limitSize, org.springframework.data.domain.Sort.by(sortField).descending());
            return ResponseEntity.ok(ApiResponse.success(salesmanService.getAllLeads(pageable), "CRM leads retrieved (paginated)"));
        }
        return ResponseEntity.ok(ApiResponse.success(salesmanService.getAllLeads(), "CRM leads retrieved"));
    }

    @GetMapping("/leads/salesman/{salesmanId}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN', 'SALESMAN')")
    @Operation(summary = "Get leads assigned to a specific salesman")
    public ResponseEntity<ApiResponse<List<SalesmanLead>>> getLeadsBySalesman(@PathVariable @NotNull UUID salesmanId) {
        UUID targetSalesmanId = salesmanId;
        org.springframework.security.core.Authentication auth = org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.isAuthenticated()) {
            boolean isSalesman = auth.getAuthorities().stream()
                    .anyMatch(a -> a.getAuthority().equals("ROLE_SALESMAN"));
            if (isSalesman) {
                com.medsupply.platform.modules.auth.model.User user = userRepository.findByEmail(auth.getName())
                        .orElseThrow(() -> new com.medsupply.platform.common.exception.DomainException(
                                "SALESMAN_NOT_FOUND", "Logged-in salesman user not found.", HttpStatus.NOT_FOUND));
                if (!user.getId().equals(salesmanId)) {
                    throw new com.medsupply.platform.common.exception.DomainException(
                            "ACCESS_DENIED", "You are not authorized to view other salesmen's leads.", HttpStatus.FORBIDDEN);
                }
                targetSalesmanId = user.getId();
            }
        }
        return ResponseEntity.ok(ApiResponse.success(salesmanService.getLeadsBySalesman(targetSalesmanId), "Assigned leads retrieved"));
    }

    @PostMapping("/leads")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN', 'SALESMAN')")
    @Operation(summary = "Log a new prospective customer onboarding lead")
    public ResponseEntity<ApiResponse<SalesmanLead>> createLead(
            @RequestParam(required = false) UUID salesmanId,
            @RequestParam @NotBlank String name,
            @RequestParam @NotBlank @Email String email,
            @RequestParam @NotBlank @Pattern(regexp = "^\\+?[0-9]{10,15}$", message = "Phone must be a valid mobile number") String phone,
            @RequestParam(required = false) String company,
            @RequestParam(required = false) @DecimalMin("0.0") BigDecimal pipeValue,
            @RequestParam(required = false) String source,
            @RequestParam(required = false) String notes) {
        
        UUID targetSalesmanId = salesmanId;
        org.springframework.security.core.Authentication auth = org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.isAuthenticated()) {
            boolean isSalesman = auth.getAuthorities().stream()
                    .anyMatch(a -> a.getAuthority().equals("ROLE_SALESMAN"));
            if (isSalesman) {
                com.medsupply.platform.modules.auth.model.User user = userRepository.findByEmail(auth.getName())
                        .orElseThrow(() -> new com.medsupply.platform.common.exception.DomainException(
                                "SALESMAN_NOT_FOUND", "Logged-in salesman user not found.", HttpStatus.NOT_FOUND));
                targetSalesmanId = user.getId();
            }
        }

        if (targetSalesmanId == null) {
            throw new com.medsupply.platform.common.exception.DomainException(
                    "MISSING_SALESMAN_ID", "Salesman ID must be supplied or resolved from session.", HttpStatus.BAD_REQUEST);
        }

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(salesmanService.createLead(targetSalesmanId, name, email, phone, company, pipeValue, source, notes), "Prospect lead logged successfully"));
    }

    @PutMapping("/leads/{id}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN', 'SALESMAN')")
    @Operation(summary = "Update CRM lead details")
    public ResponseEntity<ApiResponse<SalesmanLead>> updateLead(
            @PathVariable @NotNull UUID id,
            @RequestParam @NotBlank String name,
            @RequestParam @NotBlank @Email String email,
            @RequestParam @NotBlank @Pattern(regexp = "^\\+?[0-9]{10,15}$", message = "Phone must be a valid mobile number") String phone,
            @RequestParam(required = false) String company,
            @RequestParam(required = false) @DecimalMin("0.0") BigDecimal pipeValue,
            @RequestParam(required = false) String source,
            @RequestParam(required = false) String notes) {
        return ResponseEntity.ok(ApiResponse.success(salesmanService.updateLead(id, name, email, phone, company, pipeValue, source, notes), "Lead updated successfully"));
    }

    @PutMapping("/leads/{id}/status")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN', 'SALESMAN')")
    @Operation(summary = "Update onboarding pipeline status")
    public ResponseEntity<ApiResponse<SalesmanLead>> updateLeadStatus(
            @PathVariable @NotNull UUID id,
            @RequestParam @NotNull LeadStatus status) {
        return ResponseEntity.ok(ApiResponse.success(salesmanService.updateLeadStatus(id, status.name()), "Pipeline updated successfully"));
    }

    @DeleteMapping("/leads/{id}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN')")
    @Operation(summary = "Soft delete a CRM lead")
    public ResponseEntity<ApiResponse<Void>> deleteLead(@PathVariable @NotNull UUID id) {
        salesmanService.deleteLead(id);
        return ResponseEntity.ok(ApiResponse.success(null, "Lead deleted successfully"));
    }
}
