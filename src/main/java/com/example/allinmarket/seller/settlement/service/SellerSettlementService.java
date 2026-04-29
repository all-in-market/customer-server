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
import lombok.RequiredArgsConstructor;
import org.hibernate.exception.ConstraintViolationException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Pageable;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import static com.example.allinmarket.seller.consts.SellerConsts.COMMISSION_RATE;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class SellerSettlementService {

    private final SettlementRepository settlementRepository;
    private final RedisTemplate<String, Object> redisTemplate;
    private final SellerRepository sellerRepository;
    private final SellerDailyStatisticsRepository sellerDailyStatisticsRepository;


    // 캐시 무효화는 정산 데이터가 추가 될 떄 무효화 필요
    public PageResponse<SettlementDetailResponse> findAll(Long sellerId, Pageable pageable) {
        if (pageable.getPageNumber() < 5) {
            String key = "settlement:" + sellerId + ":" + pageable.getPageNumber() + ":" + pageable.getPageSize();

            Object cached = redisTemplate.opsForValue().get(key);
            if (cached instanceof PageResponse<?> pageResponse) {
                return (PageResponse<SettlementDetailResponse>) pageResponse;
            }

            PageResponse<SettlementDetailResponse> response = PageResponse.register(
                    settlementRepository.findAllBySellerId(sellerId, pageable)
                            .map(SettlementDetailResponse::from)
            );

            redisTemplate.opsForValue().set(key, response, Duration.ofMinutes(10));
            return response;
        }

        return PageResponse.register(
                settlementRepository.findAllBySellerId(sellerId, pageable)
                        .map(SettlementDetailResponse::from)
        );
    }

    @Transactional
    public void createSettlement(LocalDate periodStart, LocalDate periodEnd, SettlementType settlementType) {
        // seller 한번에 조회
        List<Seller> sellers = sellerRepository.findAllActiveSellers();

        Map<Long, Seller> sellerMap = sellers.stream()
                .collect(Collectors.toMap(
                        Seller::getId,
                        Function.identity()
                ));

        LocalDateTime completedAt = LocalDateTime.now();

        for (Long sellerId : sellerMap.keySet()) {
            BigDecimal netSales = sellerDailyStatisticsRepository.sumNetSalesBySellerAndPeriod(
                    sellerId,
                    periodStart,
                    periodEnd
            );

            if (netSales == null) {
                netSales = BigDecimal.ZERO;
            }

            BigDecimal fee = netSales.multiply(COMMISSION_RATE).setScale(2, RoundingMode.DOWN);

            BigDecimal settlementAmount = netSales.subtract(fee).setScale(2, RoundingMode.DOWN);

            Seller seller = sellerMap.get(sellerId);

            if (seller == null) {
                continue;
            }

            Settlement settlement = Settlement.of(
                    seller,
                    settlementAmount,
                    fee,
                    SettlementStatus.COMPLETED,
                    settlementType,
                    periodStart,
                    periodEnd,
                    completedAt
            );

            try {
                settlementRepository.saveAndFlush(settlement);

            } catch (DataIntegrityViolationException e) {
                // 중복 생성 방지
                Throwable cause = e.getCause();

                if (cause instanceof ConstraintViolationException exception) {
                    String constraintName = exception.getConstraintName();

                    if ("uk_settlement_period".equals(constraintName)) {
                        continue;
                    }
                }

                throw e;
            }
        }
    }
}
