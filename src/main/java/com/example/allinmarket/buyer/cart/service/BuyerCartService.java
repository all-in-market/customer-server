package com.example.allinmarket.buyer.cart.service;

import com.example.allinmarket.buyer.cart.dto.CartDetailResponse;
import com.example.allinmarket.buyer.cartitem.dto.CartItemDetailResponse;
import com.example.allinmarket.common.enums.ErrorEnum;
import com.example.allinmarket.common.exception.BaseException;
import com.example.allinmarket.domain.cart.entity.Cart;
import com.example.allinmarket.domain.cart.repository.CartRepository;
import com.example.allinmarket.domain.cartitem.entity.CartItem;
import com.example.allinmarket.domain.cartitem.repository.CartItemRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class BuyerCartService {
    private final CartRepository cartRepository;
    private final CartItemRepository cartItemRepository;

    public CartDetailResponse getCart(Long currentUserId, Pageable pageable) {
        Cart cart = cartRepository.findByBuyerId(currentUserId).orElseThrow(
                () -> new BaseException(ErrorEnum.CART_NOT_FOUND)
        );

        Page<CartItem> items = cartItemRepository.findByCartId(cart.getId(), pageable);

        Page<CartItemDetailResponse> responsePage = items.map(CartItemDetailResponse::from);

        return CartDetailResponse.from(cart, responsePage);
    }
}
