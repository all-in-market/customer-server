package com.example.allinmarket.seller.dashboard.service;

import com.example.allinmarket.common.enums.ErrorEnum;
import com.example.allinmarket.common.exception.BaseException;
import com.example.allinmarket.domain.sellerdashboard.entity.SellerDashboard;
import com.example.allinmarket.domain.sellerdashboard.repository.SellerDashboardRepository;
import com.example.allinmarket.seller.dashboard.dto.response.SellerDashboardResponse;
import com.example.allinmarket.seller.entity.Seller;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDate;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class SellerDashboardServiceTest {

    @Mock
    private SellerDashboardRepository sellerDashboardRepository;

    @Mock
    private RedisTemplate<String, Object> redisTemplate;

    @Mock
    private ValueOperations<String, Object> valueOperations;

    @InjectMocks
    private SellerDashboardService sellerDashboardService;

    private Long sellerId;
    private SellerDashboard dashboard;
    private String expectedKey;

    @BeforeEach
    void setUp() {
        sellerId = 1L;
        expectedKey = "dashboard:" + sellerId + ":" + LocalDate.now();

        Seller seller = mock(Seller.class);

        LocalDate today = LocalDate.now();
        dashboard = SellerDashboard.of(
                seller,
                today,
                10,
                8,
                2,
                BigDecimal.valueOf(500000),
                BigDecimal.valueOf(30000)
        );
        ReflectionTestUtils.setField(dashboard, "id", 1L);
    }

    @Test
    void 판매자_대시보드_조회_캐시_미스_성공_테스트() {
        // given - 캐시에 아무것도 없는 상황
        given(redisTemplate.opsForValue()).willReturn(valueOperations);
        given(valueOperations.get(expectedKey)).willReturn(null);
        given(sellerDashboardRepository.findBySellerId(eq(sellerId)))
                .willReturn(Optional.of(dashboard));

        // when
        SellerDashboardResponse response = sellerDashboardService.getSellerDashboard(sellerId);

        // then
        assertNotNull(response);
        assertEquals(0L, response.sellerId());
        assertEquals(LocalDate.now(), response.statDate());
        assertEquals(10, response.totalOrders());
        assertEquals(BigDecimal.valueOf(500000), response.totalSales());
        assertEquals(8, response.totalProductsSold());
        assertEquals(2, response.totalRefunds());
        assertEquals(BigDecimal.valueOf(30000), response.refundAmount());
        assertEquals(0, BigDecimal.valueOf(446500).compareTo(response.settlementAmount()));
        assertEquals(0, BigDecimal.valueOf(23500).compareTo(response.feeAmount()));

        // DB 조회가 실제로 일어났는지 검증
        verify(sellerDashboardRepository).findBySellerId(eq(sellerId));
        // 캐시에 저장됐는지 검증
        verify(valueOperations).set(eq(expectedKey), any(SellerDashboardResponse.class), eq(Duration.ofMinutes(5)));
    }

    @Test
    void 판매자_대시보드_조회_캐시_히트_성공_테스트() {
        // given - 캐시에 이미 데이터가 있는 상황
        given(redisTemplate.opsForValue()).willReturn(valueOperations);
        SellerDashboardResponse cachedResponse = new SellerDashboardResponse(
                sellerId, LocalDate.now(), 10, BigDecimal.valueOf(500000), 8,
                2, BigDecimal.valueOf(30000), BigDecimal.valueOf(445000), BigDecimal.valueOf(25000)
        );
        given(valueOperations.get(expectedKey)).willReturn(cachedResponse);

        // when
        SellerDashboardResponse response = sellerDashboardService.getSellerDashboard(sellerId);

        // then
        assertEquals(cachedResponse, response);

        // 캐시 히트 시 DB 조회가 일어나지 않았는지 검증
        verify(sellerDashboardRepository, never()).findBySellerId(any());
    }

    @Test
    void 판매자_대시보드_조회_없음_실패_테스트() {
        // given
        given(redisTemplate.opsForValue()).willReturn(valueOperations);
        Long notExistSellerId = 999L;
        String notExistKey = "dashboard:" + notExistSellerId + ":" + LocalDate.now();

        given(valueOperations.get(notExistKey)).willReturn(null);
        given(sellerDashboardRepository.findBySellerId(eq(notExistSellerId)))
                .willReturn(Optional.empty());

        // when & then
        BaseException exception = assertThrows(
                BaseException.class,
                () -> sellerDashboardService.getSellerDashboard(notExistSellerId)
        );

        assertEquals(ErrorEnum.DASHBOARD_NOT_FOUND, exception.getErrorEnum());
    }

    @Test
    void 판매자_대시보드_갱신_성공_테스트() {
        // given
        given(redisTemplate.opsForValue()).willReturn(valueOperations);
        given(sellerDashboardRepository.findBySellerId(eq(sellerId)))
                .willReturn(Optional.of(dashboard));

        // when
        SellerDashboardResponse response = sellerDashboardService.refreshSellerDashboard(sellerId);

        // then
        assertNotNull(response);
        assertEquals(LocalDate.now(), response.statDate());
        assertEquals(10, response.totalOrders());
        assertEquals(BigDecimal.valueOf(500000), response.totalSales());
        assertEquals(8, response.totalProductsSold());
        assertEquals(2, response.totalRefunds());
        assertEquals(BigDecimal.valueOf(30000), response.refundAmount());

        // 기존 캐시 삭제 검증
        verify(redisTemplate).delete(expectedKey);
        // DB 조회 검증
        verify(sellerDashboardRepository).findBySellerId(eq(sellerId));
        // 새 캐시 저장 검증
        verify(valueOperations).set(eq(expectedKey), any(SellerDashboardResponse.class), any());
    }

    @Test
    void 판매자_대시보드_갱신_데이터없음_실패_테스트() {
        // given
        given(sellerDashboardRepository.findBySellerId(eq(sellerId)))
                .willReturn(Optional.empty());

        // when & then
        BaseException exception = assertThrows(
                BaseException.class,
                () -> sellerDashboardService.refreshSellerDashboard(sellerId)
        );

        assertEquals(ErrorEnum.DASHBOARD_NOT_FOUND, exception.getErrorEnum());

        // 캐시 삭제는 됐지만 새로 저장은 안 됐는지 검증
        verify(redisTemplate).delete(expectedKey);
        verify(valueOperations, never()).set(any(), any(), any());
    }
}
