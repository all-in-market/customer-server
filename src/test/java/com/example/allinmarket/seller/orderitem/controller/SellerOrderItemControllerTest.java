package com.example.allinmarket.seller.orderitem.controller;

import com.example.allinmarket.common.response.PageResponse;
import com.example.allinmarket.common.security.JwtProvider;
import com.example.allinmarket.domain.orderitem.dto.OrderItemDetailResponse;
import com.example.allinmarket.seller.orderitem.service.SellerOrderItemService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.resttestclient.autoconfigure.AutoConfigureRestTestClient;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.client.RestTestClient;

import java.math.BigDecimal;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@WebMvcTest(SellerOrderItemController.class)
@AutoConfigureRestTestClient
public class SellerOrderItemControllerTest {

    @Autowired
    private RestTestClient restTestClient;

    @MockitoBean
    private JwtProvider jwtProvider;

    @MockitoBean
    private SellerOrderItemService sellerOrderItemService;

    private void setAuth() {
        UsernamePasswordAuthenticationToken auth =
                new UsernamePasswordAuthenticationToken(1L, null, List.of(new SimpleGrantedAuthority("SELLER")));
        SecurityContextHolder.getContext().setAuthentication(auth);
    }

    @Test
    void 판매자_주문상품목록_조회_성공_테스트() {
        // given
        setAuth();

        OrderItemDetailResponse item = new OrderItemDetailResponse(
                1L, 10L, 100L, 1L, "신발", BigDecimal.valueOf(30000), 1
        );

        PageResponse<OrderItemDetailResponse> pageResponse = new PageResponse<>(
                List.of(item), 1, 1, 1L, 10, true
        );

        when(sellerOrderItemService.findAll(any(Long.class), any(Pageable.class)))
                .thenReturn(pageResponse);

        // when & then
        restTestClient.get().uri("/seller/orderitems")
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.success").isEqualTo(true)
                .jsonPath("$.status").isEqualTo(200)
                .jsonPath("$.data.content[0].id").isEqualTo(1)
                .jsonPath("$.data.content[0].orderId").isEqualTo(10)
                .jsonPath("$.data.content[0].productName").isEqualTo("신발")
                .jsonPath("$.data.content[0].unitPrice").isEqualTo(30000)
                .jsonPath("$.data.totalElements").isEqualTo(1)
                .jsonPath("$.data.currentPage").isEqualTo(1);
    }

    @Test
    void 판매자_주문상품목록_조회_빈목록_성공_테스트() {
        // given
        setAuth();

        PageResponse<OrderItemDetailResponse> emptyPage = new PageResponse<>(
                List.of(), 1, 0, 0L, 10, true
        );

        when(sellerOrderItemService.findAll(any(Long.class), any(Pageable.class)))
                .thenReturn(emptyPage);

        // when & then
        restTestClient.get().uri("/seller/orderitems")
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.success").isEqualTo(true)
                .jsonPath("$.data.content").isEmpty()
                .jsonPath("$.data.totalElements").isEqualTo(0);
    }

    @Test
    void 판매자_주문상품목록_조회_페이징_파라미터_성공_테스트() {
        // given
        setAuth();

        PageResponse<OrderItemDetailResponse> pageResponse = new PageResponse<>(
                List.of(), 2, 5, 42L, 10, false
        );

        when(sellerOrderItemService.findAll(any(Long.class), any(Pageable.class)))
                .thenReturn(pageResponse);

        // when & then
        restTestClient.get().uri("/seller/orderitems?page=1&size=10")
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.data.currentPage").isEqualTo(2)
                .jsonPath("$.data.totalPages").isEqualTo(5)
                .jsonPath("$.data.totalElements").isEqualTo(42)
                .jsonPath("$.data.isLast").isEqualTo(false);
    }
}