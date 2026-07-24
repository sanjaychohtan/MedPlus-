package com.medsupply.platform.modules.order.model;

import com.medsupply.platform.modules.auth.model.User;
import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * JPA Entity tracking legal tax invoices for orders.
 * Supports split GST taxes (CGST/SGST/IGST) and due date tracking for credit.
 */
@Entity
@Table(name = "invoices")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@ToString(exclude = {"order", "customer"})
public class Invoice {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @Column(name = "version")
    @Version
    private Long version;

    @NotBlank(message = "Invoice number cannot be blank")
    @Size(max = 50, message = "Invoice number cannot exceed 50 characters")
    @Column(name = "invoice_number", nullable = false, unique = true, length = 50)
    private String invoiceNumber;

    @NotNull(message = "Order cannot be null")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id", nullable = false)
    private Order order;

    @NotBlank(message = "Order number cannot be blank")
    @Size(max = 50, message = "Order number cannot exceed 50 characters")
    @Column(name = "order_number", nullable = false, length = 50)
    private String orderNumber;

    @NotNull(message = "Customer cannot be null")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "customer_id", nullable = false)
    private User customer;

    @Size(max = 20, message = "GSTIN cannot exceed 20 characters")
    @Pattern(regexp = "^([0-9]{2}[A-Z]{5}[0-9]{4}[A-Z]{1}[1-9A-Z]{1}Z[0-9A-Z]{1})?$", message = "Invalid GSTIN format")
    @Column(length = 20)
    private String gstin;

    @NotNull(message = "Subtotal cannot be null")
    @DecimalMin(value = "0.0", message = "Subtotal must be non-negative")
    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal subtotal;

    @NotNull(message = "CGST cannot be null")
    @DecimalMin(value = "0.0", message = "CGST must be non-negative")
    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal cgst;

    @NotNull(message = "SGST cannot be null")
    @DecimalMin(value = "0.0", message = "SGST must be non-negative")
    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal sgst;

    @NotNull(message = "IGST cannot be null")
    @DecimalMin(value = "0.0", message = "IGST must be non-negative")
    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal igst;

    @NotNull(message = "Total amount cannot be null")
    @DecimalMin(value = "0.0", message = "Total amount must be non-negative")
    @Column(name = "total_amount", nullable = false, precision = 12, scale = 2)
    private BigDecimal totalAmount;

    @NotNull(message = "PDF generated at cannot be null")
    @Column(name = "pdf_generated_at", nullable = false)
    private OffsetDateTime pdfGeneratedAt;

    @NotNull(message = "Payment due date cannot be null")
    @Column(name = "payment_due_date", nullable = false)
    private OffsetDateTime paymentDueDate;

    @NotBlank(message = "Status cannot be blank")
    @Size(max = 50, message = "Status cannot exceed 50 characters")
    @Builder.Default
    @Column(nullable = false, length = 50)
    private String status = "UNPAID";

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Invoice)) return false;
        Invoice invoice = (Invoice) o;
        return id != null && id.equals(invoice.id);
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
}
