package com.example.allinmarket.domain.product.entity;

import com.example.allinmarket.common.entity.DeletableEntity;
import com.example.allinmarket.domain.category.entity.Category;
import com.example.allinmarket.domain.product.enums.ProductStatus;
import com.example.allinmarket.seller.entity.Seller;
import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Getter
@Entity
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "products")
public class Product extends DeletableEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "seller_id", nullable = false)
    private Seller seller;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id", nullable = false)
    private Category category;

    @NotBlank
    @Column(nullable = false, length = 200)
    private String name;

    @PositiveOrZero
    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal price;

    @PositiveOrZero
    private int stock;


    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ProductStatus status = ProductStatus.ON_SALE;

    @NotBlank
    @Column(nullable = false)
    private String description;

    public static Product of(Seller seller, Category category, String name, BigDecimal price, int stock, String description) {
        Product product = new Product();
        product.seller = seller;
        product.category = category;
        product.name = name;
        product.price = price != null ? price : BigDecimal.ZERO;
        product.stock = stock;
        product.status = ProductStatus.ON_SALE;
        product.description = description;
        return product;
    }
}
