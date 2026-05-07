package com.example.allinmarket.seller.dailystatistics.controller;

import com.example.allinmarket.common.enums.ErrorEnum;
import com.example.allinmarket.common.exception.BaseException;
import com.example.allinmarket.seller.dailystatistics.dto.DailyStatisticsResponse;
import com.example.allinmarket.seller.dailystatistics.service.SellerDailyStatisticsService;
import com.example.allinmarket.support.RestDocsControllerTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.document;
import static org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.*;
import static org.springframework.restdocs.payload.PayloadDocumentation.*;
import static org.springframework.restdocs.request.RequestDocumentation.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(SellerDailyStatisticsController.class)
class SellerDailyStatisticsControllerTest extends RestDocsControllerTest {

    @MockitoBean
    private SellerDailyStatisticsService sellerDailyStatisticsService;

    @BeforeEach
    void setAuth() {
        UsernamePasswordAuthenticationToken auth =
                new UsernamePasswordAuthenticationToken(1L, null, List.of(new SimpleGrantedAuthority("SELLER")));
        SecurityContextHolder.getContext().setAuthentication(auth);
    }

    @Test
    void 특정일_통계_조회_성공_테스트() throws Exception {
        LocalDate date = LocalDate.of(2025, 4, 10);
        DailyStatisticsResponse response = new DailyStatisticsResponse(
                1L, date, date, 5, 10,
                BigDecimal.valueOf(150000), 1, BigDecimal.valueOf(30000), BigDecimal.valueOf(120000)
        );
        when(sellerDailyStatisticsService.getDailyStatistics(any(Long.class), eq(date)))
                .thenReturn(response);

        mockMvc.perform(get("/seller/statistics/daily/{date}", "2025-04-10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.data.from").value("2025-04-10"))
                .andExpect(jsonPath("$.data.to").value("2025-04-10"))
                .andExpect(jsonPath("$.data.totalOrders").value(5))
                .andExpect(jsonPath("$.data.totalItems").value(10))
                .andExpect(jsonPath("$.data.totalRefunds").value(1))
                .andExpect(jsonPath("$.data.totalSales").value(150000))
                .andExpect(jsonPath("$.data.refundAmount").value(30000))
                .andExpect(jsonPath("$.data.netSales").value(120000))
                .andDo(document("seller/statistics/daily",
                        pathParameters(
                                parameterWithName("date").description("조회할 날짜 (yyyy-MM-dd)")
                        ),
                        responseFields(
                                fieldWithPath("success").description("요청 성공 여부"),
                                fieldWithPath("status").description("HTTP 상태 코드"),
                                fieldWithPath("message").description("응답 메시지"),
                                fieldWithPath("data.sellerId").description("판매자 ID"),
                                fieldWithPath("data.from").description("통계 시작 날짜"),
                                fieldWithPath("data.to").description("통계 종료 날짜"),
                                fieldWithPath("data.totalOrders").description("총 주문 수"),
                                fieldWithPath("data.totalItems").description("총 판매 아이템 수"),
                                fieldWithPath("data.totalSales").description("총 매출액"),
                                fieldWithPath("data.totalRefunds").description("총 환불 건수"),
                                fieldWithPath("data.refundAmount").description("총 환불 금액"),
                                fieldWithPath("data.netSales").description("순 매출액 (매출 - 환불)"),
                                fieldWithPath("timestamp").description("응답 시각")
                        )
                ));
    }

    @Test
    void 특정일_통계_조회_판매없는날_영값_성공_테스트() throws Exception {
        LocalDate date = LocalDate.of(2025, 4, 10);
        DailyStatisticsResponse response = new DailyStatisticsResponse(
                1L, date, date, 0, 0,
                BigDecimal.ZERO, 0, BigDecimal.ZERO, BigDecimal.ZERO
        );
        when(sellerDailyStatisticsService.getDailyStatistics(any(Long.class), eq(date)))
                .thenReturn(response);

        mockMvc.perform(get("/seller/statistics/daily/2025-04-10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.totalOrders").value(0))
                .andExpect(jsonPath("$.data.totalSales").value(0))
                .andExpect(jsonPath("$.data.netSales").value(0));
    }

    @Test
    void 특정일_통계_조회_데이터없음_예외_테스트() throws Exception {
        when(sellerDailyStatisticsService.getDailyStatistics(any(Long.class), any(LocalDate.class)))
                .thenThrow(new BaseException(ErrorEnum.STATISTICS_NOT_FOUND));

        mockMvc.perform(get("/seller/statistics/daily/2025-04-10"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    void 특정일_통계_조회_잘못된날짜형식_400_테스트() throws Exception {
        mockMvc.perform(get("/seller/statistics/daily/20250410"))
                .andExpect(status().is(400))
                .andExpect(jsonPath("$.success").value(false));
    }
}
