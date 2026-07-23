package com.medsupply.platform.modules.delivery.controller;

import com.medsupply.platform.common.dto.ApiResponse;
import com.medsupply.platform.modules.delivery.model.DeliveryTask;
import com.medsupply.platform.modules.delivery.service.DeliveryService;
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
@RequestMapping("/api/deliveries")
@RequiredArgsConstructor
@Tag(name = "Last-Mile Delivery Operations", description = "Endpoints handling driver assignments, tracking telemetry coordinates, and secure handover PIN verifications.")
public class DeliveryController {

    private final DeliveryService deliveryService;

    @GetMapping
    @Operation(summary = "Get all delivery tasks")
    public ResponseEntity<ApiResponse<List<DeliveryTask>>> getAllTasks() {
        return ResponseEntity.ok(ApiResponse.success(deliveryService.getAllTasks(), "Tasks retrieved"));
    }

    @GetMapping("/driver/{driverId}")
    @Operation(summary = "Get delivery queue for driver")
    public ResponseEntity<ApiResponse<List<DeliveryTask>>> getTasksByDriver(@PathVariable UUID driverId) {
        return ResponseEntity.ok(ApiResponse.success(deliveryService.getTasksByDriver(driverId), "Driver queue retrieved"));
    }

    @PostMapping
    @Operation(summary = "Assign order to driver")
    public ResponseEntity<ApiResponse<DeliveryTask>> createTask(@RequestParam UUID orderId, @RequestParam UUID driverId) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(deliveryService.createTask(orderId, driverId), "Delivery assigned. Handover PIN generated."));
    }

    @PutMapping("/{id}/location")
    @Operation(summary = "Update courier real-time GPS coords and estimated arrival times")
    public ResponseEntity<ApiResponse<DeliveryTask>> updateLocation(
            @PathVariable UUID id,
            @RequestParam BigDecimal lat,
            @RequestParam BigDecimal lng,
            @RequestParam int minutes) {
        return ResponseEntity.ok(ApiResponse.success(deliveryService.updateLocation(id, lat, lng, minutes), "GPS streaming update stored"));
    }

    @PutMapping("/{id}/complete")
    @Operation(summary = "Verify OTP and complete last-mile handover")
    public ResponseEntity<ApiResponse<DeliveryTask>> completeTask(@PathVariable UUID id, @RequestParam String otp) {
        return ResponseEntity.ok(ApiResponse.success(deliveryService.completeTask(id, otp), "OTP verified. Shipment handed over safely."));
    }
}
