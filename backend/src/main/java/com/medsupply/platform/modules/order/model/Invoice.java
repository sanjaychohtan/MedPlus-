package com.medsupply.platform.modules.order.model;

import com.medsupply.platform.modules.auth.model.User;
import jakarta.persistence.*;
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
public class Invoice {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @Column(name = "invoice_number", nullable = false, unique = true, length = 50)
    private String invoiceNumber;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id", nullable = false)
    private Order order;

    @Column(name = "order_number", nullable = false, length = 50)
    private String orderNumber;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "customer_id", nullable = false)
    private User customer;

    @Column(length = 20)
    private String gstin;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal subtotal;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal cgst;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal sgst;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal igst;

    @Column(name = "total_amount", nullable = false, precision = 12, scale = 2)
    private BigDecimal totalAmount;

    @Column(name = "pdf_generated_at", nullable = false)
    private OffsetDateTime pdfGeneratedAt;

    @Column(name = "payment_due_date", nullable = false)
    private OffsetDateTime paymentDueDate;

    @Builder.Default
    @Column(nullable = false, length = 50)
    private String status = "UNPAID";
}
