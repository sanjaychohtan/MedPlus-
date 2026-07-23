package com.medsupply.platform.modules.audit.repository;

import com.medsupply.platform.modules.audit.model.AuditLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.UUID;

/**
 * Spring Data JPA Repository interface handling query operations against 'audit_logs' table.
 */
@Repository
public interface AuditLogRepository extends JpaRepository<AuditLog, UUID> {

    Page<AuditLog> findByModuleName(String moduleName, Pageable pageable);

    Page<AuditLog> findByExecutorId(UUID executorId, Pageable pageable);
}
