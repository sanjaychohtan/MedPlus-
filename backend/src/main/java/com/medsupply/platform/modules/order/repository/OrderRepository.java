package com.medsupply.platform.modules.order.repository;

import com.medsupply.platform.modules.order.model.Order;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.UUID;

@Repository
public interface OrderRepository extends JpaRepository<Order, UUID> {
    List<Order> findByCustomerIdAndIsDeletedFalse(UUID customerId);
    List<Order> findByOrderTypeAndIsDeletedFalse(String orderType);
    List<Order> findByOrderStatusAndIsDeletedFalse(String orderStatus);

    @org.springframework.data.jpa.repository.Lock(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE)
    @org.springframework.data.jpa.repository.Query("SELECT o FROM Order o WHERE o.id = :id AND o.isDeleted = false")
    java.util.Optional<Order> findByIdWithLock(@org.springframework.data.repository.query.Param("id") UUID id);

    boolean existsByCustomerAndPoNumberAndIsDeletedFalse(com.medsupply.platform.modules.auth.model.User customer, String poNumber);
}
