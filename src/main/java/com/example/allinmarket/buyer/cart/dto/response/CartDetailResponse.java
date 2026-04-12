package com.example.allinmarket.buyer.cart.dto.response;

import com.example.allinmarket.buyer.cartitem.dto.CartItemDetailResponse;
import com.example.allinmarket.common.response.PageResponse;
import com.example.allinmarket.domain.cart.entity.Cart;
import org.springframework.data.domain.Page;


public record CartDetailResponse(
        Long id,
        Long buyerId,
        PageResponse<CartItemDetailResponse> items
) {
    public static CartDetailResponse from(Cart cart, Page<CartItemDetailResponse> items) {
        return new CartDetailResponse(
                cart.getId(),
                cart.getBuyer().getId(),
                PageResponse.register(items)
        );
    }
}
