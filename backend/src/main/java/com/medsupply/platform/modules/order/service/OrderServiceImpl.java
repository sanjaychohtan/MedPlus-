package com.medsupply.platform.modules.order.service;

import com.medsupply.platform.modules.auth.model.User;
import com.medsupply.platform.modules.auth.repository.UserRepository;
import com.medsupply.platform.modules.inventory.model.Batch;
import com.medsupply.platform.modules.inventory.model.Product;
import com.medsupply.platform.modules.inventory.repository.BatchRepository;
import com.medsupply.platform.modules.inventory.repository.ProductRepository;
import com.medsupply.platform.modules.order.model.*;
import com.medsupply.platform.modules.order.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

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
        User customer = userRepository.findById(customerId)
                .orElseThrow(() -> new IllegalArgumentException("Customer not found: " + customerId));

        String orderNumber = "ORD-" + System.currentTimeMillis();

        Order order = Order.builder()
                .orderNumber(orderNumber)
                .orderType(orderType)
                .customer(customer)
                .customerName(customer.getFirstName() + " " + customer.getLastName())
                .customerEmail(customer.getEmail())
                .paymentMethod(paymentMethod)
                .paymentStatus("PENDING")
                .orderStatus("PENDING_APPROVAL")
                .deliveryAddress(deliveryAddress)
                .poNumber(poNumber)
                .prescriptionUrl(prescriptionUrl)
                .items(new ArrayList<>())
                .build();

        BigDecimal subtotal = BigDecimal.ZERO;
        BigDecimal taxAmount = BigDecimal.ZERO;

        for (OrderItemInput input : items) {
            Product product = productRepository.findById(input.getProductId())
                    .orElseThrow(() -> new IllegalArgumentException("Product not found: " + input.getProductId()));

            // FEFO picking algorithm
            List<Batch> activeBatches = batchRepository.findFefoBatchesForProduct(product.getId());
            if (activeBatches.isEmpty()) {
                throw new IllegalArgumentException("Product " + product.getName() + " is completely out of stock.");
            }

            // Find earliest expiring batch that satisfies demand, or pick the first one
            Batch chosenBatch = null;
            int requestedQty = input.getQuantity();
            for (Batch batch : activeBatches) {
                if (batch.getQuantityAvailable() >= requestedQty) {
                    chosenBatch = batch;
                    break;
                }
            }

            if (chosenBatch == null) {
                chosenBatch = activeBatches.get(0); // fallback
            }

            if (chosenBatch.getQuantityAvailable() < requestedQty) {
                throw new IllegalArgumentException("Insufficient inventory available for: " + product.getName() + ". Available: " + chosenBatch.getQuantityAvailable());
            }

            // Reserve stock
            chosenBatch.setQuantityReserved(chosenBatch.getQuantityReserved() + requestedQty);
            chosenBatch.calculateAvailableQuantity();
            batchRepository.save(chosenBatch);

            // Determine unit price based on customer and b2b tiers
            BigDecimal unitPrice = product.getB2cPrice();
            if ("B2B".equalsIgnoreCase(orderType)) {
                // Tier based on credit limits
                if (customer.getCreditLimit().compareTo(new BigDecimal("10000.00")) > 0) {
                    unitPrice = product.getB2bPriceTier2();
                } else {
                    unitPrice = product.getB2bPriceTier1();
                }
            }

            BigDecimal itemSubtotal = unitPrice.multiply(BigDecimal.valueOf(requestedQty));
            BigDecimal taxRatePercent = product.getTaxRatePercent();
            BigDecimal itemTax = itemSubtotal.multiply(taxRatePercent).divide(new BigDecimal("100.00"), RoundingMode.HALF_UP);
            BigDecimal itemTotal = itemSubtotal.add(itemTax);

            subtotal = subtotal.add(itemSubtotal);
            taxAmount = taxAmount.add(itemTax);

            OrderItem orderItem = OrderItem.builder()
                    .order(order)
                    .product(product)
                    .batch(chosenBatch)
                    .productName(product.getName())
                    .productSku(product.getSku())
                    .batchNumber(chosenBatch.getBatchNumber())
                    .quantity(requestedQty)
                    .unitPrice(unitPrice)
                    .mrp(product.getMrp())
                    .taxRate(taxRatePercent)
                    .taxAmount(itemTax)
                    .totalPrice(itemTotal)
                    .build();

            order.getItems().add(orderItem);
        }

        BigDecimal discountAmount = BigDecimal.ZERO;
        if (couponCode != null && !couponCode.isBlank()) {
            Coupon coupon = validateCoupon(couponCode, subtotal);
            if (coupon != null) {
                discountAmount = subtotal.multiply(coupon.getDiscountPercent()).divide(new BigDecimal("100.00"), RoundingMode.HALF_UP);
                if (discountAmount.compareTo(coupon.getMaxDiscount()) > 0) {
                    discountAmount = coupon.getMaxDiscount();
                }
                coupon.setUsageCount(coupon.getUsageCount() + 1);
                couponRepository.save(coupon);
            }
        }

        order.setSubtotal(subtotal);
        order.setTaxAmount(taxAmount);
        order.setDiscountAmount(discountAmount);
        order.setTotalAmount(subtotal.add(taxAmount).subtract(discountAmount));

        // Net-30 credit check for B2B Customers
        if ("B2B".equalsIgnoreCase(orderType)) {
            BigDecimal outstanding = customer.getOutstandingBalance().add(order.getTotalAmount());
            if (outstanding.compareTo(customer.getCreditLimit()) > 0) {
                throw new IllegalArgumentException("Credit limit exceeded. Outstanding: " + customer.getOutstandingBalance() + " + Order: " + order.getTotalAmount() + " exceeds limit of: " + customer.getCreditLimit());
            }
            customer.setOutstandingBalance(outstanding);
            userRepository.save(customer);
        }

        return orderRepository.save(order);
    }

    @Override
    public Order updateOrderStatus(UUID orderId, String status) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new IllegalArgumentException("Order not found: " + orderId));
        
        String oldStatus = order.getOrderStatus();
        order.setOrderStatus(status);

        boolean wasAlreadyDeducted = "SHIPPED".equalsIgnoreCase(oldStatus) || "DELIVERED".equalsIgnoreCase(oldStatus);
        boolean needsDeduction = "SHIPPED".equalsIgnoreCase(status) || "DELIVERED".equalsIgnoreCase(status);

        if (needsDeduction && !wasAlreadyDeducted) {
            // Confirm stock deduction
            for (OrderItem item : order.getItems()) {
                Batch batch = item.getBatch();
                if (batch != null) {
                    batch.setQuantityOnHand(batch.getQuantityOnHand() - item.getQuantity());
                    batch.setQuantityReserved(batch.getQuantityReserved() - item.getQuantity());
                    batch.calculateAvailableQuantity();
                    batchRepository.save(batch);
                }
            }
        }

        return orderRepository.save(order);
    }

    @Override
    public Invoice generateInvoice(UUID orderId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new IllegalArgumentException("Order not found: " + orderId));

        String invoiceNumber = "INV-" + System.currentTimeMillis();
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

        return invoiceRepository.save(invoice);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Invoice> getInvoicesByCustomer(UUID customerId) {
        return invoiceRepository.findByCustomerId(customerId);
    }

    @Override
    @Transactional(readOnly = true)
    public Coupon validateCoupon(String code, BigDecimal orderAmount) {
        Coupon coupon = couponRepository.findByCodeAndIsDeletedFalse(code)
                .orElseThrow(() -> new IllegalArgumentException("Invalid or expired coupon code"));

        if (!coupon.isActive()) {
            throw new IllegalArgumentException("Coupon code has been suspended");
        }

        if (orderAmount.compareTo(coupon.getMinOrderAmount()) < 0) {
            throw new IllegalArgumentException("Minimum order value required: $" + coupon.getMinOrderAmount());
        }

        return coupon;
    }
}
