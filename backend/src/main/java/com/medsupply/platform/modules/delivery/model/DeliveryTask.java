package com.medsupply.platform.modules.delivery.model;

import com.medsupply.platform.common.model.BaseEntity;
import com.medsupply.platform.modules.auth.model.User;
import com.medsupply.platform.modules.order.model.Order;
import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;

/**
 * JPA Entity tracking courier routes, live GPS coordinates, and 4-digit handover OTP verifications.
 */
@Entity
@Table(name = "delivery_tasks")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DeliveryTask extends BaseEntity {

    @Column(name = "delivery_number", nullable = false, unique = true, length = 50)
    private String deliveryNumber;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id", nullable = false)
    private Order order;

    @Column(name = "order_number", nullable = false, length = 50)
    private String orderNumber;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "delivery_boy_id", nullable = false)
    private User deliveryBoy;

    @Column(name = "delivery_boy_name", nullable = false, length = 200)
    private String deliveryBoyName;

    @Column(name = "customer_name", nullable = false, length = 200)
    private String customerName;

    @Column(nullable = false, length = 50)
    private String phone;

    @Column(name = "delivery_address", nullable = false, columnDefinition = "TEXT")
    private String deliveryAddress;

    @Builder.Default
    @Column(name = "current_lat", nullable = false, precision = 10, scale = 6)
    private BigDecimal currentLat = BigDecimal.ZERO;

    @Builder.Default
    @Column(name = "current_lng", nullable = false, precision = 10, scale = 6)
    private BigDecimal currentLng = BigDecimal.ZERO;

    @Builder.Default
    @Column(name = "estimated_arrival_minutes", nullable = false)
    private int estimatedArrivalMinutes = 30;

    @Builder.Default
    @Column(nullable = false, length = 50)
    private String status = "ASSIGNED";

    @Column(name = "otp_code", nullable = false, length = 64) // 64 to store SHA-256 hash
    private String otpCode;

    @Transient
    private String transientOtp;

    @Column(name = "otp_expiry")
    private java.time.OffsetDateTime otpExpiry;

    @Builder.Default
    @Column(name = "otp_attempts", nullable = false)
    private int otpAttempts = 0;

    @Column(name = "lockout_time")
    private java.time.OffsetDateTime lockoutTime;
}
