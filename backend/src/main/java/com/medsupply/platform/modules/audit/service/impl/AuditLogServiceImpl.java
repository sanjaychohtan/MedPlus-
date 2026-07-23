package com.medsupply.platform.modules.audit.service.impl;

import com.medsupply.platform.modules.audit.model.AuditLog;
import com.medsupply.platform.modules.audit.repository.AuditLogRepository;
import com.medsupply.platform.modules.audit.service.AuditLogService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import java.util.UUID;

/**
 * Concrete implementation of AuditLogService.
 * Uses REQUIRES_NEW transaction propagation to protect log writes from parent rollbacks.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AuditLogServiceImpl implements AuditLogService {

    private final AuditLogRepository auditLogRepository;

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void log(UUID executorId, String executorRole, String action, String moduleName, String details, String clientIp) {
        try {
            AuditLog auditEntry = AuditLog.builder()
                    .executorId(executorId)
                    .executorRole(executorRole)
                    .action(action)
                    .moduleName(moduleName)
                    .details(details)
                    .clientIp(clientIp)
                    .build();

            auditLogRepository.save(auditEntry);
            log.debug("AuditLog saved successfully: Action '{}' in Module '{}'", action, moduleName);
        } catch (Exception e) {
            // Never crash the primary transaction if the audit logger experiences an DB failure
            log.error("CRITICAL: Failed to write to immutable audit ledger: ", e);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public Page<AuditLog> getLogsByModule(String moduleName, Pageable pageable) {
        return auditLogRepository.findByModuleName(moduleName, pageable);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<AuditLog> getLogsByExecutor(UUID executorId, Pageable pageable) {
        return auditLogRepository.findByExecutorId(executorId, pageable);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<AuditLog> getAllLogs(Pageable pageable) {
        return auditLogRepository.findAll(pageable);
    }
}
