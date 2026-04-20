package com.example.allinmarket.domain.sellerdashboard.service;

import com.example.allinmarket.domain.orderitem.entity.OrderItem;
import com.example.allinmarket.domain.orderitem.repository.OrderItemRepository;
import com.example.allinmarket.domain.sellerdashboard.entity.SellerDashboard;
import com.example.allinmarket.domain.sellerdashboard.repository.SellerDashboardRepository;
import com.example.allinmarket.seller.entity.Seller;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
@Transactional(propagation = Propagation.REQUIRES_NEW)
public class DashboardService {

    private final SellerDashboardRepository sellerDashboardRepository;
    private final OrderItemRepository orderItemRepository;

    public void updateSellerDashboard(Long orderId) {
        List<OrderItem> orderItems = orderItemRepository.findAllByOrderIdWithSeller(orderId);

        // orderItems들을 seller 기준으로 그룹화
        Map<Seller, List<OrderItem>> itemsBySeller = orderItems.stream()
                .collect(Collectors.groupingBy(OrderItem::getSeller));

        // seller별로 집계
        itemsBySeller.forEach((seller, items) -> {
                    BigDecimal salesAmount = items.stream()
                            .map(item -> item.getUnitPrice().multiply(BigDecimal.valueOf(item.getQuantity())))
                            .reduce(BigDecimal.ZERO, BigDecimal::add); // map()으로 변환된 각 아이템의 금액들을 하나의 합계로 합산(BigDecimal.ZERO는 합산 시작값)

                    int productsSold = items.stream()
                            .mapToInt(OrderItem::getQuantity)
                            .sum();

                    // 대시보드 upsert
                    SellerDashboard dashboard = sellerDashboardRepository
                            .findBySellerIdAndStatDate(seller.getId(), LocalDate.now())
                            .orElseGet(() -> SellerDashboard.of(seller, LocalDate.now(), 0, 0, 0, BigDecimal.ZERO, BigDecimal.ZERO));
                    dashboard.addOrder(salesAmount, productsSold);
                    sellerDashboardRepository.save(dashboard);
                }
        );
    }

}
