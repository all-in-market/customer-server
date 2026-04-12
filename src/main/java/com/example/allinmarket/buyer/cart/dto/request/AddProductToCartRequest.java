package com.example.allinmarket.buyer.cart.dto.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record AddProductToCartRequest(
        @NotNull
        Long productId,

        @Positive
        int quantity
) {
}
