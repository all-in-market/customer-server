package com.example.allinmarket.seller.orderitem.service;

import com.example.allinmarket.common.response.PageResponse;
import com.example.allinmarket.domain.orderitem.dto.OrderItemDetailResponse;
import com.example.allinmarket.domain.orderitem.repository.OrderItemRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class SellerOrderItemService {

    private final OrderItemRepository orderItemRepository;

    public PageResponse<OrderItemDetailResponse> findAll(Long sellerId, Pageable pageable) {
        return PageResponse.register(
                orderItemRepository.findAllBySellerId(sellerId, pageable)
                        .map(OrderItemDetailResponse::from)
        );
    }
}
