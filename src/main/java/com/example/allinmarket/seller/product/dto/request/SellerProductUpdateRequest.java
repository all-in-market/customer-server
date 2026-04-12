package com.example.allinmarket.seller.product.dto.request;

import com.example.allinmarket.domain.product.enums.ProductStatus;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public record SellerProductUpdateRequest(
        Long categoryId,

        @Size(max = 200, message = "상품명은 200자 이하여야 합니다")
        String name,

        @PositiveOrZero(message = "가격은 0 이상이어야 합니다")
        @Digits(integer = 10, fraction = 2, message = "가격은 정수 10자리, 소수 2자리 이하여야 합니다")
        BigDecimal price,

        ProductStatus status,

        @Size(max = 255, message = "상품 설명은 255자 이하여야 합니다")
        String description
) {
}
