package com.medsupply.platform.modules.auth.repository;

import com.medsupply.platform.modules.auth.model.Role;
import com.medsupply.platform.modules.auth.model.UserRole;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;
import java.util.UUID;

/**
 * Spring Data JPA Repository mapping core database queries to the roles table.
 */
@Repository
public interface RoleRepository extends JpaRepository<Role, UUID> {

    Optional<Role> findByName(UserRole name);
}
