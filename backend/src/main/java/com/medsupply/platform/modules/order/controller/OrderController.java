package com.medsupply.platform.modules.order.controller;

import com.medsupply.platform.common.dto.ApiResponse;
import com.medsupply.platform.modules.order.model.Order;
import com.medsupply.platform.modules.order.model.Invoice;
import com.medsupply.platform.modules.order.model.Coupon;
import com.medsupply.platform.modules.order.service.OrderService;
import com.medsupply.platform.modules.order.service.OrderItemInput;
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
@RequestMapping("/api/orders")
@RequiredArgsConstructor
@Tag(name = "Order & Invoice Management", description = "Endpoints handling customer ordering, Net-30 invoice logs, and promotional coupons.")
public class OrderController {

    private final OrderService orderService;

    @GetMapping
    @Operation(summary = "Get all orders")
    public ResponseEntity<ApiResponse<List<Order>>> getAllOrders() {
        return ResponseEntity.ok(ApiResponse.success(orderService.getAllOrders(), "Orders retrieved"));
    }

    @GetMapping("/customer/{customerId}")
    @Operation(summary = "Get customer's order history")
    public ResponseEntity<ApiResponse<List<Order>>> getOrdersByCustomer(@PathVariable UUID customerId) {
        return ResponseEntity.ok(ApiResponse.success(orderService.getOrdersByCustomer(customerId), "Customer history retrieved"));
    }

    @PostMapping
    @Operation(summary = "Place a new order (with automatic FEFO lot allocations)")
    public ResponseEntity<ApiResponse<Order>> createOrder(
            @RequestParam UUID customerId,
            @RequestParam String orderType,
            @RequestBody List<OrderItemInput> items,
            @RequestParam String paymentMethod,
            @RequestParam String deliveryAddress,
            @RequestParam(required = false) String poNumber,
            @RequestParam(required = false) String prescriptionUrl,
            @RequestParam(required = false) String couponCode) {
        Order order = orderService.createOrder(customerId, orderType, items, paymentMethod, deliveryAddress, poNumber, prescriptionUrl, couponCode);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(order, "Order placed successfully. Earliest expiry lots locked."));
    }

    @PutMapping("/{id}/status")
    @Operation(summary = "Update order processing status")
    public ResponseEntity<ApiResponse<Order>> updateOrderStatus(@PathVariable UUID id, @RequestParam String status) {
        return ResponseEntity.ok(ApiResponse.success(orderService.updateOrderStatus(id, status), "Order status updated"));
    }

    @PostMapping("/{id}/invoice")
    @Operation(summary = "Generate a legally-compliant CGST/SGST Net-30 tax invoice")
    public ResponseEntity<ApiResponse<Invoice>> generateInvoice(@PathVariable UUID id) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(orderService.generateInvoice(id), "Invoice generated successfully"));
    }

    @GetMapping("/invoices/customer/{customerId}")
    @Operation(summary = "Get customer's Net-30 tax invoices")
    public ResponseEntity<ApiResponse<List<Invoice>>> getInvoicesByCustomer(@PathVariable UUID customerId) {
        return ResponseEntity.ok(ApiResponse.success(orderService.getInvoicesByCustomer(customerId), "Invoices retrieved"));
    }

    @GetMapping("/coupons/validate")
    @Operation(summary = "Validate a coupon code")
    public ResponseEntity<ApiResponse<Coupon>> validateCoupon(@RequestParam String code, @RequestParam BigDecimal amount) {
        return ResponseEntity.ok(ApiResponse.success(orderService.validateCoupon(code, amount), "Coupon is valid"));
    }
}
