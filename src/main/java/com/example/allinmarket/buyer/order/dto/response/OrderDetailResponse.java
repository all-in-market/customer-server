package com.example.allinmarket.buyer.order.dto.response;

import com.example.allinmarket.domain.order.entity.Order;
import com.example.allinmarket.domain.order.enums.OrderStatus;

import java.math.BigDecimal;

public record OrderDetailResponse(
        Long orderId,
        Long buyerId,
        BigDecimal totalAmount,
        OrderStatus status,
        String trackingNumber,
        String recipient,
        String address
) {
    public static OrderDetailResponse from(Order order) {
        return new OrderDetailResponse(
                order.getId(),
                order.getBuyer().getId(),
                order.getTotalAmount(),
                order.getStatus(),
                order.getTrackingNumber(),
                order.getRecipient(),
                order.getAddress()
        );
    }
}
