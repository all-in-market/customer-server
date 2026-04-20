package com.example.allinmarket.seller.dashboard.service;

import com.example.allinmarket.common.enums.ErrorEnum;
import com.example.allinmarket.common.exception.BaseException;
import com.example.allinmarket.domain.sellerdashboard.entity.SellerDashboard;
import com.example.allinmarket.domain.sellerdashboard.repository.SellerDashboardRepository;
import com.example.allinmarket.seller.dashboard.dto.response.SellerDashboardResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDate;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class SellerDashboardService {

    private final SellerDashboardRepository sellerDashboardRepository;
    private final RedisTemplate<String, Object> redisTemplate;

    // 이 후 관리자가 환불 처리 시 캐시 무효화 필요
    public SellerDashboardResponse getSellerDashboard(Long sellerId) {
        String key = "dashboard:" + sellerId + ":" + LocalDate.now();

        Object cached = redisTemplate.opsForValue().get(key);
        if (cached instanceof SellerDashboardResponse response) {
            return response;
        }

        SellerDashboard sellerDashboard = sellerDashboardRepository.findBySellerIdAndStatDate(sellerId, LocalDate.now())
                .orElseThrow(() -> new BaseException(ErrorEnum.DASHBOARD_NOT_FOUND));

        SellerDashboardResponse response = SellerDashboardResponse.from(sellerDashboard);
        redisTemplate.opsForValue().set(key, response, Duration.ofMinutes(5));

        return response;
    }

    @Transactional
    public SellerDashboardResponse refreshSellerDashboard(Long sellerId) {
        String key = "dashboard:" + sellerId + ":" + LocalDate.now();
        redisTemplate.delete(key);

        SellerDashboard sellerDashboard = sellerDashboardRepository.findBySellerIdAndStatDate(sellerId, LocalDate.now())
                .orElseThrow(() -> new BaseException(ErrorEnum.DASHBOARD_NOT_FOUND));

        SellerDashboardResponse response = SellerDashboardResponse.from(sellerDashboard);
        redisTemplate.opsForValue().set(key, response, Duration.ofMinutes(5));

        return response;
    }
}
