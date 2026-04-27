package com.example.allinmarket.domain.sellerdashboard.service;

import com.example.allinmarket.common.enums.ErrorEnum;
import com.example.allinmarket.common.exception.BaseException;
import com.example.allinmarket.domain.orderitem.entity.OrderItem;
import com.example.allinmarket.domain.orderitem.repository.OrderItemRepository;
import com.example.allinmarket.domain.sellerdashboard.entity.SellerDashboard;
import com.example.allinmarket.domain.sellerdashboard.repository.SellerDashboardRepository;
import com.example.allinmarket.seller.entity.Seller;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static com.example.allinmarket.seller.consts.sellerConsts.COMMISSION_RATE;

@Service
@Slf4j
@RequiredArgsConstructor
@Transactional(propagation = Propagation.REQUIRES_NEW)
public class DashboardService {

    private final SellerDashboardRepository sellerDashboardRepository;
    private final OrderItemRepository orderItemRepository;
    private final RedisTemplate<String, Object> redisTemplate;
    private final DashboardRowCreatorService dashboardRowCreatorService;

    // 결제 성공 이후 판매자 대시보드를 비동기로 업데이트
    // @Retryable : 오류 발생 시 최대 3회까지 재시도 추가
    @Retryable(retryFor = Exception.class, maxAttempts = 3, backoff = @Backoff(delay = 1000))
    @Async("dashboardExecutor")
    public void updateSellerDashboard(Long orderId, LocalDate statDate) {
        try {
            List<OrderItem> orderItems = orderItemRepository.findAllByOrderIdWithSeller(orderId);

            Map<Seller, List<OrderItem>> itemsBySeller = orderItems.stream()
                    .collect(Collectors.groupingBy(OrderItem::getSeller));

            // 오늘 기준 대시보드 row에 누적하기 위해 statDate 사용
            // sellerId + statDate 조합으로 하루 단위 매출을 관리
            LocalDate today = statDate;

            itemsBySeller.forEach((seller, items) -> {
                BigDecimal salesAmount = items.stream()
                        .map(item -> item.getUnitPrice().multiply(BigDecimal.valueOf(item.getQuantity())))
                        .reduce(BigDecimal.ZERO, BigDecimal::add);

                int productsSold = items.stream()
                        .mapToInt(OrderItem::getQuantity)
                        .sum();

                String key = "dashboard:" + seller.getId() + ":" + today;

                // 기존 dashboard row에 누적 업데이트 수행
                // 반환값(int updated)은 실제 없데이트된 row 수를 의미
                int updated = sellerDashboardRepository.addOrder(
                        seller.getId(),
                        salesAmount,
                        productsSold,
                        COMMISSION_RATE,
                        today
                );

                // update 결과가 0이면 dashboard row가 존재하지 않는 상태
                // 신규 row를 생성한 후 다시 업데이트를 수행
                if (updated == 0) {
                    log.warn("대시보드 row 없음 -> 생성 시도 : sellerId = {}", seller.getId());

                    dashboardRowCreatorService.createDashboardIfNotExists(seller, today);

                    // row 생성 후 재시도
                    updated = sellerDashboardRepository.addOrder(
                            seller.getId(),
                            salesAmount,
                            productsSold,
                            COMMISSION_RATE,
                            today
                    );

                    if (updated == 0) {
                        log.error("대시보드 row 생성 후 업데이트 최종 실패 : sellerId = {}, statDate = {}", seller.getId(), today);

                        throw new BaseException(ErrorEnum.DASHBOARD_UPDATE_FAILED);
                    }
                }

                // dashboard 업데이트 성공 시 캐시 무효화
                if (updated > 0) {
                    redisTemplate.delete(key);
                }

                log.info("대시보드 업데이트 성공: orderId = {}, sellerId = {}", orderId, seller.getId());
            });
        } catch (Exception e) {
            log.error("대시보드 업데이트 실패: orderId = {}", orderId, e);
            throw e;
        }
    }
}
