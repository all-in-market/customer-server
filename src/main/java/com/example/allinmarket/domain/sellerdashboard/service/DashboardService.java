package com.example.allinmarket.domain.sellerdashboard.service;

import com.example.allinmarket.domain.orderitem.entity.OrderItem;
import com.example.allinmarket.domain.orderitem.repository.OrderItemRepository;
import com.example.allinmarket.seller.dashboard.service.SellerDashboardProcessor;
import com.example.allinmarket.seller.entity.Seller;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;


@Service
@Slf4j
@RequiredArgsConstructor
public class DashboardService {

    private final OrderItemRepository orderItemRepository;
    private final SellerDashboardProcessor sellerDashboardProcessor;

    // 결제 성공 이후 판매자 대시보드를 비동기로 업데이트(Outbox 이용)
    // seller 단위로 처리 분리, 각 seller는 독립 트랜잭션
    // 실패 시 seller 단위 retry 실행
    @Transactional
    public void updateSellerDashboard(Long orderId, LocalDate statDate) {
        List<OrderItem> orderItems = orderItemRepository.findAllByOrderIdWithSeller(orderId);

        Map<Seller, List<OrderItem>> itemsBySeller = orderItems.stream()
                .collect(Collectors.groupingBy(OrderItem::getSeller));

        for (Map.Entry<Seller, List<OrderItem>> entry : itemsBySeller.entrySet()) {
            Seller seller = entry.getKey();
            List<OrderItem> items = entry.getValue();

            sellerDashboardProcessor.processSingleSeller(
                    seller,
                    items,
                    statDate,
                    orderId
            );
        }
    }
}
