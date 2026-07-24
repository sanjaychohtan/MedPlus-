package com.medsupply.platform.modules.order.model;

import com.medsupply.platform.common.model.BaseEntity;
import com.medsupply.platform.modules.auth.model.User;
import com.medsupply.platform.modules.warehouse.model.Warehouse;
import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.*;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * JPA Entity representing a supply or retail order in the system.
 */
@Entity
@Table(name = "orders")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@ToString(exclude = {"customer", "warehouse", "items"})
public class Order extends BaseEntity {

    @Column(name = "version")
    @Version
    private Long version;

    @NotBlank(message = "Order number cannot be blank")
    @Size(max = 50, message = "Order number cannot exceed 50 characters")
    @Column(name = "order_number", nullable = false, unique = true, length = 50)
    private String orderNumber;

    @NotBlank(message = "Order type cannot be blank")
    @Size(max = 20, message = "Order type cannot exceed 20 characters")
    @Column(name = "order_type", nullable = false, length = 20)
    private String orderType;

    @NotNull(message = "Customer cannot be null")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "customer_id", nullable = false)
    private User customer;

    @NotBlank(message = "Customer name cannot be blank")
    @Size(max = 200, message = "Customer name cannot exceed 200 characters")
    @Column(name = "customer_name", nullable = false, length = 200)
    private String customerName;

    @NotBlank(message = "Customer email cannot be blank")
    @Email(message = "Customer email must be a valid email address")
    @Size(max = 255, message = "Customer email cannot exceed 255 characters")
    @Column(name = "customer_email", nullable = false)
    private String customerEmail;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "warehouse_id")
    private Warehouse warehouse;

    @NotNull(message = "Subtotal cannot be null")
    @DecimalMin(value = "0.0", message = "Subtotal must be non-negative")
    @Builder.Default
    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal subtotal = BigDecimal.ZERO;

    @NotNull(message = "Tax amount cannot be null")
    @DecimalMin(value = "0.0", message = "Tax amount must be non-negative")
    @Builder.Default
    @Column(name = "tax_amount", nullable = false, precision = 12, scale = 2)
    private BigDecimal taxAmount = BigDecimal.ZERO;

    @NotNull(message = "Discount amount cannot be null")
    @DecimalMin(value = "0.0", message = "Discount amount must be non-negative")
    @Builder.Default
    @Column(name = "discount_amount", nullable = false, precision = 12, scale = 2)
    private BigDecimal discountAmount = BigDecimal.ZERO;

    @NotNull(message = "Total amount cannot be null")
    @DecimalMin(value = "0.0", message = "Total amount must be non-negative")
    @Builder.Default
    @Column(name = "total_amount", nullable = false, precision = 12, scale = 2)
    private BigDecimal totalAmount = BigDecimal.ZERO;

    @NotBlank(message = "Payment status cannot be blank")
    @Size(max = 50, message = "Payment status cannot exceed 50 characters")
    @Builder.Default
    @Column(name = "payment_status", nullable = false, length = 50)
    private String paymentStatus = "PENDING";

    @NotBlank(message = "Payment method cannot be blank")
    @Size(max = 50, message = "Payment method cannot exceed 50 characters")
    @Column(name = "payment_method", nullable = false, length = 50)
    private String paymentMethod;

    @NotBlank(message = "Order status cannot be blank")
    @Size(max = 50, message = "Order status cannot exceed 50 characters")
    @Builder.Default
    @Column(name = "order_status", nullable = false, length = 50)
    private String orderStatus = "PENDING_APPROVAL";

    @NotBlank(message = "Delivery address cannot be blank")
    @Column(name = "delivery_address", nullable = false, columnDefinition = "TEXT")
    private String deliveryAddress;

    @Size(max = 100, message = "PO number cannot exceed 100 characters")
    @Column(name = "po_number", length = 100)
    private String poNumber;

    @Size(max = 500, message = "Prescription URL cannot exceed 500 characters")
    @Column(name = "prescription_url", length = 500)
    private String prescriptionUrl;

    @Builder.Default
    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<OrderItem> items = new ArrayList<>();

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Order)) return false;
        Order order = (Order) o;
        return getId() != null && getId().equals(order.getId());
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
}
