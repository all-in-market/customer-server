package com.example.allinmarket.buyer.order.service;

import com.example.allinmarket.domain.order.entity.Order;
import com.example.allinmarket.domain.order.enums.OrderStatus;
import com.example.allinmarket.domain.orderitem.entity.OrderItem;
import com.example.allinmarket.domain.orderitem.repository.OrderItemRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class StockReleaseService {

    private final OrderItemRepository orderItemRepository;

    @Transactional
    public void releaseStockAndFailOrder(Order order) {

        if (order.getStatus() != OrderStatus.CREATED) {
            return;
        }

        List<OrderItem> orderItems = orderItemRepository.findAllByOrderIdWithProduct(order.getId());

        orderItems.forEach(item ->
                item.getProduct().releaseStock(item.getQuantity())
        );

        order.fail();

        log.info("재고 복구 및 주문 실패 처리: orderId = {}", order.getId());
    }
}
