package com.medsupply.platform.modules.delivery.repository;

import com.medsupply.platform.modules.delivery.model.DeliveryTask;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.UUID;

@Repository
public interface DeliveryTaskRepository extends JpaRepository<DeliveryTask, UUID> {
    List<DeliveryTask> findByDeliveryBoyIdAndIsDeletedFalse(UUID deliveryBoyId);
}
