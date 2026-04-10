package com.example.allinmarket.domain.category.entity;

import com.example.allinmarket.common.entity.BaseEntity;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.validator.constraints.Length;

@Getter
@Entity
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "categories")
public class Category extends BaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank
    @Length(max = 50)
    @Column(unique = true)
    private String name;

    @PositiveOrZero
    private int sortOrder;

    public static Category of(String name, int sortOrder) {
        Category category = new Category();
        category.name = name;
        category.sortOrder = sortOrder;
        return category;
    }
}
