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
}
