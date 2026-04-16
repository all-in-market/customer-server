package com.example.allinmarket.seller.dailystatistics.service;

import com.example.allinmarket.common.enums.ErrorEnum;
import com.example.allinmarket.common.exception.BaseException;
import com.example.allinmarket.domain.sellerdailystatistics.dto.response.DailyStatisticsResponse;
import com.example.allinmarket.domain.sellerdailystatistics.entity.SellerDailyStatistics;
import com.example.allinmarket.domain.sellerdailystatistics.repository.SellerDailyStatisticsRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

@ExtendWith(MockitoExtension.class)
class SellerDailyStatisticsServiceTest {

    @Mock
    private SellerDailyStatisticsRepository sellerDailyStatisticsRepository;

    @InjectMocks
    private SellerDailyStatisticsService sellerDailyStatisticsService;

    // getId(), getSeller() 제거 - DailyStatisticsResponse.from()에서 미사용
    private SellerDailyStatistics createStatsMock(LocalDate date) {
        SellerDailyStatistics stats = mock(SellerDailyStatistics.class);
        given(stats.getStatDate()).willReturn(date);
        given(stats.getTotalOrders()).willReturn(5);
        given(stats.getTotalItems()).willReturn(10);
        given(stats.getTotalRefunds()).willReturn(1);
        given(stats.getTotalSales()).willReturn(BigDecimal.valueOf(150000));
        given(stats.getRefundAmount()).willReturn(BigDecimal.valueOf(30000));
        given(stats.getNetSales()).willReturn(BigDecimal.valueOf(120000));
        return stats;
    }

    @Test
    void 특정일_통계_조회_성공_테스트() {
        // given
        Long sellerId = 1L;
        LocalDate date = LocalDate.of(2025, 4, 10);
        SellerDailyStatistics stats = createStatsMock(date);

        given(sellerDailyStatisticsRepository.findBySellerIdAndStatDate(sellerId, date))
                .willReturn(Optional.of(stats));

        // when
        DailyStatisticsResponse result = sellerDailyStatisticsService.getDailyStatistics(sellerId, date);

        // then
        assertNotNull(result);
        assertEquals(date, result.statDate());
        assertEquals(5, result.totalOrders());
        assertEquals(10, result.totalItems());
        assertEquals(1, result.totalRefunds());
        assertEquals(BigDecimal.valueOf(150000), result.totalSales());
        assertEquals(BigDecimal.valueOf(30000), result.refundAmount());
        assertEquals(BigDecimal.valueOf(120000), result.netSales());
    }

    @Test
    void 특정일_통계_조회_데이터없음_예외_테스트() {
        // given
        Long sellerId = 1L;
        LocalDate date = LocalDate.of(2025, 4, 10);

        given(sellerDailyStatisticsRepository.findBySellerIdAndStatDate(sellerId, date))
                .willReturn(Optional.empty());

        // when & then
        BaseException exception = assertThrows(BaseException.class,
                () -> sellerDailyStatisticsService.getDailyStatistics(sellerId, date));

        assertEquals(ErrorEnum.STATISTICS_NOT_FOUND, exception.getErrorEnum());
    }

    @Test
    void 특정일_통계_조회_오늘날짜_성공_테스트() {
        // given
        Long sellerId = 1L;
        LocalDate today = LocalDate.now();
        SellerDailyStatistics stats = createStatsMock(today);

        given(sellerDailyStatisticsRepository.findBySellerIdAndStatDate(sellerId, today))
                .willReturn(Optional.of(stats));

        // when
        DailyStatisticsResponse result = sellerDailyStatisticsService.getDailyStatistics(sellerId, today);

        // then
        assertNotNull(result);
        assertEquals(today, result.statDate());
    }

    @Test
    void 특정일_통계_조회_판매없는날_영값_성공_테스트() {
        // given
        Long sellerId = 1L;
        LocalDate date = LocalDate.of(2025, 4, 10);

        // seller, seller.getId() stub 제거 - from()에서 미사용
        SellerDailyStatistics emptyStats = mock(SellerDailyStatistics.class);
        given(emptyStats.getStatDate()).willReturn(date);
        given(emptyStats.getTotalOrders()).willReturn(0);
        given(emptyStats.getTotalItems()).willReturn(0);
        given(emptyStats.getTotalRefunds()).willReturn(0);
        given(emptyStats.getTotalSales()).willReturn(BigDecimal.ZERO);
        given(emptyStats.getRefundAmount()).willReturn(BigDecimal.ZERO);
        given(emptyStats.getNetSales()).willReturn(BigDecimal.ZERO);

        given(sellerDailyStatisticsRepository.findBySellerIdAndStatDate(sellerId, date))
                .willReturn(Optional.of(emptyStats));

        // when
        DailyStatisticsResponse result = sellerDailyStatisticsService.getDailyStatistics(sellerId, date);

        // then
        assertNotNull(result);
        assertEquals(0, result.totalOrders());
        assertEquals(BigDecimal.ZERO, result.totalSales());
        assertEquals(BigDecimal.ZERO, result.netSales());
    }
}