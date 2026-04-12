package com.example.allinmarket.buyer.cart.dto.request;

public record AddProductToCartRequest(
        Long productId,
        int quantity
) {
}
