package com.example.allinmarket.domain.product.entity;

import com.example.allinmarket.common.entity.DeletableEntity;
import com.example.allinmarket.common.enums.ErrorEnum;
import com.example.allinmarket.common.exception.BaseException;
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

    public void decreaseStock(int amount) {
        if (amount > stock) {
            throw new BaseException(ErrorEnum.PRODUCT_OUT_OF_STOCK);
        }
        stock -= amount;
    }

    public void updateCategory(Category category) {
        this.category = category;
    }

    public void updateName(String name) {
        this.name = name;
    }

    public void updatePrice(BigDecimal price) {
        this.price = price;
    }

    public void updateStock(int stock) {
        this.stock = stock;
    }

    public void updateStatus(ProductStatus status) {
        this.status = status;
    }

    public void updateDescription(String description) {
        this.description = description;
    }
}
