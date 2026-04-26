package com.example.allinmarket.common.scheduler;

import com.example.allinmarket.buyer.order.service.StockReleaseService;
import com.example.allinmarket.domain.order.entity.Order;
import com.example.allinmarket.domain.order.enums.OrderStatus;
import com.example.allinmarket.domain.order.repository.OrderRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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

    @Scheduled(fixedDelay = 60_000)
    public void expireUnpaidOrders() {
        LocalDateTime threshold = LocalDateTime.now().minusMinutes(30);

        List<Order> expiredOrders = orderRepository.findByStatusAndCreatedAtBefore(
                OrderStatus.CREATED,
                threshold
        );

        if (expiredOrders.isEmpty()) return;

        log.info("만료 대상 주문 수: {}", expiredOrders.size());

        expiredOrders.forEach(stockReleaseService::releaseStockAndFailOrder);
    }
}
