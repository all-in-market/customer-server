package com.example.allinmarket.domain.orderitem.dto;

import com.example.allinmarket.domain.orderitem.entity.OrderItem;

import java.math.BigDecimal;

public record OrderItemDetailResponse(
        Long id,
        Long orderId,
        Long productId,
        Long sellerId,
        String productName,
        BigDecimal unitPrice,
        int quantity
) {
    public static OrderItemDetailResponse from(OrderItem orderItem) {
        return new OrderItemDetailResponse(
                orderItem.getId(),
                orderItem.getOrder().getId(),
                orderItem.getProduct().getId(),
                orderItem.getSeller().getId(),
                orderItem.getProductName(),
                orderItem.getUnitPrice(),
                orderItem.getQuantity()
        );
    }
}
