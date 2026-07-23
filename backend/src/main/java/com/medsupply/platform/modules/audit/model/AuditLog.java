package com.medsupply.platform.modules.audit.model;

import jakarta.persistence.*;
import lombok.*;
import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * JPA Entity mapping to 'audit_logs' database table.
 * Serves as our permanent, immutable ledger recording every security and structural change in the system.
 */
@Entity
@Table(name = "audit_logs")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AuditLog {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID id;

    @Column(name = "executor_id")
    private UUID executorId;

    @Column(name = "executor_role", length = 50)
    private String executorRole;

    @Column(nullable = false, length = 100)
    private String action;

    @Column(name = "module_name", nullable = false, length = 100)
    private String moduleName;

    @Column(columnDefinition = "TEXT")
    private String details;

    @Column(name = "client_ip", nullable = false, length = 45)
    private String clientIp;

    @Builder.Default
    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt = OffsetDateTime.now();

    @PrePersist
    protected void onCreate() {
        this.createdAt = OffsetDateTime.now();
    }
}
