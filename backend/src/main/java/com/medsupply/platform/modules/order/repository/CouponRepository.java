package com.medsupply.platform.modules.order.repository;

import com.medsupply.platform.modules.order.model.Coupon;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface CouponRepository extends JpaRepository<Coupon, UUID> {
    Optional<Coupon> findByCodeAndIsDeletedFalse(String code);
}
