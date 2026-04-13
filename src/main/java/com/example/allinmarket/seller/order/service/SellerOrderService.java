package com.example.allinmarket.seller.order.service;

import com.example.allinmarket.buyer.order.dto.response.OrderDetailResponse;
import com.example.allinmarket.common.enums.ErrorEnum;
import com.example.allinmarket.common.exception.BaseException;
import com.example.allinmarket.common.response.PageResponse;
import com.example.allinmarket.domain.order.entity.Order;
import com.example.allinmarket.domain.order.repository.OrderRepository;
import com.example.allinmarket.domain.orderitem.dto.response.OrderItemDetailResponse;
import com.example.allinmarket.domain.orderitem.entity.OrderItem;
import com.example.allinmarket.domain.orderitem.repository.OrderItemRepository;
import com.example.allinmarket.seller.order.dto.response.SellerOrderGetResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class SellerOrderService {

    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;

    public PageResponse<OrderDetailResponse> findAll(Long sellerId, Pageable pageable) {
        List<OrderItem> orderItems  = orderItemRepository.findAllBySellerId(sellerId, pageable);

        Map<Order, List<OrderItem>> groupedByOrder = orderItems.stream()
                .collect(Collectors.groupingBy(OrderItem::getOrder));

        List<OrderDetailResponse> responses = groupedByOrder.entrySet().stream()
                .map(entry -> OrderDetailResponse.from(entry.getKey()))
                .toList();

        int start = (int) pageable.getOffset();
        int end = Math.min(start + pageable.getPageSize(), responses.size());
        Page<OrderDetailResponse> page = new PageImpl<>(
                responses.subList(start, end), pageable, responses.size()
        );

        return PageResponse.register(page);
    }

    public SellerOrderGetResponse findOne(Long sellerId, Long orderId) {
        List<OrderItem> orderItems = orderItemRepository.findAllBySellerIdAndOrderId(sellerId, orderId);

        if (orderItems.isEmpty()) {
            throw new BaseException(ErrorEnum.ORDER_NOT_FOUND);
        }

        Order order = orderItems.get(0).getOrder();
        List<OrderItemDetailResponse> items = orderItems.stream()
                .map(OrderItemDetailResponse::from)
                .toList();

        return SellerOrderGetResponse.from(order, items);
    }
}
