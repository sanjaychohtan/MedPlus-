package com.medsupply.platform.modules.inventory.repository;

import com.medsupply.platform.modules.inventory.model.Category;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.UUID;

@Repository
public interface CategoryRepository extends JpaRepository<Category, UUID> {
    boolean existsByNameIgnoreCaseAndIsDeletedFalse(String name);
}
