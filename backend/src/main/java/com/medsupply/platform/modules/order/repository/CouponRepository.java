package com.medsupply.platform.modules.order.repository;

import com.medsupply.platform.modules.order.model.Coupon;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface CouponRepository extends JpaRepository<Coupon, UUID> {
    Optional<Coupon> findByCodeAndIsDeletedFalse(String code);

    @org.springframework.data.jpa.repository.Lock(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE)
    @org.springframework.data.jpa.repository.Query("SELECT c FROM Coupon c WHERE c.code = :code AND c.isDeleted = false")
    Optional<Coupon> findByCodeAndIsDeletedFalseWithLock(@org.springframework.data.repository.query.Param("code") String code);
}
