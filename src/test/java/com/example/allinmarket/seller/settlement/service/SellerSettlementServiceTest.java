package com.example.allinmarket.seller.settlement.service;

import com.example.allinmarket.common.response.PageResponse;
import com.example.allinmarket.domain.sellerdailystatistics.repository.SellerDailyStatisticsRepository;
import com.example.allinmarket.domain.settlement.dto.response.SettlementDetailResponse;
import com.example.allinmarket.domain.settlement.entity.Settlement;
import com.example.allinmarket.domain.settlement.enums.SettlementStatus;
import com.example.allinmarket.domain.settlement.enums.SettlementType;
import com.example.allinmarket.domain.settlement.repository.SettlementRepository;
import com.example.allinmarket.seller.entity.Seller;
import com.example.allinmarket.seller.repository.SellerRepository;
import org.hibernate.exception.ConstraintViolationException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDate;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SellerSettlementServiceTest {

    @Mock
    private SettlementRepository settlementRepository;

    @Mock
    private RedisTemplate<String, Object> redisTemplate;

    @Mock
    private ValueOperations<String, Object> valueOperations;

    @InjectMocks
    private SellerSettlementService sellerSettlementService;

    @Mock
    private SellerRepository sellerRepository;

    @Mock
    private SellerDailyStatisticsRepository sellerDailyStatisticsRepository;

    @BeforeEach
    void setUp() {
        TransactionSynchronizationManager.initSynchronization();
    }

    @AfterEach
    void tearDown() {
        TransactionSynchronizationManager.clearSynchronization();
    }

    private Settlement createSettlementMock(Long settlementId, Long sellerId, BigDecimal amount) {
        Seller seller = mock(Seller.class);
        given(seller.getId()).willReturn(sellerId);

        Settlement settlement = mock(Settlement.class);
        given(settlement.getId()).willReturn(settlementId);
        given(settlement.getSeller()).willReturn(seller);
        given(settlement.getAmount()).willReturn(amount);
        given(settlement.getFee()).willReturn(BigDecimal.valueOf(1000));
        given(settlement.getStatus()).willReturn(SettlementStatus.COMPLETED);
        given(settlement.getType()).willReturn(SettlementType.MID);
        given(settlement.getPeriodStart()).willReturn(LocalDate.of(2024, 1, 1));
        given(settlement.getPeriodEnd()).willReturn(LocalDate.of(2024, 1, 15));
        given(settlement.getCompletedAt()).willReturn(null);
        return settlement;
    }

    @Test
    void 판매자_정산내역_조회_캐시_미스_성공_테스트() {
        // given
        Long sellerId = 1L;
        Pageable pageable = PageRequest.of(0, 10);
        String key = "settlement:" + sellerId + ":v0:" + pageable.getPageNumber() + ":" + pageable.getPageSize();

        given(redisTemplate.opsForValue()).willReturn(valueOperations);
        given(valueOperations.get("settlement:version:" + sellerId)).willReturn("0"); // 버전 키 초기값
        given(valueOperations.get(key)).willReturn(null); // 캐시 미스

        Settlement settlement = createSettlementMock(1L, sellerId, BigDecimal.valueOf(50000));
        Page<Settlement> page = new PageImpl<>(List.of(settlement), pageable, 1);
        given(settlementRepository.findAllBySellerId(sellerId, pageable)).willReturn(page);

        // when
        PageResponse<SettlementDetailResponse> result = sellerSettlementService.findAll(sellerId, pageable);

        // then
        assertNotNull(result);
        assertEquals(1, result.totalElements());
        assertEquals(1, result.content().size());
        assertEquals(1L, result.content().get(0).id());
        assertEquals(sellerId, result.content().get(0).sellerId());
        assertEquals(BigDecimal.valueOf(50000), result.content().get(0).amount());

        // DB 조회 및 캐시 저장 검증
        verify(settlementRepository).findAllBySellerId(sellerId, pageable);
        verify(valueOperations).set(eq(key), any(PageResponse.class), eq(Duration.ofMinutes(10)));
    }

    @Test
    void 판매자_정산내역_조회_캐시_히트_성공_테스트() {
        // given
        Long sellerId = 1L;
        Pageable pageable = PageRequest.of(0, 10);
        String key = "settlement:" + sellerId + ":v0:" + pageable.getPageNumber() + ":" + pageable.getPageSize();

        SettlementDetailResponse cachedItem = new SettlementDetailResponse(
                1L, sellerId, BigDecimal.valueOf(50000), BigDecimal.valueOf(1000),
                SettlementStatus.COMPLETED, SettlementType.MID,
                LocalDate.of(2024, 1, 1), LocalDate.of(2024, 1, 15), null
        );
        PageResponse<SettlementDetailResponse> cachedResponse = new PageResponse<>(
                List.of(cachedItem), 1, 1, 1L, 10, true
        );

        given(redisTemplate.opsForValue()).willReturn(valueOperations);
        given(valueOperations.get("settlement:version:" + sellerId)).willReturn("0"); // 버전 키 초기값
        given(valueOperations.get(key)).willReturn(cachedResponse); // 캐시 히트

        // when
        PageResponse<SettlementDetailResponse> result = sellerSettlementService.findAll(sellerId, pageable);

        // then
        assertEquals(cachedResponse, result);

        // 캐시 히트 시 DB 조회가 일어나지 않았는지 검증
        verify(settlementRepository, never()).findAllBySellerId(any(), any());
    }

    @Test
    void 판매자_정산내역_조회_빈목록_성공_테스트() {
        // given
        Long sellerId = 1L;
        Pageable pageable = PageRequest.of(0, 10);
        String key = "settlement:" + sellerId + ":v0:" + pageable.getPageNumber() + ":" + pageable.getPageSize();

        given(redisTemplate.opsForValue()).willReturn(valueOperations);
        given(valueOperations.get("settlement:version:" + sellerId)).willReturn("0"); // 버전 키 스텁
        given(valueOperations.get(key)).willReturn(null);

        Page<Settlement> emptyPage = new PageImpl<>(List.of(), pageable, 0);
        given(settlementRepository.findAllBySellerId(sellerId, pageable)).willReturn(emptyPage);

        // when
        PageResponse<SettlementDetailResponse> result = sellerSettlementService.findAll(sellerId, pageable);

        // then
        assertNotNull(result);
        assertEquals(0, result.totalElements());
        assertTrue(result.content().isEmpty());
        assertTrue(result.isLast());
    }

    @Test
    void 판매자_정산내역_조회_페이징_테스트() {
        // given
        Long sellerId = 1L;
        Pageable pageable = PageRequest.of(0, 2);
        String key = "settlement:" + sellerId + ":v0:" + pageable.getPageNumber() + ":" + pageable.getPageSize();

        given(redisTemplate.opsForValue()).willReturn(valueOperations);
        given(valueOperations.get("settlement:version:" + sellerId)).willReturn("0"); // 버전 키 스텁
        given(valueOperations.get(key)).willReturn(null);

        Settlement settlement1 = createSettlementMock(1L, sellerId, BigDecimal.valueOf(10000));
        Settlement settlement2 = createSettlementMock(2L, sellerId, BigDecimal.valueOf(20000));
        Page<Settlement> page = new PageImpl<>(List.of(settlement1, settlement2), pageable, 3);
        given(settlementRepository.findAllBySellerId(sellerId, pageable)).willReturn(page);

        // when
        PageResponse<SettlementDetailResponse> result = sellerSettlementService.findAll(sellerId, pageable);

        // then
        assertEquals(3, result.totalElements());
        assertEquals(2, result.content().size());
        assertEquals(2, result.totalPages());
        assertFalse(result.isLast());
    }

    @Test
    void 판매자_정산내역_캐시_범위_초과_페이지_DB_직접_조회_테스트() {
        // given - MAX_CACHEABLE_PAGE(5) 이상의 페이지 요청
        Long sellerId = 1L;
        Pageable pageable = PageRequest.of(5, 10);

        Settlement settlement = createSettlementMock(1L, sellerId, BigDecimal.valueOf(50000));
        Page<Settlement> page = new PageImpl<>(List.of(settlement), pageable, 100);
        given(settlementRepository.findAllBySellerId(sellerId, pageable)).willReturn(page);

        // when
        PageResponse<SettlementDetailResponse> result = sellerSettlementService.findAll(sellerId, pageable);

        // then
        assertNotNull(result);

        // 캐시 범위 초과 시 Redis 접근 자체가 없어야 함
        verify(redisTemplate, never()).opsForValue();
        verify(settlementRepository).findAllBySellerId(sellerId, pageable);
    }

    @Test
    void 정산_생성_시_DB저장과_캐시_버전_증가_테스트() {
        Long sellerId = 1L;
        Seller seller = mock(Seller.class);
        given(seller.getId()).willReturn(sellerId);

        given(sellerRepository.findAllActiveSellers()).willReturn(List.of(seller));

        Object[] row = new Object[]{sellerId, BigDecimal.valueOf(50000)};
        given(sellerDailyStatisticsRepository.sumNetSalesGroupBySeller(any(), any()))
                .willReturn(Collections.singletonList(row));

        // DB 저장은 단순 mock 처리
        given(settlementRepository.save(any(Settlement.class)))
                .willReturn(mock(Settlement.class));

        given(redisTemplate.opsForValue()).willReturn(valueOperations);

        // when
        sellerSettlementService.createSettlement(
                LocalDate.of(2024, 1, 1),
                LocalDate.of(2024, 1, 15),
                SettlementType.MID
        );

        // then
        verify(settlementRepository).save(any(Settlement.class)); // 호출 여부만 검증
        verify(valueOperations).increment("settlement:version:" + sellerId); // 캐시 무효화 검증
    }

    @Test
    void 정산_중복_생성_시_예외_무시_및_캐시_버전_증가_안함() {
        Long sellerId = 1L;
        Seller seller = mock(Seller.class);
        given(seller.getId()).willReturn(sellerId);

        given(sellerRepository.findAllActiveSellers()).willReturn(List.of(seller));

        Object[] row = new Object[]{sellerId, BigDecimal.valueOf(50000)};
        given(sellerDailyStatisticsRepository.sumNetSalesGroupBySeller(any(), any()))
                .willReturn(Collections.singletonList(row));

        // Hibernate 예외를 모킹하여 'uk_settlement_period'를 반환하도록 설정
        ConstraintViolationException mockCv = mock(ConstraintViolationException.class);
        given(mockCv.getConstraintName()).willReturn("uk_settlement_period");

        // Spring의 DataIntegrityViolationException의 원인으로 위 모킹 예외를 주입
        DataIntegrityViolationException exception = new DataIntegrityViolationException("duplicate", mockCv);

        doThrow(exception).when(settlementRepository).save(any(Settlement.class));

        assertDoesNotThrow(() -> sellerSettlementService.createSettlement(
                LocalDate.of(2024, 1, 1),
                LocalDate.of(2024, 1, 15),
                SettlementType.MID
        ));

        verify(valueOperations, never()).increment(anyString());
    }
}