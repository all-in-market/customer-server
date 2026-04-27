package com.example.allinmarket.seller.dashboard.service;

import com.example.allinmarket.common.enums.ErrorEnum;
import com.example.allinmarket.common.exception.BaseException;
import com.example.allinmarket.domain.orderitem.entity.OrderItem;
import com.example.allinmarket.domain.orderitem.repository.OrderItemRepository;
import com.example.allinmarket.domain.sellerdashboard.repository.SellerDashboardRepository;
import com.example.allinmarket.domain.sellerdashboard.service.DashboardRowCreatorService;
import com.example.allinmarket.seller.entity.Seller;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static com.example.allinmarket.seller.consts.sellerConsts.COMMISSION_RATE;

@Slf4j
@Service
@RequiredArgsConstructor
public class SellerDashboardProcessor {
    private final SellerDashboardRepository sellerDashboardRepository;
    private final DashboardRowCreatorService dashboardRowCreatorService;
    private final RedisTemplate<String, Object> redisTemplate;

    @Retryable(retryFor = Exception.class, maxAttempts = 3, backoff = @Backoff(delay = 1000))
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void processSingleSeller(Seller seller, List<OrderItem> items, LocalDate statDate, Long orderId) {
        BigDecimal salesAmount = items.stream()
                .map(item -> item.getUnitPrice().multiply(BigDecimal.valueOf(item.getQuantity())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        int productsSold = items.stream()
                .mapToInt(OrderItem::getQuantity)
                .sum();

        // 오늘 기준 대시보드 row에 누적하기 위해 statDate 사용
        // sellerId + statDate 조합으로 하루 단위 매출을 관리
        String key = "dashboard:" + seller.getId() + ":" + statDate;

        // 기존 dashboard row에 누적 업데이트 수행
        // 반환값(int updated)은 실제 없데이트된 row 수를 의미
        int updated = sellerDashboardRepository.addOrder(
                seller.getId(),
                salesAmount,
                productsSold,
                COMMISSION_RATE,
                statDate
        );

        // update 결과가 0이면 dashboard row가 존재하지 않는 상태
        // 신규 row를 생성한 후 다시 업데이트를 수행
        if (updated == 0) {
            log.warn("대시보드 row 없음 -> 생성 시도 : sellerId = {}", seller.getId());

            dashboardRowCreatorService.createDashboardIfNotExists(seller, statDate);

            // row 생성 후 재시도
            updated = sellerDashboardRepository.addOrder(
                    seller.getId(),
                    salesAmount,
                    productsSold,
                    COMMISSION_RATE,
                    statDate
            );

            if (updated == 0) {
                log.error("대시보드 row 생성 후 업데이트 최종 실패 : sellerId = {}, statDate = {}", seller.getId(), statDate);

                throw new BaseException(ErrorEnum.DASHBOARD_UPDATE_FAILED);
            }
        }

        // dashboard 업데이트 성공 시 캐시 무효화
        if (updated > 0) {
            redisTemplate.delete(key);
        }

        log.info("대시보드 업데이트 성공: orderId = {}, sellerId = {}", orderId, seller.getId());
    }
}
