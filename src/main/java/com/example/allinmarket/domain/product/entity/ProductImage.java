package com.example.allinmarket.domain.product.entity;

import com.example.allinmarket.common.entity.CreatableEntity;
import jakarta.persistence.*;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "product_image")
public class ProductImage extends CreatableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    @Column(nullable = false)
    private String imageUrl;

    @PositiveOrZero
    @Column(name = "sort_order")
    private Integer sortOrder;

    @Column(nullable = false)
    private boolean representative;

    public static ProductImage of(Product product, String imageUrl, Integer sortOrder, boolean representative) {
        ProductImage productImage = new ProductImage();
        productImage.product = product;
        productImage.imageUrl = imageUrl;
        productImage.sortOrder = sortOrder;
        productImage.representative = representative;
        return productImage;
    }
}
