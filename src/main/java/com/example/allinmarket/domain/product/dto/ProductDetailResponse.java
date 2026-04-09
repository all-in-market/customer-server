package com.example.allinmarket.domain.product.dto;

import com.example.allinmarket.domain.category.entity.Category;
import com.example.allinmarket.domain.product.entity.Product;
import com.example.allinmarket.domain.product.enums.ProductStatus;
import com.example.allinmarket.seller.entity.Seller;

import java.math.BigDecimal;

public record ProductDetailResponse (
        Seller seller,
        Category category,
        String name,
        BigDecimal price,
        int stock,
        ProductStatus status,
        String description
) {
    public static ProductDetailResponse from(Product product) {
        return new ProductDetailResponse(
                product.getSeller(),
                product.getCategory(),
                product.getName(),
                product.getPrice(),
                product.getStock(),
                product.getStatus(),
                product.getDescription()
        );
    }
}