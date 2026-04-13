package com.example.allinmarket.seller.orderitem.service;

import com.example.allinmarket.common.response.PageResponse;
import com.example.allinmarket.domain.orderitem.dto.OrderItemDetailResponse;
import com.example.allinmarket.domain.orderitem.entity.OrderItem;
import com.example.allinmarket.domain.orderitem.repository.OrderItemRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class SellerOrderItemService {

    private final OrderItemRepository orderItemRepository;

    public PageResponse<OrderItemDetailResponse> findAll(Long sellerId, Pageable pageable) {
        List<OrderItem> orderItems  = orderItemRepository.findAllBySellerId(sellerId, pageable);

        List<OrderItemDetailResponse> responses = orderItems.stream()
                .map(OrderItemDetailResponse::from)
                .toList();

        int start = (int) pageable.getOffset();
        int end = Math.min(start + pageable.getPageSize(), responses.size());
        Page<OrderItemDetailResponse> page = new PageImpl<>(
                responses.subList(start, end), pageable, responses.size()
        );

        return PageResponse.register(page);
    }
}
