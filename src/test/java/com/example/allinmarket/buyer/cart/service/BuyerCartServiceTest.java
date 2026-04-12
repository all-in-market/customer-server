package com.example.allinmarket.buyer.cart.service;

import com.example.allinmarket.buyer.cart.dto.response.CartDetailResponse;
import com.example.allinmarket.buyer.entity.Buyer;
import com.example.allinmarket.common.exception.BaseException;
import com.example.allinmarket.domain.cart.entity.Cart;
import com.example.allinmarket.domain.cart.repository.CartRepository;
import com.example.allinmarket.domain.cartitem.entity.CartItem;
import com.example.allinmarket.domain.cartitem.repository.CartItemRepository;
import com.example.allinmarket.domain.product.entity.Product;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
public class BuyerCartServiceTest {
    @Mock
    private CartRepository cartRepository;

    @Mock
    private CartItemRepository cartItemRepository;

    @InjectMocks
    private BuyerCartService buyerCartService;

    @Test
    void 장바구니_조회_성공_테스트() {
        // given
        Buyer buyer = Buyer.of("test@test.com", "pw", "홍길동", "010-1234-5678");

        ReflectionTestUtils.setField(buyer, "id", 1L);

        Cart cart = Cart.of(buyer);

        ReflectionTestUtils.setField(cart, "id", 1L);

        CartItem item = CartItem.of(
                cart,
                Product.of(
                        null,
                        null,
                        "테스트",
                        BigDecimal.valueOf(15000),
                        10,
                        "설명"
                )
        );
        Page<CartItem> page = new PageImpl<>(List.of(item));

        given(cartRepository.findByBuyerId(1L)).willReturn(Optional.of(cart));
        given(cartItemRepository.findByCartId(eq(1L), any(Pageable.class))).willReturn(page);

        // when
        CartDetailResponse response = buyerCartService.getCart(1L, PageRequest.of(0, 10));

        // then
        assertEquals(1L, response.id());
        assertEquals(1L, response.buyerId());
        assertEquals(1, response.items().content().size());
        assertEquals("테스트", response.items().content().get(0).productName());
    }

    @Test
    void 장바구니_조회_실패_테스트() {
        // given
        given(cartRepository.findByBuyerId(99L)).willReturn(Optional.empty());

        // when & then
        assertThrows(BaseException.class,
                () -> buyerCartService.getCart(99L, PageRequest.of(0, 10)));
    }
}
