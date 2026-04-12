package com.example.allinmarket.buyer.cart.controller;

import com.example.allinmarket.buyer.cart.dto.response.CartDetailResponse;
import com.example.allinmarket.buyer.cart.service.BuyerCartService;
import com.example.allinmarket.buyer.cartitem.dto.CartItemDetailResponse;
import com.example.allinmarket.common.enums.ErrorEnum;
import com.example.allinmarket.common.enums.SuccessEnum;
import com.example.allinmarket.common.exception.BaseException;
import com.example.allinmarket.common.response.PageResponse;
import com.example.allinmarket.common.security.JwtProvider;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.resttestclient.autoconfigure.AutoConfigureRestTestClient;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.client.RestTestClient;

import java.math.BigDecimal;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willThrow;

@WebMvcTest(BuyerCartController.class)
@AutoConfigureRestTestClient
public class BuyerCartControllerTest {
    @Autowired
    private RestTestClient restTestClient;

    @MockitoBean
    private JwtProvider jwtProvider;

    @MockitoBean
    private BuyerCartService buyerCartService;

    @Test
    @WithMockUser
    void 장바구니_조회_성공_테스트() {
        // given
        Authentication auth = new UsernamePasswordAuthenticationToken(1L, null, List.of());
        SecurityContextHolder.getContext().setAuthentication(auth);

        CartDetailResponse response = new CartDetailResponse(
                1L,
                1L,
                new PageResponse<>(
                        List.of(new CartItemDetailResponse(
                                1L,
                                1L,
                                1L,
                                "노트북",
                                BigDecimal.valueOf(1200000),
                                1
                                )
                        ),
                        1, 1, 1, 10, true
                )
        );

        given(buyerCartService.getCart(eq(1L), any(Pageable.class))).willReturn(response);

        // when & then
        restTestClient.get().uri("/carts")
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.success").isEqualTo(true)
                .jsonPath("$.status").isEqualTo(200)
                .jsonPath("$.message").isEqualTo(SuccessEnum.READ_SUCCESS.getMessage())
                .jsonPath("$.data.buyerId").isEqualTo(1)
                .jsonPath("$.data.items.content[0].cartId").isEqualTo(1)
                .jsonPath("$.data.items.content[0].productName").isEqualTo("노트북")
                .jsonPath("$.data.items.content[0].productPrice").isEqualTo(1200000)
                .jsonPath("$.data.items.content[0].quantity").isEqualTo(1);
    }

    @Test
    void 장비구니_조회_실패_테스트() {
        // given
        given(buyerCartService.getCart(eq(1L), any(Pageable.class)))
                .willThrow(new BaseException(ErrorEnum.UNAUTHORIZED));

        // when & then
        restTestClient.get().uri("/carts")
                .exchange()
                .expectStatus().isUnauthorized()
                .expectBody()
                .jsonPath("$.success").isEqualTo(false)
                .jsonPath("$.status").isEqualTo(401)
                .jsonPath("$.message").isEqualTo(ErrorEnum.UNAUTHORIZED.getMessage())
                .jsonPath("$.data").isEmpty();
    }
}
