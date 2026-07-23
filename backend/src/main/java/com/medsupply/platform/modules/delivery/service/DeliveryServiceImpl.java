package com.medsupply.platform.modules.delivery.service;

import com.medsupply.platform.modules.auth.model.User;
import com.medsupply.platform.modules.auth.repository.UserRepository;
import com.medsupply.platform.modules.order.model.Order;
import com.medsupply.platform.modules.order.repository.OrderRepository;
import com.medsupply.platform.modules.order.service.OrderService;
import com.medsupply.platform.modules.delivery.model.DeliveryTask;
import com.medsupply.platform.modules.delivery.repository.DeliveryTaskRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional
public class DeliveryServiceImpl implements DeliveryService {

    private final DeliveryTaskRepository deliveryTaskRepository;
    private final OrderRepository orderRepository;
    private final UserRepository userRepository;
    private final OrderService orderService;

    @Override
    @Transactional(readOnly = true)
    public List<DeliveryTask> getAllTasks() {
        return deliveryTaskRepository.findAll().stream()
                .filter(t -> !t.isDeleted())
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<DeliveryTask> getTasksByDriver(UUID driverId) {
        return deliveryTaskRepository.findByDeliveryBoyIdAndIsDeletedFalse(driverId);
    }

    @Override
    public DeliveryTask createTask(UUID orderId, UUID driverId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new IllegalArgumentException("Order not found: " + orderId));
        User driver = userRepository.findById(driverId)
                .orElseThrow(() -> new IllegalArgumentException("Driver not found: " + driverId));

        String deliveryNumber = "DEL-" + System.currentTimeMillis();
        
        // Generate a random 4-digit verification code
        String otpCode = String.format("%04d", (int) (Math.random() * 10000));

        DeliveryTask task = DeliveryTask.builder()
                .deliveryNumber(deliveryNumber)
                .order(order)
                .orderNumber(order.getOrderNumber())
                .deliveryBoy(driver)
                .deliveryBoyName(driver.getFirstName() + " " + driver.getLastName())
                .customerName(order.getCustomerName())
                .phone(order.getCustomer().getPhone() != null ? order.getCustomer().getPhone() : "555-0199")
                .deliveryAddress(order.getDeliveryAddress())
                .currentLat(new BigDecimal("12.9716")) // Seed Default Bengaluru
                .currentLng(new BigDecimal("77.5946"))
                .estimatedArrivalMinutes(30)
                .status("ASSIGNED")
                .otpCode(otpCode)
                .build();

        return deliveryTaskRepository.save(task);
    }

    @Override
    public DeliveryTask updateLocation(UUID taskId, BigDecimal lat, BigDecimal lng, int minutes) {
        DeliveryTask task = deliveryTaskRepository.findById(taskId)
                .orElseThrow(() -> new IllegalArgumentException("Task not found: " + taskId));
        task.setCurrentLat(lat);
        task.setCurrentLng(lng);
        task.setEstimatedArrivalMinutes(minutes);
        task.setStatus("TRANSIT");
        return deliveryTaskRepository.save(task);
    }

    @Override
    public DeliveryTask completeTask(UUID taskId, String otpCode) {
        DeliveryTask task = deliveryTaskRepository.findById(taskId)
                .orElseThrow(() -> new IllegalArgumentException("Task not found: " + taskId));

        if (!task.getOtpCode().equals(otpCode)) {
            throw new IllegalArgumentException("Security Handover OTP verification failed. Invalid OTP code.");
        }

        task.setStatus("COMPLETED");
        
        // Complete corresponding order status too (calling OrderService to trigger FEFO and inventory updates)
        Order order = task.getOrder();
        orderService.updateOrderStatus(order.getId(), "DELIVERED");
        order.setPaymentStatus("PAID"); // Assuming paid on delivery verification
        orderRepository.save(order);

        return deliveryTaskRepository.save(task);
    }
}
