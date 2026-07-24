package com.medsupply.platform.modules.salesman.model;

import com.medsupply.platform.common.model.BaseEntity;
import com.medsupply.platform.modules.auth.model.User;
import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;

/**
 * JPA Entity tracking B2B client onboarding leads and sales pipeline valuation.
 */
@Entity
@Table(name = "salesman_leads")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SalesmanLead extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "salesman_id", nullable = false)
    private User salesman;

    @Column(nullable = false, length = 200)
    private String name;

    @Column(nullable = false)
    private String email;

    @Column(nullable = false, length = 50)
    private String phone;

    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private LeadStatus status = LeadStatus.LEAD;

    @Column(length = 100)
    private String source;

    @Column(length = 200)
    private String company;

    @Builder.Default
    @Column(name = "pipe_value", nullable = false, precision = 12, scale = 2)
    private BigDecimal pipeValue = BigDecimal.ZERO;

    @Column(columnDefinition = "TEXT")
    private String notes;
}
