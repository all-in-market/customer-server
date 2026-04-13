package com.example.allinmarket.buyer.order.dto.response;

import com.example.allinmarket.domain.order.entity.Order;
import com.example.allinmarket.domain.order.enums.OrderStatus;
import com.example.allinmarket.domain.orderitem.dto.OrderItemDetailResponse;

import java.math.BigDecimal;
import java.util.List;

public record OrderWithOrderItemDetailResponse(
        Long orderId,
        Long buyerId,
        BigDecimal totalAmount,
        OrderStatus status,
        String trackingNumber,
        String recipient,
        String address,
        List<OrderItemDetailResponse> items
) {
    public static OrderWithOrderItemDetailResponse from(Order order, List<OrderItemDetailResponse> items) {
        return new OrderWithOrderItemDetailResponse(
                order.getId(),
                order.getBuyer().getId(),
                order.getTotalAmount(),
                order.getStatus(),
                order.getTrackingNumber(),
                order.getRecipient(),
                order.getAddress(),
                items
        );
    }
}
