package com.medsupply.platform.modules.delivery.service;

import com.medsupply.platform.modules.auth.model.User;
import com.medsupply.platform.modules.auth.model.UserStatus;
import com.medsupply.platform.modules.auth.repository.UserRepository;
import com.medsupply.platform.modules.order.model.Order;
import com.medsupply.platform.modules.order.repository.OrderRepository;
import com.medsupply.platform.modules.order.service.OrderService;
import com.medsupply.platform.modules.delivery.model.DeliveryTask;
import com.medsupply.platform.modules.delivery.model.DeliveryStatus;
import com.medsupply.platform.modules.delivery.repository.DeliveryTaskRepository;
import com.medsupply.platform.common.exception.DomainException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.http.HttpStatus;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.security.SecureRandom;
import java.security.MessageDigest;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class DeliveryServiceImpl implements DeliveryService {

    private final DeliveryTaskRepository deliveryTaskRepository;
    private final OrderRepository orderRepository;
    private final UserRepository userRepository;
    private final OrderService orderService;
    private final com.medsupply.platform.modules.audit.service.AuditLogService auditLogService;

    @org.springframework.beans.factory.annotation.Autowired(required = false)
    private jakarta.servlet.http.HttpServletRequest httpServletRequest;

    private static final SecureRandom secureRandom = new SecureRandom();

    private User getCurrentUser() {
        if (userRepository == null) return null;
        try {
            org.springframework.security.core.Authentication auth = org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication();
            if (auth != null && auth.isAuthenticated() && !"anonymousUser".equals(auth.getName())) {
                return userRepository.findByEmail(auth.getName()).orElse(null);
            }
        } catch (Exception e) {
            // handle exception gracefully
        }
        return null;
    }

    private String getCurrentUserRole(User user) {
        if (user == null || user.getRoles() == null || user.getRoles().isEmpty()) {
            return "ANONYMOUS";
        }
        return "ROLE_" + user.getRoles().iterator().next().getName().name();
    }

    private String getClientIp() {
        if (httpServletRequest == null) return "127.0.0.1";
        try {
            String ipList = httpServletRequest.getHeader("X-Forwarded-For");
            if (ipList != null && !ipList.isEmpty()) {
                return ipList.split(",")[0].trim();
            }
            return httpServletRequest.getRemoteAddr();
        } catch (Exception e) {
            return "127.0.0.1";
        }
    }

    private String hashOtp(String otp) {
        if (otp == null) return null;
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(otp.getBytes(StandardCharsets.UTF_8));
            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) hexString.append('0');
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (Exception e) {
            throw new RuntimeException("Error hashing OTP", e);
        }
    }

    private boolean slowEquals(String a, String b) {
        if (a == null || b == null) return false;
        return MessageDigest.isEqual(
            a.getBytes(StandardCharsets.UTF_8), 
            b.getBytes(StandardCharsets.UTF_8)
        );
    }

    private void validateCoordinates(BigDecimal lat, BigDecimal lng) {
        if (lat == null || lng == null) {
            throw new DomainException("INVALID_GPS", "GPS coordinates cannot be null", HttpStatus.BAD_REQUEST);
        }
        if (lat.compareTo(new BigDecimal("-90")) < 0 || lat.compareTo(new BigDecimal("90")) > 0) {
            throw new DomainException("INVALID_LATITUDE", "Latitude must be between -90 and 90. Got: " + lat, HttpStatus.BAD_REQUEST);
        }
        if (lng.compareTo(new BigDecimal("-180")) < 0 || lng.compareTo(new BigDecimal("180")) > 0) {
            throw new DomainException("INVALID_LONGITUDE", "Longitude must be between -180 and 180. Got: " + lng, HttpStatus.BAD_REQUEST);
        }
    }

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
                .orElseThrow(() -> new DomainException("ORDER_NOT_FOUND", "Order not found with ID: " + orderId, HttpStatus.NOT_FOUND));
        
        if ("DELIVERED".equals(order.getOrderStatus())) {
            throw new DomainException("ORDER_ALREADY_DELIVERED", "Cannot assign delivery task. The order is already delivered.", HttpStatus.BAD_REQUEST);
        }

        User driver = userRepository.findById(driverId)
                .orElseThrow(() -> new DomainException("DRIVER_NOT_FOUND", "Driver not found with ID: " + driverId, HttpStatus.NOT_FOUND));

        if (driver.getStatus() != UserStatus.ACTIVE) {
            throw new DomainException("INACTIVE_DRIVER", "Cannot assign task. Driver account is not active. Current status: " + driver.getStatus(), HttpStatus.BAD_REQUEST);
        }

        if (order.getCustomer() == null || order.getCustomer().getPhone() == null || order.getCustomer().getPhone().isEmpty()) {
            throw new DomainException("MISSING_PHONE", "Customer has no registered phone number for delivery task creation", HttpStatus.BAD_REQUEST);
        }

        // Generate enterprise-safe UUID/sequence delivery number
        String deliveryNumber = "DEL-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        
        // Generate a random 4-digit verification code using SecureRandom
        String otpCode = String.format("%04d", secureRandom.nextInt(10000));
        String hashedOtp = hashOtp(otpCode);

        DeliveryTask task = DeliveryTask.builder()
                .deliveryNumber(deliveryNumber)
                .order(order)
                .orderNumber(order.getOrderNumber())
                .deliveryBoy(driver)
                .deliveryBoyName(driver.getFirstName() + " " + driver.getLastName())
                .customerName(order.getCustomerName())
                .phone(order.getCustomer().getPhone())
                .deliveryAddress(order.getDeliveryAddress())
                .currentLat(BigDecimal.ZERO) // Start clean without hardcoded seed
                .currentLng(BigDecimal.ZERO)
                .estimatedArrivalMinutes(30)
                .status(DeliveryStatus.ASSIGNED.name())
                .otpCode(hashedOtp)
                .otpExpiry(OffsetDateTime.now().plusMinutes(15))
                .otpAttempts(0)
                .build();

        task.setTransientOtp(otpCode); // Set transient plain OTP for secure handover feedback
        DeliveryTask savedTask = deliveryTaskRepository.save(task);

        // Audit Logging
        User executor = getCurrentUser();
        UUID executorId = executor != null ? executor.getId() : null;
        String executorRole = getCurrentUserRole(executor);
        auditLogService.log(executorId, executorRole, "DELIVERY_ASSIGNED", "DELIVERY", 
                "Delivery task '" + deliveryNumber + "' assigned to driver '" + driver.getEmail() + "' for order '" + order.getOrderNumber() + "'", getClientIp());

        return savedTask;
    }

    @Override
    public DeliveryTask updateLocation(UUID taskId, BigDecimal lat, BigDecimal lng, int minutes) {
        validateCoordinates(lat, lng);

        DeliveryTask task = deliveryTaskRepository.findById(taskId)
                .orElseThrow(() -> new DomainException("TASK_NOT_FOUND", "Delivery task not found with ID: " + taskId, HttpStatus.NOT_FOUND));

        if (DeliveryStatus.COMPLETED.name().equals(task.getStatus()) || DeliveryStatus.FAILED.name().equals(task.getStatus())) {
            throw new DomainException("INVALID_TASK_STATE", "Cannot update location on a finalized task. Status is: " + task.getStatus(), HttpStatus.BAD_REQUEST);
        }

        task.setCurrentLat(lat);
        task.setCurrentLng(lng);
        task.setEstimatedArrivalMinutes(minutes);

        boolean isStarting = DeliveryStatus.ASSIGNED.name().equals(task.getStatus());
        task.setStatus(DeliveryStatus.TRANSIT.name());
        
        DeliveryTask savedTask = deliveryTaskRepository.save(task);

        // Audit Logging
        User executor = getCurrentUser();
        UUID executorId = executor != null ? executor.getId() : null;
        String executorRole = getCurrentUserRole(executor);
        if (isStarting) {
            auditLogService.log(executorId, executorRole, "DELIVERY_STARTED", "DELIVERY", 
                    "Delivery task '" + task.getDeliveryNumber() + "' started transit", getClientIp());
        }

        return savedTask;
    }

    @Override
    public DeliveryTask completeTask(UUID taskId, String otpCode) {
        DeliveryTask task = deliveryTaskRepository.findById(taskId)
                .orElseThrow(() -> new DomainException("TASK_NOT_FOUND", "Delivery task not found with ID: " + taskId, HttpStatus.NOT_FOUND));

        // 1. Prevent completed tasks from being completed twice
        if (DeliveryStatus.COMPLETED.name().equals(task.getStatus())) {
            throw new DomainException("TASK_ALREADY_COMPLETED", "This delivery task has already been completed.", HttpStatus.BAD_REQUEST);
        }

        // 2. Verify only assigned driver can complete the task
        User currentUser = getCurrentUser();
        if (currentUser == null) {
            throw new DomainException("UNAUTHORIZED", "Authentication required to complete delivery task.", HttpStatus.UNAUTHORIZED);
        }
        boolean isAdmin = currentUser.getRoles().stream()
                .anyMatch(r -> "ADMIN".equals(r.getName().name()));
        if (!isAdmin && !task.getDeliveryBoy().getId().equals(currentUser.getId())) {
            throw new DomainException("UNAUTHORIZED_DRIVER", "Only the assigned driver or an administrator can complete this task.", HttpStatus.FORBIDDEN);
        }

        // 3. Lockout validation
        OffsetDateTime now = OffsetDateTime.now();
        if (task.getLockoutTime() != null && task.getLockoutTime().isAfter(now)) {
            throw new DomainException("VERIFICATION_LOCKED", "OTP verification is locked due to too many failed attempts. Try again later.", HttpStatus.TOO_MANY_REQUESTS);
        }

        // 4. Expiry validation
        if (task.getOtpExpiry() != null && task.getOtpExpiry().isBefore(now)) {
            auditLogService.log(currentUser.getId(), getCurrentUserRole(currentUser), "DELIVERY_FAILED", "DELIVERY", 
                    "Delivery completion failed for '" + task.getDeliveryNumber() + "' due to expired OTP", getClientIp());
            throw new DomainException("OTP_EXPIRED", "The verification OTP has expired.", HttpStatus.BAD_REQUEST);
        }

        // 5. OTP verification with constant-time comparison
        String incomingHash = hashOtp(otpCode);
        if (!slowEquals(incomingHash, task.getOtpCode())) {
            int attempts = task.getOtpAttempts() + 1;
            task.setOtpAttempts(attempts);
            
            if (attempts >= 3) {
                task.setLockoutTime(now.plusMinutes(15));
                deliveryTaskRepository.save(task);
                auditLogService.log(currentUser.getId(), getCurrentUserRole(currentUser), "DELIVERY_FAILED", "DELIVERY", 
                        "Delivery '" + task.getDeliveryNumber() + "' locked out after 3 failed OTP attempts", getClientIp());
                throw new DomainException("VERIFICATION_LOCKED_OUT", "Invalid OTP. Too many failed attempts. Verification locked for 15 minutes.", HttpStatus.TOO_MANY_REQUESTS);
            } else {
                deliveryTaskRepository.save(task);
                auditLogService.log(currentUser.getId(), getCurrentUserRole(currentUser), "OTP_FAILED", "DELIVERY", 
                        "Invalid OTP attempt (" + attempts + "/3) for delivery '" + task.getDeliveryNumber() + "'", getClientIp());
                throw new DomainException("INVALID_OTP", "Security Handover OTP verification failed. Invalid OTP code.", HttpStatus.BAD_REQUEST);
            }
        }

        // OTP verified successfully
        auditLogService.log(currentUser.getId(), getCurrentUserRole(currentUser), "OTP_VERIFIED", "DELIVERY", 
                "OTP verified successfully for delivery '" + task.getDeliveryNumber() + "'", getClientIp());

        task.setStatus(DeliveryStatus.COMPLETED.name());
        task.setOtpAttempts(0);
        task.setLockoutTime(null);
        
        // Complete corresponding order status too (calling OrderService to trigger FEFO and inventory updates)
        Order order = task.getOrder();
        orderService.updateOrderStatus(order.getId(), "DELIVERED");
        order.setPaymentStatus("PAID"); // Assuming paid on delivery verification
        orderRepository.save(order);

        DeliveryTask completedTask = deliveryTaskRepository.save(task);

        // Log final task completion
        auditLogService.log(currentUser.getId(), getCurrentUserRole(currentUser), "DELIVERY_COMPLETED", "DELIVERY", 
                "Delivery task '" + task.getDeliveryNumber() + "' completed successfully and order marked as DELIVERED", getClientIp());

        return completedTask;
    }
}
