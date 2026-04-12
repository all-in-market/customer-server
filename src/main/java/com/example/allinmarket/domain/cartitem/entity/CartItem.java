package com.example.allinmarket.domain.cartitem.entity;

import com.example.allinmarket.common.entity.ModifiableEntity;
import com.example.allinmarket.common.enums.ErrorEnum;
import com.example.allinmarket.common.exception.BaseException;
import com.example.allinmarket.domain.cart.entity.Cart;
import com.example.allinmarket.domain.product.entity.Product;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "cart_items")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Getter
public class CartItem extends ModifiableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cart_id", nullable = false)
    private Cart cart;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    @Column(nullable = false)
    private int quantity = 1;

    public static CartItem of(Cart cart, Product product) {
        CartItem cartItem = new CartItem();
        cartItem.cart = cart;
        cartItem.product = product;
        cartItem.quantity = 1;

        return cartItem;
    }

    public void increaseQuantity(int quantity) {
        if (quantity <= 0) {
            throw new BaseException(ErrorEnum.INVALID_INPUT);
        }
        this.quantity += quantity;
    }

    public void decreaseQuantity(int quantity) {
        if (quantity <= 0 || quantity > this.quantity) {
            throw new BaseException(ErrorEnum.INVALID_INPUT);
        }
        this.quantity -= quantity;
    }

    public void updateQuantity(int quantity) {
        if (quantity <= 0) {
            throw new BaseException(ErrorEnum.INVALID_INPUT);
        }
        this.quantity = quantity;
    }
}
