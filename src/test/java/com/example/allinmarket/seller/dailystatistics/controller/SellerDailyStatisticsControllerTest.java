package com.example.allinmarket.seller.dailystatistics.controller;

import com.example.allinmarket.common.enums.ErrorEnum;
import com.example.allinmarket.common.exception.BaseException;
import com.example.allinmarket.common.security.JwtProvider;
import com.example.allinmarket.domain.sellerdailystatistics.dto.response.DailyStatisticsResponse;
import com.example.allinmarket.seller.dailystatistics.service.SellerDailyStatisticsService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.resttestclient.autoconfigure.AutoConfigureRestTestClient;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.client.RestTestClient;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@WebMvcTest(SellerDailyStatisticsController.class)
@AutoConfigureRestTestClient
class SellerDailyStatisticsControllerTest {

    @Autowired
    private RestTestClient restTestClient;

    @MockitoBean
    private JwtProvider jwtProvider;

    @MockitoBean
    private SellerDailyStatisticsService sellerDailyStatisticsService;

    private void setAuth() {
        UsernamePasswordAuthenticationToken auth =
                new UsernamePasswordAuthenticationToken(1L, null, List.of(new SimpleGrantedAuthority("SELLER")));
        SecurityContextHolder.getContext().setAuthentication(auth);
    }

    @Test
    void 특정일_통계_조회_성공_테스트() {
        // given
        setAuth();
        LocalDate date = LocalDate.of(2025, 4, 10);

        DailyStatisticsResponse response = new DailyStatisticsResponse(
                1L,
                date,
                date,
                5,
                10,
                BigDecimal.valueOf(150000),
                1,
                BigDecimal.valueOf(30000),
                BigDecimal.valueOf(120000)
        );

        when(sellerDailyStatisticsService.getDailyStatistics(any(Long.class), eq(date)))
                .thenReturn(response);

        // when & then
        restTestClient.get().uri("/seller/statistics/daily/2025-04-10")
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.success").isEqualTo(true)
                .jsonPath("$.status").isEqualTo(200)
                .jsonPath("$.data.statDate").isEqualTo("2025-04-10")
                .jsonPath("$.data.totalOrders").isEqualTo(5)
                .jsonPath("$.data.totalItems").isEqualTo(10)
                .jsonPath("$.data.totalRefunds").isEqualTo(1)
                .jsonPath("$.data.totalSales").isEqualTo(150000)
                .jsonPath("$.data.refundAmount").isEqualTo(30000)
                .jsonPath("$.data.netSales").isEqualTo(120000);
    }

    @Test
    void 특정일_통계_조회_판매없는날_영값_성공_테스트() {
        // given
        setAuth();
        LocalDate date = LocalDate.of(2025, 4, 10);

        DailyStatisticsResponse response = new DailyStatisticsResponse(
                1L,
                date,
                date,
                0,
                0,
                BigDecimal.ZERO,
                0,
                BigDecimal.ZERO,
                BigDecimal.ZERO
        );

        when(sellerDailyStatisticsService.getDailyStatistics(any(Long.class), eq(date)))
                .thenReturn(response);

        // when & then
        restTestClient.get().uri("/seller/statistics/daily/2025-04-10")
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.success").isEqualTo(true)
                .jsonPath("$.data.totalOrders").isEqualTo(0)
                .jsonPath("$.data.totalSales").isEqualTo(0)
                .jsonPath("$.data.netSales").isEqualTo(0);
    }

    @Test
    void 특정일_통계_조회_데이터없음_예외_테스트() {
        // given
        setAuth();

        when(sellerDailyStatisticsService.getDailyStatistics(any(Long.class), any(LocalDate.class)))
                .thenThrow(new BaseException(ErrorEnum.STATISTICS_NOT_FOUND));

        // when & then
        restTestClient.get().uri("/seller/statistics/daily/2025-04-10")
                .exchange()
                .expectStatus().isNotFound()
                .expectBody()
                .jsonPath("$.success").isEqualTo(false);
    }

    @Test
    void 특정일_통계_조회_잘못된날짜형식_500_테스트() {
        // given
        setAuth();

        // when & then
        restTestClient.get().uri("/seller/statistics/daily/20250410")
                .exchange()
                .expectStatus().isEqualTo(500)
                .expectBody()
                .jsonPath("$.success").isEqualTo(false);
    }
}