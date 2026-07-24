package com.medsupply.platform.modules.order.service;

import com.medsupply.platform.common.exception.DomainException;
import com.medsupply.platform.modules.audit.service.AuditLogService;
import com.medsupply.platform.modules.auth.model.User;
import com.medsupply.platform.modules.auth.model.UserStatus;
import com.medsupply.platform.modules.auth.repository.UserRepository;
import com.medsupply.platform.modules.inventory.model.Batch;
import com.medsupply.platform.modules.inventory.model.Product;
import com.medsupply.platform.modules.inventory.repository.BatchRepository;
import com.medsupply.platform.modules.inventory.repository.ProductRepository;
import com.medsupply.platform.modules.order.model.*;
import com.medsupply.platform.modules.order.repository.*;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.*;

@Service
@RequiredArgsConstructor
@Transactional
public class OrderServiceImpl implements OrderService {

    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    private final InvoiceRepository invoiceRepository;
    private final CouponRepository couponRepository;
    private final UserRepository userRepository;
    private final ProductRepository productRepository;
    private final BatchRepository batchRepository;
    private final AuditLogService auditLogService;
    private final HttpServletRequest httpRequest;

    @Override
    @Transactional(readOnly = true)
    public List<Order> getAllOrders() {
        return orderRepository.findAll().stream()
                .filter(o -> !o.isDeleted())
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<Order> getOrdersByCustomer(UUID customerId) {
        return orderRepository.findByCustomerIdAndIsDeletedFalse(customerId);
    }

    @Override
    public Order createOrder(UUID customerId, String orderType, List<OrderItemInput> items, String paymentMethod, String deliveryAddress, String poNumber, String prescriptionUrl, String couponCode) {
        if (items == null || items.isEmpty()) {
            throw new DomainException("EMPTY_ORDER", "Order must contain at least one item.", HttpStatus.BAD_REQUEST);
        }

        // Lock customer row to prevent credit check race conditions
        User customer = userRepository.findByIdWithLock(customerId)
                .orElseThrow(() -> new DomainException("CUSTOMER_NOT_FOUND", "Customer not found with ID: " + customerId, HttpStatus.NOT_FOUND));

        if (customer.getStatus() != UserStatus.ACTIVE) {
            throw new DomainException("INACTIVE_CUSTOMER", "Customer account is not ACTIVE. Current status: " + customer.getStatus(), HttpStatus.BAD_REQUEST);
        }

        // PO Number duplicate check
        if (poNumber != null && !poNumber.isBlank()) {
            if (orderRepository.existsByCustomerAndPoNumberAndIsDeletedFalse(customer, poNumber.trim())) {
                throw new DomainException("DUPLICATE_PO_NUMBER", "An order with PO Number '" + poNumber.trim() + "' already exists for this customer.", HttpStatus.BAD_REQUEST);
            }
        }

        // Validate items and duplicate products
        Set<UUID> seenProducts = new HashSet<>();
        for (OrderItemInput input : items) {
            if (input.getQuantity() <= 0) {
                throw new DomainException("INVALID_QUANTITY", "Item quantity must be greater than zero.", HttpStatus.BAD_REQUEST);
            }
            if (!seenProducts.add(input.getProductId())) {
                throw new DomainException("DUPLICATE_ORDER_ITEM", "Duplicate product in order items: " + input.getProductId(), HttpStatus.BAD_REQUEST);
            }
        }

        String orderNumber = "ORD-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();

        Order order = Order.builder()
                .orderNumber(orderNumber)
                .orderType(orderType)
                .customer(customer)
                .customerName(customer.getFirstName() + " " + customer.getLastName())
                .customerEmail(customer.getEmail())
                .paymentMethod(paymentMethod)
                .paymentStatus("PENDING")
                .orderStatus(OrderStatus.PENDING_APPROVAL.name())
                .deliveryAddress(deliveryAddress)
                .poNumber(poNumber)
                .prescriptionUrl(prescriptionUrl)
                .items(new ArrayList<>())
                .build();

        BigDecimal subtotal = BigDecimal.ZERO;
        BigDecimal taxAmount = BigDecimal.ZERO;

        for (OrderItemInput input : items) {
            Product product = productRepository.findById(input.getProductId())
                    .orElseThrow(() -> new DomainException("PRODUCT_NOT_FOUND", "Product not found: " + input.getProductId(), HttpStatus.NOT_FOUND));

            if (product.isDeleted()) {
                throw new DomainException("PRODUCT_INACTIVE", "Product is inactive or deleted: " + product.getName(), HttpStatus.BAD_REQUEST);
            }

            int requestedQty = input.getQuantity();

            // FEFO Multi-Batch picking algorithm with pessimistic write locks
            List<Batch> activeBatches = batchRepository.findFefoBatchesForProductWithLock(product.getId());

            // Filter valid active, unexpired, non-deleted batches
            List<Batch> validBatches = activeBatches.stream()
                    .filter(b -> !b.isDeleted())
                    .filter(b -> "ACTIVE".equalsIgnoreCase(b.getStatus()))
                    .filter(b -> b.getExpiryDate() == null || !b.getExpiryDate().isBefore(LocalDate.now()))
                    .filter(b -> b.getQuantityAvailable() > 0)
                    .toList();

            int totalAvailable = validBatches.stream().mapToInt(Batch::getQuantityAvailable).sum();
            if (totalAvailable < requestedQty) {
                throw new DomainException("INSUFFICIENT_STOCK",
                        "Insufficient available inventory for product " + product.getName() + ". Requested: " + requestedQty + ", Available: " + totalAvailable,
                        HttpStatus.BAD_REQUEST);
            }

            // Determine unit price based on customer/order type tiers
            BigDecimal unitPrice = product.getB2cPrice();
            if ("B2B".equalsIgnoreCase(orderType)) {
                BigDecimal creditLimit = customer.getCreditLimit() != null ? customer.getCreditLimit() : BigDecimal.ZERO;
                if (creditLimit.compareTo(new BigDecimal("10000.00")) > 0) {
                    unitPrice = product.getB2bPriceTier2();
                } else {
                    unitPrice = product.getB2bPriceTier1();
                }
            }

            int remainingQtyNeeded = requestedQty;

            // Allocate across earliest-expiring available FEFO batches
            for (Batch batch : validBatches) {
                if (remainingQtyNeeded <= 0) break;

                int batchAvail = batch.getQuantityAvailable();
                int allocQty = Math.min(remainingQtyNeeded, batchAvail);

                // Reserve stock on batch
                batch.setQuantityReserved(batch.getQuantityReserved() + allocQty);
                batch.calculateAvailableQuantity();
                batchRepository.save(batch);

                BigDecimal itemSubtotal = unitPrice.multiply(BigDecimal.valueOf(allocQty));
                BigDecimal taxRatePercent = product.getTaxRatePercent();
                BigDecimal itemTax = itemSubtotal.multiply(taxRatePercent).divide(new BigDecimal("100.00"), RoundingMode.HALF_UP);
                BigDecimal itemTotal = itemSubtotal.add(itemTax);

                subtotal = subtotal.add(itemSubtotal);
                taxAmount = taxAmount.add(itemTax);

                OrderItem orderItem = OrderItem.builder()
                        .order(order)
                        .product(product)
                        .batch(batch)
                        .productName(product.getName())
                        .productSku(product.getSku())
                        .batchNumber(batch.getBatchNumber())
                        .quantity(allocQty)
                        .unitPrice(unitPrice)
                        .mrp(product.getMrp())
                        .taxRate(taxRatePercent)
                        .taxAmount(itemTax)
                        .totalPrice(itemTotal)
                        .build();

                order.getItems().add(orderItem);
                remainingQtyNeeded -= allocQty;
            }
        }

        BigDecimal discountAmount = BigDecimal.ZERO;
        if (couponCode != null && !couponCode.isBlank()) {
            Coupon coupon = validateCouponInternal(couponCode, subtotal);
            if (coupon != null) {
                discountAmount = subtotal.multiply(coupon.getDiscountPercent()).divide(new BigDecimal("100.00"), RoundingMode.HALF_UP);
                if (coupon.getMaxDiscount() != null && discountAmount.compareTo(coupon.getMaxDiscount()) > 0) {
                    discountAmount = coupon.getMaxDiscount();
                }
                coupon.setUsageCount(coupon.getUsageCount() + 1);
                couponRepository.save(coupon);

                auditLogService.log(
                        customer.getFirstName() + " " + customer.getLastName(),
                        "COUPON_APPLIED",
                        "ORDER",
                        "Applied coupon " + couponCode + " with discount $" + discountAmount,
                        getClientIp()
                );
            }
        }

        order.setSubtotal(subtotal);
        order.setTaxAmount(taxAmount);
        order.setDiscountAmount(discountAmount);
        order.setTotalAmount(subtotal.add(taxAmount).subtract(discountAmount));

        // Net-30 credit check for B2B Customers with locking
        if ("B2B".equalsIgnoreCase(orderType)) {
            BigDecimal currentOutstanding = customer.getOutstandingBalance() != null ? customer.getOutstandingBalance() : BigDecimal.ZERO;
            BigDecimal creditLimit = customer.getCreditLimit() != null ? customer.getCreditLimit() : BigDecimal.ZERO;
            BigDecimal newOutstanding = currentOutstanding.add(order.getTotalAmount());

            if (newOutstanding.compareTo(creditLimit) > 0) {
                throw new DomainException("CREDIT_LIMIT_EXCEEDED",
                        "Credit limit exceeded. Current outstanding: $" + currentOutstanding + " + Order total: $" + order.getTotalAmount() + " exceeds limit of $" + creditLimit,
                        HttpStatus.BAD_REQUEST);
            }
            customer.setOutstandingBalance(newOutstanding);
            userRepository.save(customer);
        }

        Order savedOrder = orderRepository.save(order);

        auditLogService.log(
                customer.getFirstName() + " " + customer.getLastName(),
                "ORDER_CREATED",
                "ORDER",
                "Created order " + savedOrder.getOrderNumber() + " total amount $" + savedOrder.getTotalAmount(),
                getClientIp()
        );

        return savedOrder;
    }

    @Override
    public Order updateOrderStatus(UUID orderId, String status) {
        OrderStatus newStatus;
        try {
            newStatus = OrderStatus.valueOf(status.toUpperCase());
        } catch (Exception e) {
            throw new DomainException("INVALID_ORDER_STATUS", "Invalid order status value: " + status, HttpStatus.BAD_REQUEST);
        }

        Order order = orderRepository.findByIdWithLock(orderId)
                .orElseThrow(() -> new DomainException("ORDER_NOT_FOUND", "Order not found: " + orderId, HttpStatus.NOT_FOUND));

        OrderStatus currentStatus;
        try {
            currentStatus = OrderStatus.valueOf(order.getOrderStatus().toUpperCase());
        } catch (Exception e) {
            currentStatus = OrderStatus.PENDING_APPROVAL;
        }

        if (!currentStatus.isValidTransition(newStatus)) {
            throw new DomainException("INVALID_STATUS_TRANSITION",
                    "Cannot transition order status from " + currentStatus + " to " + newStatus,
                    HttpStatus.BAD_REQUEST);
        }

        order.setOrderStatus(newStatus.name());

        // Stock handling during status transition
        boolean isCancellationOrRejection = (newStatus == OrderStatus.CANCELLED || newStatus == OrderStatus.REJECTED);
        boolean isFulfillment = (newStatus == OrderStatus.SHIPPED || newStatus == OrderStatus.DELIVERED);

        if (isCancellationOrRejection) {
            // Release reserved stock back to available pool
            for (OrderItem item : order.getItems()) {
                if (item.getBatch() != null) {
                    Batch batch = batchRepository.findByIdWithLock(item.getBatch().getId()).orElse(null);
                    if (batch != null) {
                        batch.setQuantityReserved(Math.max(0, batch.getQuantityReserved() - item.getQuantity()));
                        batch.calculateAvailableQuantity();
                        batchRepository.save(batch);
                    }
                }
            }

            // If B2B order was cancelled/rejected, release credit limit outstanding balance
            if ("B2B".equalsIgnoreCase(order.getOrderType())) {
                User customer = userRepository.findByIdWithLock(order.getCustomer().getId()).orElse(null);
                if (customer != null) {
                    BigDecimal currentOut = customer.getOutstandingBalance() != null ? customer.getOutstandingBalance() : BigDecimal.ZERO;
                    customer.setOutstandingBalance(currentOut.subtract(order.getTotalAmount()).max(BigDecimal.ZERO));
                    userRepository.save(customer);
                }
            }
        } else if (isFulfillment) {
            // If transition from pre-fulfillment state to SHIPPED/DELIVERED, deduct stock on hand and release reservation
            boolean wasUnfulfilled = (currentStatus == OrderStatus.PENDING_APPROVAL || currentStatus == OrderStatus.APPROVED);
            if (wasUnfulfilled) {
                for (OrderItem item : order.getItems()) {
                    if (item.getBatch() != null) {
                        Batch batch = batchRepository.findByIdWithLock(item.getBatch().getId()).orElse(null);
                        if (batch != null) {
                            batch.setQuantityOnHand(Math.max(0, batch.getQuantityOnHand() - item.getQuantity()));
                            batch.setQuantityReserved(Math.max(0, batch.getQuantityReserved() - item.getQuantity()));
                            batch.calculateAvailableQuantity();
                            batchRepository.save(batch);
                        }
                    }
                }
            }
        }

        Order updatedOrder = orderRepository.save(order);

        auditLogService.log(
                order.getCustomerName(),
                "ORDER_STATUS_CHANGED",
                "ORDER",
                "Order " + order.getOrderNumber() + " transitioned from " + currentStatus + " to " + newStatus,
                getClientIp()
        );

        return updatedOrder;
    }

    @Override
    public Invoice generateInvoice(UUID orderId) {
        Order order = orderRepository.findByIdWithLock(orderId)
                .orElseThrow(() -> new DomainException("ORDER_NOT_FOUND", "Order not found: " + orderId, HttpStatus.NOT_FOUND));

        // Idempotency: check if invoice already generated for this order
        Optional<Invoice> existingInvoice = invoiceRepository.findByOrderIdWithLock(orderId);
        if (existingInvoice.isPresent()) {
            return existingInvoice.get();
        }

        String invoiceNumber = "INV-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        BigDecimal subtotal = order.getSubtotal();
        BigDecimal cgst = order.getTaxAmount().divide(new BigDecimal("2.00"), RoundingMode.HALF_UP);
        BigDecimal sgst = order.getTaxAmount().divide(new BigDecimal("2.00"), RoundingMode.HALF_UP);

        Invoice invoice = Invoice.builder()
                .invoiceNumber(invoiceNumber)
                .order(order)
                .orderNumber(order.getOrderNumber())
                .customer(order.getCustomer())
                .gstin(order.getCustomer().getGstin())
                .subtotal(subtotal)
                .cgst(cgst)
                .sgst(sgst)
                .igst(BigDecimal.ZERO)
                .totalAmount(order.getTotalAmount())
                .pdfGeneratedAt(OffsetDateTime.now())
                .paymentDueDate(OffsetDateTime.now().plusDays(30)) // Net-30 payment terms
                .status("UNPAID")
                .build();

        Invoice savedInvoice = invoiceRepository.save(invoice);

        auditLogService.log(
                order.getCustomerName(),
                "INVOICE_GENERATED",
                "INVOICE",
                "Generated invoice " + invoiceNumber + " for order " + order.getOrderNumber(),
                getClientIp()
        );

        return savedInvoice;
    }

    @Override
    @Transactional(readOnly = true)
    public List<Invoice> getInvoicesByCustomer(UUID customerId) {
        return invoiceRepository.findByCustomerId(customerId);
    }

    @Override
    public Coupon validateCoupon(String code, BigDecimal orderAmount) {
        return validateCouponInternal(code, orderAmount);
    }

    private Coupon validateCouponInternal(String code, BigDecimal orderAmount) {
        Coupon coupon = couponRepository.findByCodeAndIsDeletedFalseWithLock(code)
                .orElseThrow(() -> new DomainException("COUPON_NOT_FOUND", "Invalid or expired coupon code: " + code, HttpStatus.NOT_FOUND));

        if (!coupon.isActive()) {
            throw new DomainException("COUPON_INACTIVE", "Coupon code is inactive or suspended: " + code, HttpStatus.BAD_REQUEST);
        }

        if (coupon.getExpiryDate() != null && coupon.getExpiryDate().isBefore(LocalDate.now())) {
            throw new DomainException("COUPON_EXPIRED", "Coupon code has expired on " + coupon.getExpiryDate(), HttpStatus.BAD_REQUEST);
        }

        if (coupon.getUsageLimit() != null && coupon.getUsageCount() >= coupon.getUsageLimit()) {
            throw new DomainException("COUPON_LIMIT_REACHED", "Coupon code usage limit has been reached", HttpStatus.BAD_REQUEST);
        }

        if (orderAmount != null && orderAmount.compareTo(coupon.getMinOrderAmount()) < 0) {
            throw new DomainException("COUPON_MIN_AMOUNT_NOT_MET", "Minimum order value of $" + coupon.getMinOrderAmount() + " required for coupon", HttpStatus.BAD_REQUEST);
        }

        return coupon;
    }

    private String getClientIp() {
        if (httpRequest != null) {
            String remoteAddr = httpRequest.getHeader("X-Forwarded-For");
            if (remoteAddr != null && !remoteAddr.isBlank()) {
                return remoteAddr.split(",")[0].trim();
            }
            return httpRequest.getRemoteAddr();
        }
        return "127.0.0.1";
    }
}
