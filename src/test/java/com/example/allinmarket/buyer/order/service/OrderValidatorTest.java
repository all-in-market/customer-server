package com.example.allinmarket.buyer.order.service;

import com.example.allinmarket.buyer.entity.Buyer;
import com.example.allinmarket.common.enums.ErrorEnum;
import com.example.allinmarket.common.exception.BaseException;
import com.example.allinmarket.domain.cart.entity.Cart;
import com.example.allinmarket.domain.cartitem.entity.CartItem;
import com.example.allinmarket.domain.category.entity.Category;
import com.example.allinmarket.domain.product.entity.Product;
import com.example.allinmarket.domain.product.enums.ProductStatus;
import com.example.allinmarket.seller.entity.Seller;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.assertThatCode;

class OrderValidatorTest {

    private final OrderValidator orderValidator = new OrderValidator();

    @Nested
    @DisplayName("상품 판매 가능 여부 검증")
    class ValidateProductSellableTest {

        @Test
        @DisplayName("판매 중이 아닌 상품이 포함되면 예외를 던진다")
        void validateProductSellable_whenProductNotOnSale_thenThrowProductNotAvailable() {
            // given
            Product product = createProduct(1L, ProductStatus.HIDDEN, 10);
            CartItem cartItem = createCartItem(1L, product, 1);

            Map<Long, Product> productMap = new HashMap<>();
            productMap.put(product.getId(), product);

            // when & then
            assertThatThrownBy(() -> orderValidator.validateProductSellable(productMap, List.of(cartItem)))
                    .isInstanceOf(BaseException.class)
                    .extracting("errorEnum")
                    .isEqualTo(ErrorEnum.PRODUCT_NOT_AVAILABLE);
        }

        @Test
        @DisplayName("재고가 주문 수량보다 적으면 예외를 던진다")
        void validateProductSellable_whenStockLessThanQuantity_thenThrowProductOutOfStock() {
            // given
            Product product = createProduct(1L, ProductStatus.ON_SALE, 3);
            CartItem cartItem = createCartItem(1L, product, 5);

            Map<Long, Product> productMap = new HashMap<>();
            productMap.put(product.getId(), product);

            // when & then
            assertThatThrownBy(() -> orderValidator.validateProductSellable(productMap, List.of(cartItem)))
                    .isInstanceOf(BaseException.class)
                    .extracting("errorEnum")
                    .isEqualTo(ErrorEnum.PRODUCT_OUT_OF_STOCK);
        }

        @Test
        @DisplayName("재고와 주문 수량이 정확히 같으면 통과한다 (경계값)")
        void validateProductSellable_whenStockEqualsQuantity_thenPass() {
            // given
            Product product = createProduct(1L, ProductStatus.ON_SALE, 5);
            CartItem cartItem = createCartItem(1L, product, 5);

            Map<Long, Product> productMap = new HashMap<>();
            productMap.put(product.getId(), product);

            // when & then
            assertThatCode(() -> orderValidator.validateProductSellable(productMap, List.of(cartItem)))
                    .doesNotThrowAnyException();
        }

        @Test
        @DisplayName("판매 가능한 상품이며 재고가 충분하면 통과한다")
        void validateProductSellable_whenSellableAndEnoughStock_thenPass() {
            // given
            Product product = createProduct(1L, ProductStatus.ON_SALE, 10);
            CartItem cartItem = createCartItem(1L, product, 3);

            Map<Long, Product> productMap = new HashMap<>();
            productMap.put(product.getId(), product);

            // when & then
            assertThatCode(() -> orderValidator.validateProductSellable(productMap, List.of(cartItem)))
                    .doesNotThrowAnyException();
        }
    }

    @Nested
    @DisplayName("장바구니 상품 비어있는지 검증")
    class ValidateCartItemsNotEmptyTest {

