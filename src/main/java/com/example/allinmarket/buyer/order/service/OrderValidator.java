package com.example.allinmarket.buyer.order.service;

import com.example.allinmarket.common.enums.ErrorEnum;
import com.example.allinmarket.common.exception.BaseException;
import com.example.allinmarket.domain.cartitem.entity.CartItem;
import com.example.allinmarket.domain.product.entity.Product;
import com.example.allinmarket.domain.product.enums.ProductStatus;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Service
public class OrderValidator {

    /**
     * 상품이 구매 가능한 상태인지 검증
     */
    public void validateProductSellable(Map<Long, Product> productMap, List<CartItem> cartItems) {

        for (CartItem cartItem : cartItems) {
            Product product = productMap.get(cartItem.getProduct().getId());

            if(product.getStatus() != ProductStatus.ON_SALE ) {
                throw new BaseException(ErrorEnum.PRODUCT_NOT_AVAILABLE);
            } else if (product.getStock() < cartItem.getQuantity()) {
                throw new BaseException(ErrorEnum.PRODUCT_OUT_OF_STOCK);
            }
        }
    }

    /**
     * cartItem이 비어있지 않은지 검증
     */
    public void validateCartItemsNotEmpty(List<CartItem> cartItems) {
        if (cartItems == null || cartItems.isEmpty()) {
            throw new BaseException(ErrorEnum.CART_ITEMS_EMPTY);
        }
    }

    /**
     * 전달된 cartItemId 값들이 구매자 카트에 등록된 cartItem id가 맞는지 검증
     */
    public void validateCartItemsOwnedByBuyer(List<CartItem> cartItems, Long buyerId) {

        for (CartItem cartItem : cartItems) {
            Long ownerId = cartItem.getCart().getBuyer().getId();

            if(!ownerId.equals(buyerId)){
                throw new BaseException(ErrorEnum.INVALID_CART_ITEM_OWNER);
            }
        }
    }
}
