package com.example.allinmarket.seller.product.dto.request;

import jakarta.validation.constraints.*;

import java.math.BigDecimal;

public record SellerProductCreateRequest(
        @NotNull(message = "카테고리는 필수입니다")
        Long categoryId,

        @NotBlank(message = "상품명은 필수입니다")
        @Size(max = 200, message = "상품명은 200자 이하여야 합니다")
        String name,

        @NotNull(message = "가격은 필수입니다")
        @PositiveOrZero(message = "가격은 0 이상이어야 합니다")
        @Digits(integer = 10, fraction = 2, message = "가격은 정수 10자리, 소수 2자리 이하여야 합니다")
        BigDecimal price,

        @PositiveOrZero(message = "가격은 0 이상이어야 합니다")
        int stock,

        @NotBlank(message = "상품 설명은 필수입니다")
        @Size(max = 255, message = "상품 설명은 255자 이하여야 합니다")
        String description
) {
}