        @Test
        @DisplayName("cartItems가 null이면 예외를 던진다")
        void validateCartItemsNotEmpty_whenNull_thenThrowCartItemsEmpty() {
            // when & then
            assertThatThrownBy(() -> orderValidator.validateCartItemsNotEmpty(null))
                    .isInstanceOf(BaseException.class)
                    .extracting("errorEnum")
                    .isEqualTo(ErrorEnum.CART_ITEMS_EMPTY);
        }

        @Test
        @DisplayName("cartItems가 빈 리스트면 예외를 던진다")
        void validateCartItemsNotEmpty_whenEmptyList_thenThrowCartItemsEmpty() {
            // when & then
            assertThatThrownBy(() -> orderValidator.validateCartItemsNotEmpty(Collections.emptyList()))
                    .isInstanceOf(BaseException.class)
                    .extracting("errorEnum")
                    .isEqualTo(ErrorEnum.CART_ITEMS_EMPTY);
        }

        @Test
        @DisplayName("cartItems가 비어있지 않으면 통과한다")
        void validateCartItemsNotEmpty_whenNotEmpty_thenPass() {
            // given
            Product product = createProduct(1L, ProductStatus.ON_SALE, 10);
            CartItem cartItem = createCartItem(1L, product, 1);

            // when & then
            assertThatCode(() -> orderValidator.validateCartItemsNotEmpty(List.of(cartItem)))
                    .doesNotThrowAnyException();
        }
    }

    @Nested
    @DisplayName("장바구니 상품 소유자 검증")
    class ValidateCartItemsOwnedByBuyerTest {

        @Test
        @DisplayName("장바구니 상품 소유자가 다르면 예외를 던진다")
        void validateCartItemsOwnedByBuyer_whenOwnerMismatch_thenThrowInvalidCartItemOwner() {
            // given
            Buyer owner = createBuyer(1L);
            Cart cart = Cart.of(owner);
            Product product = createProduct(1L, ProductStatus.ON_SALE, 10);
            CartItem cartItem = CartItem.of(cart, product);
            ReflectionTestUtils.setField(cartItem, "id", 1L);

            // when & then
            assertThatThrownBy(() -> orderValidator.validateCartItemsOwnedByBuyer(List.of(cartItem), 2L))
                    .isInstanceOf(BaseException.class)
                    .extracting("errorEnum")
                    .isEqualTo(ErrorEnum.INVALID_CART_ITEM_OWNER);
        }

        @Test
        @DisplayName("장바구니 상품 소유자가 일치하면 통과한다")
        void validateCartItemsOwnedByBuyer_whenOwnerMatches_thenPass() {
            // given
            Buyer owner = createBuyer(1L);
            Cart cart = Cart.of(owner);
            Product product = createProduct(1L, ProductStatus.ON_SALE, 10);
            CartItem cartItem = CartItem.of(cart, product);
            ReflectionTestUtils.setField(cartItem, "id", 1L);

            // when & then
            assertThatCode(() -> orderValidator.validateCartItemsOwnedByBuyer(List.of(cartItem), 1L))
                    .doesNotThrowAnyException();
        }
    }

    private Buyer createBuyer(Long id) {
        Buyer buyer = Buyer.of("test@test.com", "1234", "홍길동", "010-0000-0000");
        ReflectionTestUtils.setField(buyer, "id", id);
        return buyer;
    }

    private Product createProduct(Long id, ProductStatus status, int stock) {
        Seller seller = org.mockito.Mockito.mock(Seller.class);
        Category category = org.mockito.Mockito.mock(Category.class);
        Product product = Product.of(seller, category, "상품", BigDecimal.TEN, stock, "설명");
        ReflectionTestUtils.setField(product, "id", id);
        ReflectionTestUtils.setField(product, "status", status);
        return product;
    }

    private CartItem createCartItem(Long id, Product product, int quantity) {
        Buyer buyer = createBuyer(99L);
        Cart cart = Cart.of(buyer);
        CartItem cartItem = CartItem.of(cart, product);
        ReflectionTestUtils.setField(cartItem, "id", id);
        ReflectionTestUtils.setField(cartItem, "quantity", quantity);
        return cartItem;
    }
}
