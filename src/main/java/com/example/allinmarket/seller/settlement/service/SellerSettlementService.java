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
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import static com.example.allinmarket.seller.consts.SellerConsts.COMMISSION_RATE;
import static com.example.allinmarket.domain.settlement.consts.SettlementConst.SETTLEMENT_CACHE_PREFIX;
import static com.example.allinmarket.domain.settlement.consts.SettlementConst.VERSION_KEY_PREFIX;

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
        if (pageable.getPageNumber() >= 5) {
            return fetchFromDb(sellerId, pageable);
        }

        int version = 0;

        String versionKey = VERSION_KEY_PREFIX + sellerId;

        Object versionObject = redisTemplate.opsForValue().get(versionKey);

        if (versionObject != null) {
            version = Integer.parseInt(versionObject.toString());
        }

        String key = SETTLEMENT_CACHE_PREFIX + sellerId + ":v" + version + ":" + pageable.getPageNumber() + ":" + pageable.getPageSize();

        Object cached = redisTemplate.opsForValue().get(key);

        if (cached instanceof PageResponse<?> pageResponse) {
            return (PageResponse<SettlementDetailResponse>) pageResponse;
        }

        PageResponse<SettlementDetailResponse> response = fetchFromDb(sellerId, pageable);

        redisTemplate.opsForValue().set(key, response, Duration.ofMinutes(10));

        return response;
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

        Map<Long, BigDecimal> netSalesMap = sellerDailyStatisticsRepository.sumNetSalesGroupBySeller(
                periodStart, periodEnd)
                .stream()
                .collect(Collectors.toMap(
                        row -> (Long) row[0],
                        row -> (BigDecimal) row[1]
        ));

        for (Long sellerId : sellerMap.keySet()) {
            BigDecimal netSales = netSalesMap.getOrDefault(
                    sellerId,
                    BigDecimal.ZERO
            );

            BigDecimal fee = netSales.multiply(COMMISSION_RATE).setScale(2, RoundingMode.DOWN);

            BigDecimal settlementAmount = netSales.subtract(fee).setScale(2, RoundingMode.DOWN);

            Seller seller = sellerMap.get(sellerId);

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
                settlementRepository.save(settlement);

            } catch (DataIntegrityViolationException e) {
                // 중복 생성 방지
                if (isUniqueConstraintViolation(e)) continue;

                throw e;
            }
        }

        // 트랜잭션 커밋 직후에만 실행되도록 등록
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                // 모든 판매자의 버전을 일괄 업데이트 (Pipelining 권장)
                redisTemplate.executePipelined((RedisCallback<?>) connection -> {
                    for (Long sellerId : sellerMap.keySet()) {
                        String key = VERSION_KEY_PREFIX + sellerId;
                        connection.incr(key.getBytes());
                    }
                    return null;
                });
            }
        });
    }

    private PageResponse<SettlementDetailResponse> fetchFromDb(Long sellerId, Pageable pageable) {
        return PageResponse.register(
                settlementRepository.findAllBySellerId(sellerId, pageable)
                        .map(SettlementDetailResponse::from)
        );
    }

    private boolean isUniqueConstraintViolation(DataIntegrityViolationException e) {
        return e.getCause() instanceof ConstraintViolationException cv &&
                "uk_settlement_period".equals(cv.getConstraintName());
    }
}
