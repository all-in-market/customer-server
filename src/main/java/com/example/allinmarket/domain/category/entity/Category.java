package com.example.allinmarket.domain.category.entity;

import com.example.allinmarket.common.entity.DeletableEntity;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "categories")
public class Category extends DeletableEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank
    @Column(nullable = false, length = 50, unique = true)
    private String name;

    @PositiveOrZero
    @Column(name = "sort_order")
    private int sortOrder;

    public static Category of(String name, int sortOrder) {
        Category category = new Category();
        category.name = name;
        category.sortOrder = sortOrder;
        return category;
    }
}
