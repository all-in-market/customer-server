package com.example.allinmarket.buyer.order.service;

import com.example.allinmarket.common.enums.ErrorEnum;
import com.example.allinmarket.common.exception.BaseException;
import com.example.allinmarket.domain.order.entity.Order;
import com.example.allinmarket.domain.order.enums.OrderStatus;
import com.example.allinmarket.domain.order.repository.OrderRepository;
import com.example.allinmarket.domain.orderitem.entity.OrderItem;
import com.example.allinmarket.domain.orderitem.repository.OrderItemRepository;
import com.example.allinmarket.domain.product.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class StockReleaseService {

    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    private final ProductRepository productRepository;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void releaseStockAndFailOrder(Long orderId) {

        Order order = orderRepository.findByIdForUpdate(orderId).orElseThrow(
                () -> new BaseException(ErrorEnum.ORDER_NOT_FOUND)
        );

        if (order.getStatus() != OrderStatus.CREATED) {
            return;
        }

        List<OrderItem> orderItems = orderItemRepository.findAllByOrderIdWithProduct(order.getId());

        // increaseStock은 clearAutomatically = true라 영속성 컨텍스트를 비운다.
        // order 엔티티가 준영속화되어 더티체킹이 무효화되므로, 상태 전이는 재고 복구 루프보다 먼저 처리한다.
        order.fail();

        orderItems.forEach(item -> {
            int updatedRows = productRepository.increaseStock(item.getProduct().getId(), item.getQuantity());
            if (updatedRows != 1) {
                log.warn("재고 복구 실패(대상 상품 없음): orderId = {}, productId = {}, quantity = {}",
                        orderId, item.getProduct().getId(), item.getQuantity());
            }
        });

        log.info("재고 복구 및 주문 실패 처리: orderId = {}", orderId);
    }
}
