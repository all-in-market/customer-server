package com.example.allinmarket.seller.dashboard.service;

import com.example.allinmarket.common.enums.ErrorEnum;
import com.example.allinmarket.common.exception.BaseException;
import com.example.allinmarket.domain.sellerdashboard.entity.SellerDashboard;
import com.example.allinmarket.domain.sellerdashboard.repository.SellerDashboardRepository;
import com.example.allinmarket.seller.dashboard.dto.response.SellerDashboardResponse;
import com.example.allinmarket.seller.entity.Seller;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

@ExtendWith(MockitoExtension.class)
public class SellerDashboardServiceTest {

    @Mock
    private SellerDashboardRepository sellerDashboardRepository;

    @InjectMocks
    private SellerDashboardService sellerDashboardService;

    @Test
    void 판매자_대시보드_조회_성공_테스트() {
        // given
        Long sellerId = 1L;

        Seller seller = mock(Seller.class);
        given(seller.getId()).willReturn(sellerId);

        LocalDate today = LocalDate.now();
        SellerDashboard dashboard = SellerDashboard.of(
                seller,
                today,
                10,
                8,
                2,
                BigDecimal.valueOf(500000),
                BigDecimal.valueOf(30000)
        );
        ReflectionTestUtils.setField(dashboard, "id", 1L);

        given(sellerDashboardRepository.findBySellerIdAndStatDate(eq(sellerId), any(LocalDate.class)))
                .willReturn(Optional.of(dashboard));

        // when
        SellerDashboardResponse response = sellerDashboardService.getSellerDashboard(sellerId);

        // then
        assertNotNull(response);
        assertEquals(sellerId, response.sellerId());
        assertEquals(today, response.statDate());
        assertEquals(10, response.totalOrders());
        assertEquals(BigDecimal.valueOf(500000), response.totalSales());
        assertEquals(8, response.totalProductsSold());
        assertEquals(2, response.totalRefunds());
        assertEquals(BigDecimal.valueOf(30000), response.refundAmount());
        assertEquals(0, BigDecimal.valueOf(445000).compareTo(response.settlementAmount()));
        assertEquals(0, BigDecimal.valueOf(25000).compareTo(response.feeAmount()));
    }

    @Test
    void 판매자_대시보드_조회_없음_실패_테스트() {
        // given
        Long sellerId = 999L;

        given(sellerDashboardRepository.findBySellerIdAndStatDate(eq(sellerId), any(LocalDate.class)))
                .willReturn(Optional.empty());

        // when & then
        BaseException exception = assertThrows(
                BaseException.class,
                () -> sellerDashboardService.getSellerDashboard(sellerId)
        );

        assertEquals(ErrorEnum.DASHBOARD_NOT_FOUND, exception.getErrorEnum());
    }
}
