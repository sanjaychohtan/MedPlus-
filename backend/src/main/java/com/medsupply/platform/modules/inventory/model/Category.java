package com.medsupply.platform.modules.inventory.model;

import com.medsupply.platform.common.model.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

/**
 * JPA Entity representing a medical supply product category.
 */
@Entity
@Table(name = "categories")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Category extends BaseEntity {

    @Column(nullable = false, unique = true, length = 100)
    private String name;

    @Column(columnDefinition = "TEXT")
    private String description;
}
