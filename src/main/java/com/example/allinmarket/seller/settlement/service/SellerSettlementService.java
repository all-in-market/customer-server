package com.example.allinmarket.seller.settlement.service;

import com.example.allinmarket.common.response.PageResponse;
import com.example.allinmarket.domain.settlement.dto.response.SettlementDetailResponse;
import com.example.allinmarket.domain.settlement.repository.SettlementRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.time.Duration;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class SellerSettlementService {

    private final SettlementRepository settlementRepository;
    private final RedisTemplate<String, Object> redisTemplate;


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
}
