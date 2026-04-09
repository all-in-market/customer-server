package com.example.allinmarket.domain.product.entity;

import com.example.allinmarket.domain.category.entity.Category;
import com.example.allinmarket.domain.product.enums.ProductStatus;
import com.example.allinmarket.seller.entity.Seller;
import jakarta.persistence.*;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Getter
@Entity
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "products")
public class Product {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @NotNull
    private Long id;

    @NotNull
    private Seller seller;

    @NotNull
    private Category category;

    @NotBlank
    @Min(value = 0)
    @Max(value = 200)
    private String name;

    @NotNull
    private BigDecimal price;

    @NotNull
    private int stock;

    @NotNull
    private ProductStatus status;

    private String description;

    @Builder
    public Product(Seller seller, Category category, String name, BigDecimal price, int stock, ProductStatus status, String description) {
        this.seller = seller;
        this.category = category;
        this.name = name;
        this.price = price;
        this.stock = stock;
        this.status = status;
        this.description = description;
    }

}
