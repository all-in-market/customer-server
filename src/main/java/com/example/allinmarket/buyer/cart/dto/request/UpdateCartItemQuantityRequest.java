package com.example.allinmarket.buyer.cart.dto.request;

import jakarta.validation.constraints.Positive;

public record UpdateCartItemQuantityRequest(
        @Positive
        int quantity
) {
}
