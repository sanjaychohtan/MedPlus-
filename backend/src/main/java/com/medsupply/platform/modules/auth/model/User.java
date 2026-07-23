package com.medsupply.platform.modules.auth.model;

import com.medsupply.platform.common.model.BaseEntity;
import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.HashSet;
import java.util.Set;

/**
 * Core User Account JPA Entity mapping to the 'users' database table.
 * Supports hospital license registers, credit control lines, OTP states, and roles mapping.
 */
@Entity
@Table(name = "users")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class User extends BaseEntity {

    @Column(nullable = false, unique = true)
    private String email;

    @Column(name = "password_hash", nullable = false)
    private String passwordHash;

    @Column(name = "first_name", nullable = false, length = 100)
    private String firstName;

    @Column(name = "last_name", nullable = false, length = 100)
    private String lastName;

    @Column(length = 20)
    private String phone;

    // Hospital / Clinic drug license and B2B CGST/SGST invoicing identifiers
    @Column(name = "license_number", length = 100)
    private String licenseNumber;

    @Column(length = 20)
    private String gstin;

    @Builder.Default
    @Column(name = "credit_limit", nullable = false, precision = 12, scale = 2)
    private BigDecimal creditLimit = BigDecimal.ZERO;

    @Builder.Default
    @Column(name = "outstanding_balance", nullable = false, precision = 12, scale = 2)
    private BigDecimal outstandingBalance = BigDecimal.ZERO;

    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private UserStatus status = UserStatus.PENDING_APPROVAL;

    // Secure Verification Telemetries
    @Column(name = "otp_code", length = 6)
    private String otpCode;

    @Column(name = "otp_expiry")
    private OffsetDateTime otpExpiry;

    @Column(name = "reset_token", length = 100)
    private String resetToken;

    @Column(name = "reset_token_expiry")
    private OffsetDateTime resetTokenExpiry;

    @Builder.Default
    @Column(name = "failed_login_attempts", nullable = false)
    private int failedLoginAttempts = 0;

    @Builder.Default
    @ManyToMany(fetch = FetchType.EAGER)
    @JoinTable(
        name = "user_roles",
        joinColumns = @JoinColumn(name = "user_id"),
        inverseJoinColumns = @JoinColumn(name = "role_id")
    )
    private Set<Role> roles = new HashSet<>();

    /**
     * Checks if the user is verified and has an active account.
     */
    public boolean isActive() {
        return this.status == UserStatus.ACTIVE;
    }

    /**
     * Increments the failed login tracker.
     */
    public void incrementFailedLogins() {
        this.failedLoginAttempts++;
    }

    /**
     * Resets the failed login attempts.
     */
    public void resetFailedLogins() {
        this.failedLoginAttempts = 0;
    }

    /**
     * Checks if the user holds the given UserRole.
     */
    public boolean hasRole(UserRole roleName) {
        return this.roles.stream()
                .anyMatch(r -> r.getName().equals(roleName));
    }
}
