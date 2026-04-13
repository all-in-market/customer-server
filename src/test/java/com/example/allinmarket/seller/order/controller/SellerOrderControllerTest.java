package com.example.allinmarket.seller.order.controller;

import com.example.allinmarket.buyer.order.dto.response.OrderDetailResponse;
import com.example.allinmarket.common.enums.ErrorEnum;
import com.example.allinmarket.common.exception.BaseException;
import com.example.allinmarket.common.response.PageResponse;
import com.example.allinmarket.common.security.JwtProvider;
import com.example.allinmarket.domain.order.enums.OrderStatus;
import com.example.allinmarket.domain.orderitem.dto.response.OrderItemDetailResponse;
import com.example.allinmarket.seller.order.dto.response.SellerOrderGetResponse;
import com.example.allinmarket.seller.order.service.SellerOrderService;
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

@WebMvcTest(SellerOrderController.class)
@AutoConfigureRestTestClient
public class SellerOrderControllerTest {

    @Autowired
    private RestTestClient restTestClient;

    @MockitoBean
    private JwtProvider jwtProvider;

    @MockitoBean
    private SellerOrderService sellerOrderService;

    private void setAuth() {
        UsernamePasswordAuthenticationToken auth =
                new UsernamePasswordAuthenticationToken(1L, null, List.of(new SimpleGrantedAuthority("SELLER")));
        SecurityContextHolder.getContext().setAuthentication(auth);
    }

    // ==================== 목록 조회 ====================

    @Test
    void 판매자_주문목록_조회_성공_테스트() {
        // given
        setAuth();

        OrderDetailResponse listResponse = new OrderDetailResponse(
                1L,
                2L,
                BigDecimal.valueOf(30000),
                OrderStatus.PAID,
                "TRACK123",
                "홍길동",
                "서울시 강남구"
        );

        PageResponse<OrderDetailResponse> pageResponse = new PageResponse<>(
                List.of(listResponse), 1, 1, 1L, 10, true
        );

        when(sellerOrderService.findAll(any(Long.class), any(Pageable.class)))
                .thenReturn(pageResponse);

        // when & then
        restTestClient.get().uri("/seller/orders")
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.success").isEqualTo(true)
                .jsonPath("$.status").isEqualTo(200)
                .jsonPath("$.data.content[0].orderId").isEqualTo(1)
                .jsonPath("$.data.content[0].status").isEqualTo("PAID")
                .jsonPath("$.data.content[0].recipient").isEqualTo("홍길동")
                .jsonPath("$.data.totalElements").isEqualTo(1)
                .jsonPath("$.data.currentPage").isEqualTo(1);
    }

    @Test
    void 판매자_주문목록_조회_빈목록_성공_테스트() {
        // given
        setAuth();

        PageResponse<OrderDetailResponse> emptyPage = new PageResponse<>(
                List.of(), 1, 0, 0L, 10, true
        );

        when(sellerOrderService.findAll(any(Long.class), any(Pageable.class)))
                .thenReturn(emptyPage);

        // when & then
        restTestClient.get().uri("/seller/orders")
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.success").isEqualTo(true)
                .jsonPath("$.data.content").isEmpty()
                .jsonPath("$.data.totalElements").isEqualTo(0);
    }

    @Test
    void 판매자_주문목록_조회_페이징_파라미터_성공_테스트() {
        // given
        setAuth();

        PageResponse<OrderDetailResponse> pageResponse = new PageResponse<>(
                List.of(), 2, 5, 42L, 10, false
        );

        when(sellerOrderService.findAll(any(Long.class), any(Pageable.class)))
                .thenReturn(pageResponse);

        // when & then
        restTestClient.get().uri("/seller/orders?page=1&size=10")
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.data.currentPage").isEqualTo(2)
                .jsonPath("$.data.totalPages").isEqualTo(5)
                .jsonPath("$.data.totalElements").isEqualTo(42)
                .jsonPath("$.data.isLast").isEqualTo(false);
    }

    // ==================== 상세 조회 ====================

    @Test
    void 판매자_주문상세_조회_성공_테스트() {
        // given
        setAuth();

        OrderItemDetailResponse item1 = new OrderItemDetailResponse(
                1L, 1L, 100L, 1L, "신발", BigDecimal.valueOf(30000), 1
        );
        OrderItemDetailResponse item2 = new OrderItemDetailResponse(
                2L, 1L, 101L, 1L, "양말", BigDecimal.valueOf(20000), 1
        );

        SellerOrderGetResponse detailResponse = new SellerOrderGetResponse(
                1L, 2L, BigDecimal.valueOf(50000),
                OrderStatus.PAID, "TRACK123",
                "홍길동", "010-1234-5678", "서울시 강남구",
                List.of(item1, item2)
        );

        when(sellerOrderService.findOne(any(Long.class), any(Long.class)))
                .thenReturn(detailResponse);

        // when & then
        restTestClient.get().uri("/seller/orders/1")
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.success").isEqualTo(true)
                .jsonPath("$.status").isEqualTo(200)
                .jsonPath("$.data.id").isEqualTo(1)
                .jsonPath("$.data.status").isEqualTo("PAID")
                .jsonPath("$.data.items").isArray()
                .jsonPath("$.data.items.length()").isEqualTo(2)
                .jsonPath("$.data.items[0].productName").isEqualTo("신발")
                .jsonPath("$.data.items[1].productName").isEqualTo("양말");
    }

    @Test
    void 판매자_주문상세_조회_주문없음_실패_테스트() {
        // given
        setAuth();

        when(sellerOrderService.findOne(any(Long.class), any(Long.class)))
                .thenThrow(new BaseException(ErrorEnum.ORDER_NOT_FOUND));

        // when & then
        restTestClient.get().uri("/seller/orders/999")
                .exchange()
                .expectStatus().is4xxClientError();
    }
}