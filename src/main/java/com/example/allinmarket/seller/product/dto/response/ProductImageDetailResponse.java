package com.example.allinmarket.seller.product.dto.response;

import com.example.allinmarket.domain.product.entity.ProductImage;

public record ProductImageDetailResponse(
        Long imageId,
        String imageUrl,
        Integer sortOrder,
        boolean representative
) {
    public static ProductImageDetailResponse from (ProductImage productImage) {
        return new ProductImageDetailResponse(
                productImage.getId(),
                productImage.getImageUrl(),
                productImage.getSortOrder(),
                productImage.isRepresentative()
        );
    }
}