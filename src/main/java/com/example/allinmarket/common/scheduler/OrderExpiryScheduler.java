package com.example.allinmarket.common.scheduler;

import com.example.allinmarket.buyer.order.service.StockReleaseService;
import com.example.allinmarket.domain.order.entity.Order;
import com.example.allinmarket.domain.order.enums.OrderStatus;
import com.example.allinmarket.domain.order.repository.OrderRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class OrderExpiryScheduler {

    private final OrderRepository orderRepository;
    private final StockReleaseService stockReleaseService;

    @Value("${app.scheduling.order-expiry.chunk-size:100}")
    private int chunkSize;

    @Value("${app.scheduling.order-expiry.max-loops:10}")
    private int maxLoops;

    @Scheduled(fixedDelay = 60_000)
    public void expireUnpaidOrders() {
        LocalDateTime threshold = LocalDateTime.now().minusMinutes(30);
        Pageable pageable = PageRequest.of(0, chunkSize);

        int loopCount = 0;
        List<Order> expiredOrders;
        do {
            expiredOrders = orderRepository.findByStatusAndCreatedAtBefore(
                    OrderStatus.CREATED,
                    threshold,
                    pageable
            );

            if (expiredOrders.isEmpty()) return;

            log.info("만료 대상 주문 수: {}", expiredOrders.size());

            int successCount = 0;
            for (Order order : expiredOrders) {
                try {
                    stockReleaseService.releaseStockAndFailOrder(order.getId());
                    successCount++;
                } catch (Exception e) {
                    log.error("만료 주문 처리 실패: orderId={}", order.getId(), e);
                }
            }

            loopCount++;

            if (successCount == 0) {
                log.warn("만료 주문 처리에서 진행 없음 - 루프 중단. 남은 배치 크기={}", expiredOrders.size());
                return;
            }

        } while (expiredOrders.size() == chunkSize && loopCount < maxLoops);

        if (loopCount >= maxLoops && expiredOrders.size() == chunkSize) {
            log.warn("만료 주문 처리 루프 상한({}회) 도달 - 남은 백로그가 있을 수 있음", maxLoops);
        }
    }
}
