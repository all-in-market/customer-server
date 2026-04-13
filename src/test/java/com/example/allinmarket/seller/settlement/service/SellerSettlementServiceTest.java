package com.example.allinmarket.seller.settlement.service;

import com.example.allinmarket.common.response.PageResponse;
import com.example.allinmarket.domain.settlement.dto.response.SettlementDetailResponse;
import com.example.allinmarket.domain.settlement.entity.Settlement;
import com.example.allinmarket.domain.settlement.enums.SettlementStatus;
import com.example.allinmarket.domain.settlement.enums.SettlementType;
import com.example.allinmarket.domain.settlement.repository.SettlementRepository;
import com.example.allinmarket.seller.entity.Seller;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

@ExtendWith(MockitoExtension.class)
class SellerSettlementServiceTest {

    @Mock
    private SettlementRepository settlementRepository;

    @InjectMocks
    private SellerSettlementService sellerSettlementService;

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
    void 판매자_정산내역_조회_성공_테스트() {
        // given
        Long sellerId = 1L;
        Pageable pageable = PageRequest.of(0, 10);

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
    }

    @Test
    void 판매자_정산내역_조회_빈목록_성공_테스트() {
        // given
        Long sellerId = 1L;
        Pageable pageable = PageRequest.of(0, 10);

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
    void 판매자_정산내역_조회_두번째_페이지_테스트() {
        // given
        Long sellerId = 1L;
        Pageable pageable = PageRequest.of(1, 2);

        Settlement settlement3 = createSettlementMock(3L, sellerId, BigDecimal.valueOf(30000));

        Page<Settlement> page = new PageImpl<>(List.of(settlement3), pageable, 3);

        given(settlementRepository.findAllBySellerId(sellerId, pageable)).willReturn(page);

        // when
        PageResponse<SettlementDetailResponse> result = sellerSettlementService.findAll(sellerId, pageable);

        // then
        assertEquals(3, result.totalElements());
        assertEquals(1, result.content().size());
        assertEquals(2, result.currentPage());
        assertTrue(result.isLast());
    }
}