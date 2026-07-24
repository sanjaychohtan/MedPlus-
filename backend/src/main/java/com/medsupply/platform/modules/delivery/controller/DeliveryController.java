package com.medsupply.platform.modules.delivery.controller;

import com.medsupply.platform.common.dto.ApiResponse;
import com.medsupply.platform.modules.auth.repository.UserRepository;
import com.medsupply.platform.modules.delivery.model.DeliveryTask;
import com.medsupply.platform.modules.delivery.service.DeliveryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
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
@RequestMapping("/api/deliveries")
@RequiredArgsConstructor
@Validated
@Tag(name = "Last-Mile Delivery Operations", description = "Endpoints handling driver assignments, tracking telemetry coordinates, and secure handover PIN verifications.")
public class DeliveryController {

    private final DeliveryService deliveryService;
    private final UserRepository userRepository;

    @GetMapping
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN', 'WAREHOUSE_STAFF')")
    @Operation(summary = "Get all delivery tasks")
    public ResponseEntity<ApiResponse<List<DeliveryTask>>> getAllTasks() {
        return ResponseEntity.ok(ApiResponse.success(deliveryService.getAllTasks(), "Tasks retrieved"));
    }

    @GetMapping("/driver/{driverId}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN', 'WAREHOUSE_STAFF', 'DELIVERY_BOY')")
    @Operation(summary = "Get delivery queue for driver")
    public ResponseEntity<ApiResponse<List<DeliveryTask>>> getTasksByDriver(@PathVariable @NotNull UUID driverId) {
        validateDriverIdOR(driverId);
        return ResponseEntity.ok(ApiResponse.success(deliveryService.getTasksByDriver(driverId), "Driver queue retrieved"));
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN', 'WAREHOUSE_STAFF')")
    @Operation(summary = "Assign order to driver")
    public ResponseEntity<ApiResponse<DeliveryTask>> createTask(@RequestParam @NotNull UUID orderId, @RequestParam @NotNull UUID driverId) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(deliveryService.createTask(orderId, driverId), "Delivery assigned. Handover PIN generated."));
    }

    @PutMapping("/{id}/location")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN', 'DELIVERY_BOY')")
    @Operation(summary = "Update courier real-time GPS coords and estimated arrival times")
    public ResponseEntity<ApiResponse<DeliveryTask>> updateLocation(
            @PathVariable @NotNull UUID id,
            @RequestParam @NotNull @DecimalMin("-90.0") @DecimalMax("90.0") BigDecimal lat,
            @RequestParam @NotNull @DecimalMin("-180.0") @DecimalMax("180.0") BigDecimal lng,
            @RequestParam @Min(0) int minutes) {
        return ResponseEntity.ok(ApiResponse.success(deliveryService.updateLocation(id, lat, lng, minutes), "GPS streaming update stored"));
    }

    @PutMapping("/{id}/complete")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN', 'DELIVERY_BOY')")
    @Operation(summary = "Verify OTP and complete last-mile handover")
    public ResponseEntity<ApiResponse<DeliveryTask>> completeTask(
            @PathVariable @NotNull UUID id,
            @RequestParam @NotNull @Pattern(regexp = "^[0-9]{4}$", message = "OTP must be exactly 4 digits") String otp) {
        return ResponseEntity.ok(ApiResponse.success(deliveryService.completeTask(id, otp), "OTP verified. Shipment handed over safely."));
    }

    private void validateDriverIdOR(UUID driverId) {
        org.springframework.security.core.Authentication auth = org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.isAuthenticated()) {
            boolean isDriver = auth.getAuthorities().stream()
                    .anyMatch(a -> a.getAuthority().equals("ROLE_DELIVERY_BOY"));
            if (isDriver) {
                com.medsupply.platform.modules.auth.model.User user = userRepository.findByEmail(auth.getName())
                        .orElseThrow(() -> new com.medsupply.platform.common.exception.DomainException(
                                "DRIVER_NOT_FOUND", "Logged-in driver user not found.", HttpStatus.NOT_FOUND));
                if (!user.getId().equals(driverId)) {
                    throw new com.medsupply.platform.common.exception.DomainException(
                            "ACCESS_DENIED", "You are not authorized to access other drivers' tasks.", HttpStatus.FORBIDDEN);
                }
            }
        }
    }
}
