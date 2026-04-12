package com.example.allinmarket.buyer.cartitem.dto;

import com.example.allinmarket.domain.cartitem.entity.CartItem;

import java.math.BigDecimal;

public record CartItemDetailResponse(
        Long id,
        Long cartId,
        Long productId,
        String productName,
        BigDecimal productPrice,
        int quantity
) {

    public static CartItemDetailResponse from(CartItem cartItem) {
        return new CartItemDetailResponse(
                cartItem.getId(),
                cartItem.getCart().getId(),
                cartItem.getProduct().getId(),
                cartItem.getProduct().getName(),
                cartItem.getProduct().getPrice(),
                cartItem.getQuantity()
        );
    }
}
