package com.example.allinmarket.buyer.cart.service;

import com.example.allinmarket.buyer.cart.dto.request.AddProductToCartRequest;
import com.example.allinmarket.buyer.cart.dto.request.UpdateCartItemQuantityRequest;
import com.example.allinmarket.buyer.cart.dto.response.CartDetailResponse;
import com.example.allinmarket.buyer.cartitem.dto.CartItemDetailResponse;
import com.example.allinmarket.common.enums.ErrorEnum;
import com.example.allinmarket.common.exception.BaseException;
import com.example.allinmarket.domain.cart.entity.Cart;
import com.example.allinmarket.domain.cart.repository.CartRepository;
import com.example.allinmarket.domain.cartitem.entity.CartItem;
import com.example.allinmarket.domain.cartitem.repository.CartItemRepository;
import com.example.allinmarket.domain.product.entity.Product;
import com.example.allinmarket.domain.product.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class BuyerCartService {
    private final CartRepository cartRepository;
    private final CartItemRepository cartItemRepository;
    private final ProductRepository productRepository;

    // 장바구니 조회
    public CartDetailResponse getCart(Long currentUserId, Pageable pageable) {
        Cart cart = findCartOrThrow(currentUserId);

        Page<CartItem> items = cartItemRepository.findByCartId(cart.getId(), pageable);

        Page<CartItemDetailResponse> responsePage = items.map(CartItemDetailResponse::from);

        return CartDetailResponse.from(cart, responsePage);
    }

    // 장바구니 상품 추가
    @Transactional
    public CartDetailResponse addProductToCart(Long currentUserId, AddProductToCartRequest request, Pageable pageable) {
        Cart cart = findCartOrThrow(currentUserId);

        Product product = findProductOrThrow(request.productId());

        if (product.getStock() <= 0 || product.getStock() < request.quantity()) {
            throw new BaseException(ErrorEnum.PRODUCT_OUT_OF_STOCK);
        }

        // 장바구니에 동일한 상품이 담겨 있는지 확인
        Optional<CartItem> existingCartItem = cartItemRepository.findByCartIdAndProductId(cart.getId(), request.productId());

        // 동일한 상품이 담겨 있으면 해당 수량만큼 증가
        if (existingCartItem.isPresent()) {

            CartItem existingItem = existingCartItem.get();

            // 추가된 수량이 상품 재고보다 많은지 체크
            int newQuantity = existingItem.getQuantity() + request.quantity();

            if (newQuantity > product.getStock()) {
                throw new BaseException(ErrorEnum.PRODUCT_OUT_OF_STOCK);
            }

            existingItem.increaseQuantity(request.quantity());

        } else { // 없으면 해당 수량 만큼 생성(default 수량이 1이라 1보다 클 경우 -1로 처리)

            CartItem newItem = CartItem.of(cart, product);

            if (request.quantity() > 1) {

                newItem.increaseQuantity(request.quantity() - 1);
            }

            cartItemRepository.save(newItem);
        }

        Page<CartItem> items = cartItemRepository.findByCartId(cart.getId(), pageable);

        Page<CartItemDetailResponse> responsePage = items.map(CartItemDetailResponse::from);

        return CartDetailResponse.from(cart, responsePage);
    }

    // 장바구니 수량 변경
    @Transactional
    public CartDetailResponse updateCartItemQuantity(Long currentUserId, Long productId, UpdateCartItemQuantityRequest request, Pageable pageable) {
        Cart cart = findCartOrThrow(currentUserId);

        Product product = findProductOrThrow(productId);

        CartItem cartItem = findCartItemOrThrow(cart.getId(), productId);

        if (request.quantity() > product.getStock()) {
            throw new BaseException(ErrorEnum.PRODUCT_OUT_OF_STOCK);
        }

        cartItem.updateQuantity(request.quantity());

        Page<CartItem> items = cartItemRepository.findByCartId(cart.getId(), pageable);

        Page<CartItemDetailResponse> cartItemDetailResponsePage = items.map(CartItemDetailResponse::from);

        return CartDetailResponse.from(cart, cartItemDetailResponsePage);
    }

    // 장바구니 상품 삭제
    @Transactional
    public CartDetailResponse removeCartItem(Long currentUserId, Long productId, Pageable pageable) {
        Cart cart = findCartOrThrow(currentUserId);

        Product product = findProductOrThrow(productId);

        CartItem cartItem = findCartItemOrThrow(cart.getId(), productId);

        cartItemRepository.delete(cartItem);

        Page<CartItem> items = cartItemRepository.findByCartId(cart.getId(), pageable);

        Page<CartItemDetailResponse> cartItemDetailResponsePage = items.map(CartItemDetailResponse::from);

        return CartDetailResponse.from(cart, cartItemDetailResponsePage);
    }

    private Cart findCartOrThrow(Long buyerId) {
        return cartRepository.findByBuyerId(buyerId).orElseThrow(
                () -> new BaseException(ErrorEnum.CART_NOT_FOUND)
        );
    }

    private Product findProductOrThrow(Long productId) {
        return productRepository.findById(productId).orElseThrow(
                () -> new BaseException(ErrorEnum.PRODUCT_NOT_FOUND)
        );
    }

    private CartItem findCartItemOrThrow(Long cartId, Long productId) {
        return cartItemRepository.findByCartIdAndProductId(cartId, productId).orElseThrow(
                () -> new BaseException(ErrorEnum.CART_ITEMS_NOT_FOUND)
        );
    }
}
