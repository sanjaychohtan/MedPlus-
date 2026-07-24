package com.medsupply.platform.modules.order.model;

import com.medsupply.platform.modules.inventory.model.Batch;
import com.medsupply.platform.modules.inventory.model.Product;
import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.*;
import java.math.BigDecimal;
import java.util.UUID;

/**
 * JPA Entity representing an individual line-item of an order.
 */
@Entity
@Table(name = "order_items")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@ToString(exclude = {"order", "product", "batch"})
public class OrderItem {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @Column(name = "version")
    @Version
    private Long version;

    @NotNull(message = "Order cannot be null")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id", nullable = false)
    private Order order;

    @NotNull(message = "Product cannot be null")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "batch_id")
    private Batch batch;

    @NotBlank(message = "Product name cannot be blank")
    @Column(name = "product_name", nullable = false)
    private String productName;

    @NotBlank(message = "Product SKU cannot be blank")
    @Size(max = 100, message = "Product SKU cannot exceed 100 characters")
    @Pattern(regexp = "^[A-Z0-9-]+$", message = "SKU must contain only uppercase alphanumeric characters and hyphens")
    @Column(name = "product_sku", nullable = false, length = 100)
    private String productSku;

    @Size(max = 100, message = "Batch number cannot exceed 100 characters")
    @Column(name = "batch_number", length = 100)
    private String batchNumber;

    @Min(value = 1, message = "Quantity must be at least 1")
    @Column(nullable = false)
    private int quantity;

    @NotNull(message = "Unit price cannot be null")
    @DecimalMin(value = "0.0", message = "Unit price must be non-negative")
    @Column(name = "unit_price", nullable = false, precision = 12, scale = 2)
    private BigDecimal unitPrice;

    @NotNull(message = "MRP cannot be null")
    @DecimalMin(value = "0.0", message = "MRP must be non-negative")
    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal mrp;

    @NotNull(message = "Tax rate cannot be null")
    @DecimalMin(value = "0.0", message = "Tax rate must be non-negative")
    @DecimalMax(value = "100.0", message = "Tax rate cannot exceed 100%")
    @Column(name = "tax_rate", nullable = false, precision = 5, scale = 2)
    private BigDecimal taxRate;

    @NotNull(message = "Tax amount cannot be null")
    @DecimalMin(value = "0.0", message = "Tax amount must be non-negative")
    @Column(name = "tax_amount", nullable = false, precision = 12, scale = 2)
    private BigDecimal taxAmount;

    @NotNull(message = "Total price cannot be null")
    @DecimalMin(value = "0.0", message = "Total price must be non-negative")
    @Column(name = "total_price", nullable = false, precision = 12, scale = 2)
    private BigDecimal totalPrice;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof OrderItem)) return false;
        OrderItem that = (OrderItem) o;
        return id != null && id.equals(that.id);
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
}
