package com.example.allinmarket.buyer.cart.controller;

import com.example.allinmarket.buyer.cart.dto.request.AddProductToCartRequest;
import com.example.allinmarket.buyer.cart.dto.request.UpdateCartItemQuantityRequest;
import com.example.allinmarket.buyer.cart.dto.response.CartDetailResponse;
import com.example.allinmarket.buyer.cart.service.BuyerCartService;
import com.example.allinmarket.buyer.cartitem.dto.CartItemDetailResponse;
import com.example.allinmarket.common.enums.ErrorEnum;
import com.example.allinmarket.common.enums.SuccessEnum;
import com.example.allinmarket.common.exception.BaseException;
import com.example.allinmarket.common.response.PageResponse;
import com.example.allinmarket.common.security.JwtProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.resttestclient.autoconfigure.AutoConfigureRestTestClient;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.client.RestTestClient;

import java.math.BigDecimal;
import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;

@WebMvcTest(BuyerCartController.class)
@AutoConfigureRestTestClient
public class BuyerCartControllerTest {
    @Autowired
    private RestTestClient restTestClient;

    @MockitoBean
    private JwtProvider jwtProvider;

    @MockitoBean
    private BuyerCartService buyerCartService;

    @BeforeEach
    void setUp() {
        Authentication auth = new UsernamePasswordAuthenticationToken(1L, null, List.of());
        SecurityContextHolder.getContext().setAuthentication(auth);
    }

    @Test
    @WithMockUser
    void 장바구니_조회_성공_테스트() {
        // given
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
    @WithMockUser
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

    @Test
    @WithMockUser
    void 장바구니_상품_추가_성공_테스트() {
        // given
        AddProductToCartRequest request = new AddProductToCartRequest(1L, 2);

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

        given(buyerCartService.addProductToCart(eq(1L), eq(request), any(Pageable.class))).willReturn(response);

        // when & then
        restTestClient.post().uri("/carts/items")
                .contentType(MediaType.APPLICATION_JSON)
                .body(request)
                .exchange()
                .expectStatus().isCreated()
                .expectBody()
                .jsonPath("$.success").isEqualTo(true)
                .jsonPath("$.status").isEqualTo(201)
                .jsonPath("$.message").isEqualTo(SuccessEnum.CREATE_SUCCESS.getMessage())
                .jsonPath("$.data.buyerId").isEqualTo(1)
                .jsonPath("$.data.items.content[0].cartId").isEqualTo(1)
                .jsonPath("$.data.items.content[0].quantity").isEqualTo(1);
    }

    @Test
    @WithMockUser
    void 장바구니_상품_추가_실패_테스트() {
        // given
        AddProductToCartRequest request = new AddProductToCartRequest(1L, 5);

        given(buyerCartService.addProductToCart(eq(1L), eq(request), any(Pageable.class)))
                .willThrow(new BaseException(ErrorEnum.PRODUCT_OUT_OF_STOCK));

        // when & then
        restTestClient.post()
                .uri("/carts/items")
                .contentType(MediaType.APPLICATION_JSON)
                .body(request)
                .exchange()
                .expectStatus().isEqualTo(ErrorEnum.PRODUCT_OUT_OF_STOCK.getStatus())
                .expectBody()
                .jsonPath("$.success").isEqualTo(false)
                .jsonPath("$.status").isEqualTo(ErrorEnum.PRODUCT_OUT_OF_STOCK.getStatus())
                .jsonPath("$.message").isEqualTo(ErrorEnum.PRODUCT_OUT_OF_STOCK.getMessage());
    }

    @Test
    @WithMockUser
    void 장바구니_상품_수량_변경_성공_테스트() {
        // given
        UpdateCartItemQuantityRequest request = new UpdateCartItemQuantityRequest(5);

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
                                        5
                                )
                        ),
                        1, 1, 1, 10, true
                )
        );

        given(buyerCartService.updateCartItemQuantity(
                eq(1L),
                eq(1L),
                eq(request),
                any(Pageable.class)
                )
        ).willReturn(response);

        // when & then
        restTestClient.put().uri("/carts/items/{productId}", 1L)
                .contentType(MediaType.APPLICATION_JSON)
                .body(request)
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.success").isEqualTo(true)
                .jsonPath("$.status").isEqualTo(200)
                .jsonPath("$.message").isEqualTo(SuccessEnum.UPDATE_SUCCESS.getMessage())
                .jsonPath("$.data.buyerId").isEqualTo(1)
                .jsonPath("$.data.items.content[0].cartId").isEqualTo(1)
                .jsonPath("$.data.items.content[0].quantity").isEqualTo(5);
    }

    @Test
    @WithMockUser
    void 장바구니_수량_변경_실패_테스트() {
        // given
        UpdateCartItemQuantityRequest request = new UpdateCartItemQuantityRequest(5);

        given(buyerCartService.updateCartItemQuantity(eq(1L), eq(1L), eq(request), any(Pageable.class)))
                .willThrow(new BaseException(ErrorEnum.PRODUCT_OUT_OF_STOCK));

        // when & then
        restTestClient.put()
                .uri("/carts/items/{productId}", 1L)
                .contentType(MediaType.APPLICATION_JSON)
                .body(request)
                .exchange()
                .expectStatus().isEqualTo(ErrorEnum.PRODUCT_OUT_OF_STOCK.getStatus())
                .expectBody()
                .jsonPath("$.success").isEqualTo(false)
                .jsonPath("$.status").isEqualTo(ErrorEnum.PRODUCT_OUT_OF_STOCK.getStatus())
                .jsonPath("$.message").isEqualTo(ErrorEnum.PRODUCT_OUT_OF_STOCK.getMessage());
    }

    @Test
    @WithMockUser
    void 장바구니_상품_삭제_성공_테스트() {
        // given
        CartDetailResponse response = new CartDetailResponse(
                1L,
                1L,
                new PageResponse<>(List.of(), 1, 1, 1, 10, true));

        given(buyerCartService.removeCartItem(anyLong(), eq(1L), any(Pageable.class)))
                .willReturn(response);

        // when & then
        restTestClient.delete().uri("/carts/items/{productId}", 1L)
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.success").isEqualTo(true)
                .jsonPath("$.status").isEqualTo(200)
                .jsonPath("$.message").isEqualTo(SuccessEnum.DELETE_SUCCESS.getMessage())
                .jsonPath("$.data.buyerId").isEqualTo(1)
                .jsonPath("$.data.items.content").isEmpty();
    }

    @Test
    @WithMockUser
    void 장바구니_상품_삭제_실패_테스트() {
        // given
        given(buyerCartService.removeCartItem(anyLong(), eq(1L), any(Pageable.class)))
                .willThrow(new BaseException(ErrorEnum.PRODUCT_NOT_FOUND));

        // when & then
        restTestClient.delete()
                .uri("/carts/items/{productId}", 1L)
                .exchange()
                .expectStatus().isNotFound()
                .expectBody()
                .jsonPath("$.success").isEqualTo(false)
                .jsonPath("$.status").isEqualTo(ErrorEnum.PRODUCT_NOT_FOUND.getStatus())
                .jsonPath("$.message").isEqualTo(ErrorEnum.PRODUCT_NOT_FOUND.getMessage());
    }
}
