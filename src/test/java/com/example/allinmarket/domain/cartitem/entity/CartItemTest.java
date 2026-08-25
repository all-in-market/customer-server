package com.example.allinmarket.domain.cartitem.entity;

import com.example.allinmarket.common.enums.ErrorEnum;
import com.example.allinmarket.common.exception.BaseException;
import com.example.allinmarket.domain.cart.entity.Cart;
import com.example.allinmarket.domain.product.entity.Product;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;

class CartItemTest {

    @Nested
    @DisplayName("수량 증가")
    class IncreaseQuantityTest {

        @Test
        @DisplayName("증가 수량이 0이면 예외가 발생하고 수량은 변하지 않는다")
        void increaseQuantity_whenAmountIsZero_thenThrowInvalidInput() {
            // given
            CartItem cartItem = createCartItem(3);

            // when & then
            assertThatThrownBy(() -> cartItem.increaseQuantity(0))
                    .isInstanceOf(BaseException.class)
                    .extracting("errorEnum")
                    .isEqualTo(ErrorEnum.INVALID_INPUT);

            assertThat(cartItem.getQuantity()).isEqualTo(3);
        }

        @Test
        @DisplayName("증가 수량이 음수이면 예외가 발생하고 수량은 변하지 않는다")
        void increaseQuantity_whenAmountIsNegative_thenThrowInvalidInput() {
            // given
            CartItem cartItem = createCartItem(3);

            // when & then
            assertThatThrownBy(() -> cartItem.increaseQuantity(-1))
                    .isInstanceOf(BaseException.class)
                    .extracting("errorEnum")
                    .isEqualTo(ErrorEnum.INVALID_INPUT);

            assertThat(cartItem.getQuantity()).isEqualTo(3);
        }

        @Test
        @DisplayName("증가 수량이 양수이면 수량이 증가한다")
        void increaseQuantity_whenAmountIsPositive_thenIncreaseQuantity() {
            // given
            CartItem cartItem = createCartItem(3);

            // when
            cartItem.increaseQuantity(2);

            // then
            assertThat(cartItem.getQuantity()).isEqualTo(5);
        }
    }

    @Nested
    @DisplayName("수량 감소")
    class DecreaseQuantityTest {

        @Test
        @DisplayName("감소 수량이 0이면 예외가 발생하고 수량은 변하지 않는다")
        void decreaseQuantity_whenAmountIsZero_thenThrowInvalidInput() {
            // given
            CartItem cartItem = createCartItem(3);

            // when & then
            assertThatThrownBy(() -> cartItem.decreaseQuantity(0))
                    .isInstanceOf(BaseException.class)
                    .extracting("errorEnum")
                    .isEqualTo(ErrorEnum.INVALID_INPUT);

            assertThat(cartItem.getQuantity()).isEqualTo(3);
        }

        @Test
        @DisplayName("감소 수량이 음수이면 예외가 발생하고 수량은 변하지 않는다")
        void decreaseQuantity_whenAmountIsNegative_thenThrowInvalidInput() {
            // given
            CartItem cartItem = createCartItem(3);

            // when & then
            assertThatThrownBy(() -> cartItem.decreaseQuantity(-1))
                    .isInstanceOf(BaseException.class)
                    .extracting("errorEnum")
                    .isEqualTo(ErrorEnum.INVALID_INPUT);

            assertThat(cartItem.getQuantity()).isEqualTo(3);
        }

        @Test
        @DisplayName("감소 수량이 현재 수량을 초과하면 예외가 발생하고 수량은 변하지 않는다")
        void decreaseQuantity_whenAmountExceedsCurrentQuantity_thenThrowInvalidInput() {
            // given
            CartItem cartItem = createCartItem(3);

            // when & then
            assertThatThrownBy(() -> cartItem.decreaseQuantity(4))
                    .isInstanceOf(BaseException.class)
                    .extracting("errorEnum")
                    .isEqualTo(ErrorEnum.INVALID_INPUT);

            assertThat(cartItem.getQuantity()).isEqualTo(3);
        }

        @Test
        @DisplayName("감소 수량이 현재 수량과 같으면(경계값) 수량이 0이 된다")
        void decreaseQuantity_whenAmountEqualsCurrentQuantity_thenQuantityBecomesZero() {
            // given
            CartItem cartItem = createCartItem(3);

            // when
            cartItem.decreaseQuantity(3);

            // then
            assertThat(cartItem.getQuantity()).isZero();
        }

        @Test
        @DisplayName("감소 수량이 현재 수량보다 작으면 수량이 줄어든다")
        void decreaseQuantity_whenAmountLessThanCurrentQuantity_thenDecreaseQuantity() {
            // given
            CartItem cartItem = createCartItem(3);

            // when
            cartItem.decreaseQuantity(1);

            // then
            assertThat(cartItem.getQuantity()).isEqualTo(2);
        }
    }

    @Nested
    @DisplayName("수량 변경")
    class UpdateQuantityTest {

        @Test
        @DisplayName("변경 수량이 0이면 예외가 발생하고 수량은 변하지 않는다")
        void updateQuantity_whenAmountIsZero_thenThrowInvalidInput() {
            // given
            CartItem cartItem = createCartItem(3);

            // when & then
            assertThatThrownBy(() -> cartItem.updateQuantity(0))
                    .isInstanceOf(BaseException.class)
                    .extracting("errorEnum")
                    .isEqualTo(ErrorEnum.INVALID_INPUT);

            assertThat(cartItem.getQuantity()).isEqualTo(3);
        }

        @Test
        @DisplayName("변경 수량이 음수이면 예외가 발생하고 수량은 변하지 않는다")
        void updateQuantity_whenAmountIsNegative_thenThrowInvalidInput() {
            // given
            CartItem cartItem = createCartItem(3);

            // when & then
            assertThatThrownBy(() -> cartItem.updateQuantity(-5))
                    .isInstanceOf(BaseException.class)
                    .extracting("errorEnum")
                    .isEqualTo(ErrorEnum.INVALID_INPUT);

            assertThat(cartItem.getQuantity()).isEqualTo(3);
        }

        @Test
        @DisplayName("변경 수량이 양수이면 지정한 수량으로 바뀐다")
        void updateQuantity_whenAmountIsPositive_thenSetToGivenQuantity() {
            // given
            CartItem cartItem = createCartItem(3);

            // when
            cartItem.updateQuantity(10);

            // then
            assertThat(cartItem.getQuantity()).isEqualTo(10);
        }
    }

    private CartItem createCartItem(int quantity) {
        Cart cart = mock(Cart.class);
        Product product = mock(Product.class);
        CartItem cartItem = CartItem.of(cart, product);
        ReflectionTestUtils.setField(cartItem, "quantity", quantity);
        return cartItem;
    }
}
