package com.medsupply.platform.modules.delivery.service;

import com.medsupply.platform.modules.delivery.model.DeliveryTask;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public interface DeliveryService {
    List<DeliveryTask> getAllTasks();
    List<DeliveryTask> getTasksByDriver(UUID driverId);
    DeliveryTask createTask(UUID orderId, UUID driverId);
    DeliveryTask updateLocation(UUID taskId, BigDecimal lat, BigDecimal lng, int minutes);
    DeliveryTask completeTask(UUID taskId, String otpCode);
}
