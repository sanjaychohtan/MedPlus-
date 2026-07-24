package com.medsupply.platform.modules.order.repository;

import com.medsupply.platform.modules.order.model.Invoice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.UUID;

@Repository
public interface InvoiceRepository extends JpaRepository<Invoice, UUID> {
    List<Invoice> findByCustomerId(UUID customerId);
    java.util.Optional<Invoice> findByOrderId(UUID orderId);

    @org.springframework.data.jpa.repository.Lock(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE)
    @org.springframework.data.jpa.repository.Query("SELECT i FROM Invoice i WHERE i.order.id = :orderId")
    java.util.Optional<Invoice> findByOrderIdWithLock(@org.springframework.data.repository.query.Param("orderId") UUID orderId);
}
