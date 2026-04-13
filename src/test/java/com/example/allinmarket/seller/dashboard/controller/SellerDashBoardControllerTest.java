package com.example.allinmarket.seller.dashboard.controller;

import com.example.allinmarket.common.enums.ErrorEnum;
import com.example.allinmarket.common.exception.BaseException;
import com.example.allinmarket.common.security.JwtProvider;
import com.example.allinmarket.seller.dashboard.dto.response.SellerDashboardResponse;
import com.example.allinmarket.seller.dashboard.service.SellerDashboardService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.resttestclient.autoconfigure.AutoConfigureRestTestClient;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.client.RestTestClient;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.mockito.Mockito.when;

@WebMvcTest(SellerDashBoardController.class)
@AutoConfigureRestTestClient
public class SellerDashBoardControllerTest {

    @Autowired
    private RestTestClient restTestClient;

    @MockitoBean
    private JwtProvider jwtProvider;

    @MockitoBean
    private SellerDashboardService sellerDashboardService;

    private void setAuthContext(Long userId) {
        UsernamePasswordAuthenticationToken auth =
                new UsernamePasswordAuthenticationToken(userId, null, List.of());
        SecurityContextHolder.getContext().setAuthentication(auth);
    }

    @AfterEach
    void clearAuth() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void 판매자_대시보드_조회_성공_테스트() {
        // given
        setAuthContext(1L);

        LocalDate today = LocalDate.now();
        SellerDashboardResponse response = new SellerDashboardResponse(
                1L,
                today,
                10,
                BigDecimal.valueOf(500000),
                8,
                2,
                BigDecimal.valueOf(30000),
                BigDecimal.valueOf(455000),
                BigDecimal.valueOf(15000)
        );

        when(sellerDashboardService.getSellerDashboard(1L)).thenReturn(response);

        // when & then
        restTestClient.get().uri("/seller/dashboard")
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.success").isEqualTo(true)
                .jsonPath("$.status").isEqualTo(200)
                .jsonPath("$.message").isEqualTo("데이터 조회에 성공하였습니다.")
                .jsonPath("$.data.sellerId").isEqualTo(1)
                .jsonPath("$.data.statDate").isEqualTo(today.toString())
                .jsonPath("$.data.totalOrders").isEqualTo(10)
                .jsonPath("$.data.totalSales").isEqualTo(500000)
                .jsonPath("$.data.totalProductsSold").isEqualTo(8)
                .jsonPath("$.data.totalRefunds").isEqualTo(2)
                .jsonPath("$.data.refundAmount").isEqualTo(30000)
                .jsonPath("$.data.settlementAmount").isEqualTo(455000)
                .jsonPath("$.data.feeAmount").isEqualTo(15000);
    }

    @Test
    void 판매자_대시보드_조회_미인증_예외_테스트() {
        // given - SecurityContext 비어있는 상태 (인증 없음)
        SecurityContextHolder.clearContext();

        // when & then
        restTestClient.get().uri("/seller/dashboard")
                .exchange()
                .expectStatus().isUnauthorized()
                .expectBody()
                .jsonPath("$.success").isEqualTo(false)
                .jsonPath("$.status").isEqualTo(401);
    }

    @Test
    void 판매자_대시보드_조회_데이터없음_예외_테스트() {
        // given
        setAuthContext(999L);

        when(sellerDashboardService.getSellerDashboard(999L))
                .thenThrow(new BaseException(ErrorEnum.NOT_FOUND));

        // when & then
        restTestClient.get().uri("/seller/dashboard")
                .exchange()
                .expectStatus().isNotFound()
                .expectBody()
                .jsonPath("$.success").isEqualTo(false)
                .jsonPath("$.status").isEqualTo(404)
                .jsonPath("$.message").isEqualTo(ErrorEnum.NOT_FOUND.getMessage());
    }
}
