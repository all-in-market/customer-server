package com.example.allinmarket.seller.dashboard.controller;

import com.example.allinmarket.common.enums.ErrorEnum;
import com.example.allinmarket.common.exception.BaseException;
import com.example.allinmarket.seller.dashboard.dto.response.SellerDashboardResponse;
import com.example.allinmarket.seller.dashboard.service.SellerDashboardService;
import com.example.allinmarket.support.RestDocsControllerTest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(SellerDashBoardController.class)
public class SellerDashBoardControllerTest extends RestDocsControllerTest {

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
    void 판매자_대시보드_조회_성공_테스트() throws Exception {
        setAuthContext(1L);

        LocalDate today = LocalDate.now();
        SellerDashboardResponse response = new SellerDashboardResponse(
                1L, today, 10, BigDecimal.valueOf(500000), 8, 2,
                BigDecimal.valueOf(30000), BigDecimal.valueOf(455000), BigDecimal.valueOf(15000)
        );
        when(sellerDashboardService.getSellerDashboard(1L)).thenReturn(response);

        mockMvc.perform(get("/seller/dashboard"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.message").value("데이터 조회에 성공하였습니다."))
                .andExpect(jsonPath("$.data.sellerId").value(1))
                .andExpect(jsonPath("$.data.statDate").value(today.toString()))
                .andExpect(jsonPath("$.data.totalOrders").value(10))
                .andExpect(jsonPath("$.data.totalSales").value(500000))
                .andExpect(jsonPath("$.data.totalProductsSold").value(8))
                .andExpect(jsonPath("$.data.totalRefunds").value(2))
                .andExpect(jsonPath("$.data.refundAmount").value(30000))
                .andExpect(jsonPath("$.data.settlementAmount").value(455000))
                .andExpect(jsonPath("$.data.feeAmount").value(15000));
    }

    @Test
    void 판매자_대시보드_조회_미인증_예외_테스트() throws Exception {
        SecurityContextHolder.clearContext();

        mockMvc.perform(get("/seller/dashboard"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.status").value(401));
    }

    @Test
    void 판매자_대시보드_조회_데이터없음_예외_테스트() throws Exception {
        setAuthContext(999L);
        when(sellerDashboardService.getSellerDashboard(999L))
                .thenThrow(new BaseException(ErrorEnum.NOT_FOUND));

        mockMvc.perform(get("/seller/dashboard"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.message").value(ErrorEnum.NOT_FOUND.getMessage()));
    }

    @Test
    void 판매자_대시보드_갱신_성공_테스트() throws Exception {
        setAuthContext(1L);

        LocalDate today = LocalDate.now();
        SellerDashboardResponse response = new SellerDashboardResponse(
                1L, today, 10, BigDecimal.valueOf(500000), 8, 2,
                BigDecimal.valueOf(30000), BigDecimal.valueOf(455000), BigDecimal.valueOf(15000)
        );
        when(sellerDashboardService.refreshSellerDashboard(1L)).thenReturn(response);

        mockMvc.perform(post("/seller/dashboard/refresh"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.message").value("데이터 조회에 성공하였습니다."))
                .andExpect(jsonPath("$.data.sellerId").value(1))
                .andExpect(jsonPath("$.data.statDate").value(today.toString()))
                .andExpect(jsonPath("$.data.totalOrders").value(10))
                .andExpect(jsonPath("$.data.totalSales").value(500000))
                .andExpect(jsonPath("$.data.totalProductsSold").value(8))
                .andExpect(jsonPath("$.data.totalRefunds").value(2))
                .andExpect(jsonPath("$.data.refundAmount").value(30000))
                .andExpect(jsonPath("$.data.settlementAmount").value(455000))
                .andExpect(jsonPath("$.data.feeAmount").value(15000));
    }

    @Test
    void 판매자_대시보드_갱신_미인증_예외_테스트() throws Exception {
        SecurityContextHolder.clearContext();

        mockMvc.perform(post("/seller/dashboard/refresh"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.status").value(401));
    }

    @Test
    void 판매자_대시보드_갱신_데이터없음_예외_테스트() throws Exception {
        setAuthContext(999L);
        when(sellerDashboardService.refreshSellerDashboard(999L))
                .thenThrow(new BaseException(ErrorEnum.DASHBOARD_NOT_FOUND));

        mockMvc.perform(post("/seller/dashboard/refresh"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.message").value(ErrorEnum.DASHBOARD_NOT_FOUND.getMessage()));
    }
}
