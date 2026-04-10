package com.example.allinmarket.seller.product.dto.request;

import com.example.allinmarket.domain.category.entity.Category;
import com.example.allinmarket.domain.product.enums.ProductStatus;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.validation.constraints.*;

import java.math.BigDecimal;

public record SellerProductCreateRequest(
        @NotNull
        Category category,

        @NotBlank
        @Size(max = 200)
        String name,

        @NotNull
        @PositiveOrZero
        @Digits(integer = 10, fraction = 2)
        BigDecimal price,

        @PositiveOrZero
        int stock,

        @NotNull
        @Enumerated(EnumType.STRING)
        @Size(max = 20)
        ProductStatus status

) {
}
