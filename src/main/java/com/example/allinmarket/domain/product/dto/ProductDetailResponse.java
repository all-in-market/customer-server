package com.example.allinmarket.domain.product.dto;

import com.example.allinmarket.domain.product.entity.Product;
import com.example.allinmarket.domain.product.enums.ProductStatus;

import java.math.BigDecimal;

public record ProductDetailResponse (
        Long id,
        Long sellerId,
        Long categoryId,
        String name,
        BigDecimal price,
        int stock,
        ProductStatus status,
        String description
) {
    public static ProductDetailResponse from(Product product) {
        return new ProductDetailResponse(
                product.getId(),
                product.getSeller().getId(),
                product.getCategory().getId(),
                product.getName(),
                product.getPrice(),
                product.getStock(),
                product.getStatus(),
                product.getDescription()
        );
    }
}