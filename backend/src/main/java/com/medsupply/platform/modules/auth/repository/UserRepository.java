package com.medsupply.platform.modules.auth.repository;

import com.medsupply.platform.modules.auth.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import java.util.Optional;
import java.util.UUID;

/**
 * Spring Data JPA Repository handling custom query execution on the users table.
 * Automatically enforces the soft deletion filter.
 */
@Repository
public interface UserRepository extends JpaRepository<User, UUID> {

    @Query("SELECT u FROM User u WHERE u.email = :email AND u.isDeleted = false")
    Optional<User> findByEmail(String email);

    @Query("SELECT u FROM User u WHERE u.resetToken = :resetToken AND u.isDeleted = false")
    Optional<User> findByResetToken(String resetToken);

    @Query("SELECT COUNT(u) > 0 FROM User u WHERE u.email = :email AND u.isDeleted = false")
    boolean existsByEmail(String email);
}
