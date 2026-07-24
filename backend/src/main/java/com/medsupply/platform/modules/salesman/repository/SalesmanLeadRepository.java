package com.medsupply.platform.modules.salesman.repository;

import com.medsupply.platform.modules.salesman.model.SalesmanLead;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.UUID;

@Repository
public interface SalesmanLeadRepository extends JpaRepository<SalesmanLead, UUID> {
    List<SalesmanLead> findByIsDeletedFalse();
    org.springframework.data.domain.Page<SalesmanLead> findByIsDeletedFalse(org.springframework.data.domain.Pageable pageable);
    List<SalesmanLead> findBySalesmanIdAndIsDeletedFalse(UUID salesmanId);
    boolean existsByEmailIgnoreCaseAndIsDeletedFalse(String email);
    boolean existsByPhoneAndIsDeletedFalse(String phone);
}
