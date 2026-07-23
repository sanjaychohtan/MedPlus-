package com.medsupply.platform.modules.audit.service;

import com.medsupply.platform.modules.audit.model.AuditLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import java.util.UUID;

/**
 * Audit Logging Service interface.
 * Exposes methods to append entries to our security ledger and pull paginated traces.
 */
public interface AuditLogService {

    /**
     * Appends an active, immutable entry into the database ledger.
     */
    void log(UUID executorId, String executorRole, String action, String moduleName, String details, String clientIp);

    /**
     * Gets paginated logs by specific module name.
     */
    Page<AuditLog> getLogsByModule(String moduleName, Pageable pageable);

    /**
     * Gets paginated logs executed by a specific user.
     */
    Page<AuditLog> getLogsByExecutor(UUID executorId, Pageable pageable);

    /**
     * Pulls full paginated system audit history.
     */
    Page<AuditLog> getAllLogs(Pageable pageable);
}
