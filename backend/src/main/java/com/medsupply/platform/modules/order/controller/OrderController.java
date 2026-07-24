package com.medsupply.platform.modules.order.controller;

import com.medsupply.platform.common.dto.ApiResponse;
import com.medsupply.platform.modules.auth.repository.UserRepository;
import com.medsupply.platform.modules.order.model.Order;
import com.medsupply.platform.modules.order.model.Invoice;
import com.medsupply.platform.modules.order.model.Coupon;
import com.medsupply.platform.modules.order.model.OrderStatus;
import com.medsupply.platform.modules.order.service.OrderService;
import com.medsupply.platform.modules.order.service.OrderItemInput;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
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
@RequestMapping("/api/orders")
@RequiredArgsConstructor
@Validated
@Tag(name = "Order & Invoice Management", description = "Endpoints handling customer ordering, Net-30 invoice logs, and promotional coupons.")
public class OrderController {

    private final OrderService orderService;
    private final UserRepository userRepository;

    @GetMapping
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN', 'SALESMAN', 'WAREHOUSE_STAFF')")
    @Operation(summary = "Get all orders")
    public ResponseEntity<ApiResponse<List<Order>>> getAllOrders() {
        return ResponseEntity.ok(ApiResponse.success(orderService.getAllOrders(), "Orders retrieved"));
    }

    @GetMapping("/customer/{customerId}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN', 'SALESMAN', 'B2B_CUSTOMER', 'B2C_CUSTOMER')")
    @Operation(summary = "Get customer's order history")
    public ResponseEntity<ApiResponse<List<Order>>> getOrdersByCustomer(@PathVariable @NotNull UUID customerId) {
        validateCustomerIdOR(customerId);
        return ResponseEntity.ok(ApiResponse.success(orderService.getOrdersByCustomer(customerId), "Customer history retrieved"));
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN', 'SALESMAN', 'B2B_CUSTOMER', 'B2C_CUSTOMER')")
    @Operation(summary = "Place a new order (with automatic FEFO lot allocations)")
    public ResponseEntity<ApiResponse<Order>> createOrder(
            @RequestParam @NotNull UUID customerId,
            @RequestParam @NotNull String orderType,
            @RequestBody @Valid List<@Valid OrderItemInput> items,
            @RequestParam @NotNull String paymentMethod,
            @RequestParam @NotNull String deliveryAddress,
            @RequestParam(required = false) String poNumber,
            @RequestParam(required = false) String prescriptionUrl,
            @RequestParam(required = false) String couponCode) {
        
        UUID resolvedCustomerId = customerId;
        org.springframework.security.core.Authentication auth = org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.isAuthenticated()) {
            boolean isCustomer = auth.getAuthorities().stream()
                    .anyMatch(a -> a.getAuthority().equals("ROLE_B2B_CUSTOMER") || a.getAuthority().equals("ROLE_B2C_CUSTOMER"));
            if (isCustomer) {
                com.medsupply.platform.modules.auth.model.User user = userRepository.findByEmail(auth.getName())
                        .orElseThrow(() -> new com.medsupply.platform.common.exception.DomainException(
                                "CUSTOMER_NOT_FOUND", "Logged-in customer not found.", HttpStatus.NOT_FOUND));
                resolvedCustomerId = user.getId();
            }
        }

        Order order = orderService.createOrder(resolvedCustomerId, orderType, items, paymentMethod, deliveryAddress, poNumber, prescriptionUrl, couponCode);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(order, "Order placed successfully. Earliest expiry lots locked."));
    }

    @PutMapping("/{id}/status")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN', 'SALESMAN', 'WAREHOUSE_STAFF')")
    @Operation(summary = "Update order processing status")
    public ResponseEntity<ApiResponse<Order>> updateOrderStatus(@PathVariable @NotNull UUID id, @RequestParam @NotNull OrderStatus status) {
        return ResponseEntity.ok(ApiResponse.success(orderService.updateOrderStatus(id, status.name()), "Order status updated"));
    }

    @PostMapping("/{id}/invoice")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN', 'SALESMAN', 'WAREHOUSE_STAFF')")
    @Operation(summary = "Generate a legally-compliant CGST/SGST Net-30 tax invoice")
    public ResponseEntity<ApiResponse<Invoice>> generateInvoice(@PathVariable @NotNull UUID id) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(orderService.generateInvoice(id), "Invoice generated successfully"));
    }

    @GetMapping("/invoices/customer/{customerId}")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN', 'SALESMAN', 'B2B_CUSTOMER', 'B2C_CUSTOMER')")
    @Operation(summary = "Get customer's Net-30 tax invoices")
    public ResponseEntity<ApiResponse<List<Invoice>>> getInvoicesByCustomer(@PathVariable @NotNull UUID customerId) {
        validateCustomerIdOR(customerId);
        return ResponseEntity.ok(ApiResponse.success(orderService.getInvoicesByCustomer(customerId), "Invoices retrieved"));
    }

    @GetMapping("/coupons/validate")
    @PreAuthorize("hasAnyRole('SUPER_ADMIN', 'ADMIN', 'SALESMAN', 'B2B_CUSTOMER', 'B2C_CUSTOMER')")
    @Operation(summary = "Validate a coupon code")
    public ResponseEntity<ApiResponse<Coupon>> validateCoupon(
            @RequestParam @NotBlank String code,
            @RequestParam @NotNull @DecimalMin("0.0") BigDecimal amount) {
        return ResponseEntity.ok(ApiResponse.success(orderService.validateCoupon(code, amount), "Coupon is valid"));
    }

    private void validateCustomerIdOR(UUID customerId) {
        org.springframework.security.core.Authentication auth = org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.isAuthenticated()) {
            boolean isCustomer = auth.getAuthorities().stream()
                    .anyMatch(a -> a.getAuthority().equals("ROLE_B2B_CUSTOMER") || a.getAuthority().equals("ROLE_B2C_CUSTOMER"));
            if (isCustomer) {
                com.medsupply.platform.modules.auth.model.User user = userRepository.findByEmail(auth.getName())
                        .orElseThrow(() -> new com.medsupply.platform.common.exception.DomainException(
                                "CUSTOMER_NOT_FOUND", "Logged-in user not found.", HttpStatus.NOT_FOUND));
                if (!user.getId().equals(customerId)) {
                    throw new com.medsupply.platform.common.exception.DomainException(
                            "ACCESS_DENIED", "You are not authorized to view or modify other customers' data.", HttpStatus.FORBIDDEN);
                }
            }
        }
    }
}
