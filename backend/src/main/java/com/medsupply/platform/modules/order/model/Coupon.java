package com.medsupply.platform.modules.order.model;

import com.medsupply.platform.common.model.BaseEntity;
import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.*;
import java.math.BigDecimal;

/**
 * JPA Entity tracking promotional coupons for discount verification.
 */
@Entity
@Table(name = "coupons")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@ToString(callSuper = true)
public class Coupon extends BaseEntity {

    @Column(name = "version")
    @Version
    private Long version;

    @NotBlank(message = "Coupon code cannot be blank")
    @Size(max = 50, message = "Coupon code cannot exceed 50 characters")
    @Pattern(regexp = "^[A-Z0-9_-]+$", message = "Coupon code must contain only uppercase alphanumeric characters, underscores, and hyphens")
    @Column(nullable = false, unique = true, length = 50)
    private String code;

    @NotNull(message = "Discount percentage cannot be null")
    @DecimalMin(value = "0.0", message = "Discount percentage must be non-negative")
    @DecimalMax(value = "100.0", message = "Discount percentage cannot exceed 100%")
    @Column(name = "discount_percent", nullable = false, precision = 5, scale = 2)
    private BigDecimal discountPercent;

    @NotNull(message = "Maximum discount cannot be null")
    @DecimalMin(value = "0.0", message = "Maximum discount must be non-negative")
    @Column(name = "max_discount", nullable = false, precision = 12, scale = 2)
    private BigDecimal maxDiscount;

    @NotNull(message = "Minimum order amount cannot be null")
    @DecimalMin(value = "0.0", message = "Minimum order amount must be non-negative")
    @Column(name = "min_order_amount", nullable = false, precision = 12, scale = 2)
    private BigDecimal minOrderAmount;

    @Builder.Default
    @Column(nullable = false)
    private boolean active = true;

    @Min(value = 0, message = "Usage count cannot be negative")
    @Builder.Default
    @Column(name = "usage_count", nullable = false)
    private int usageCount = 0;

    @Column(name = "expiry_date")
    private java.time.LocalDate expiryDate;

    @Min(value = 1, message = "Usage limit must be at least 1")
    @Column(name = "usage_limit")
    private Integer usageLimit;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Coupon)) return false;
        Coupon coupon = (Coupon) o;
        return getId() != null && getId().equals(coupon.getId());
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
}
