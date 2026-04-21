package com.example.allinmarket.domain.sellerdashboard.service;

import com.example.allinmarket.domain.orderitem.entity.OrderItem;
import com.example.allinmarket.domain.orderitem.repository.OrderItemRepository;
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

import static com.example.allinmarket.seller.consts.sellerConsts.COMMISSION_RATE;

@Service
@Slf4j
@RequiredArgsConstructor
@Transactional(propagation = Propagation.REQUIRES_NEW)
public class DashboardService {

    private final SellerDashboardRepository sellerDashboardRepository;
    private final OrderItemRepository orderItemRepository;

    public void updateSellerDashboard(Long orderId) {
        List<OrderItem> orderItems = orderItemRepository.findAllByOrderIdWithSeller(orderId);

        Map<Seller, List<OrderItem>> itemsBySeller = orderItems.stream()
                .collect(Collectors.groupingBy(OrderItem::getSeller));

        itemsBySeller.forEach((seller, items) -> {
            BigDecimal salesAmount = items.stream()
                    .map(item -> item.getUnitPrice().multiply(BigDecimal.valueOf(item.getQuantity())))
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            int productsSold = items.stream()
                    .mapToInt(OrderItem::getQuantity)
                    .sum();

            sellerDashboardRepository.addOrder(seller.getId(), LocalDate.now(), salesAmount, productsSold, COMMISSION_RATE);
        });
    }
}
