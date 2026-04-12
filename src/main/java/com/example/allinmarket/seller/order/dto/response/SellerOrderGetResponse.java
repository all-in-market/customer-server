package com.example.allinmarket.seller.order.dto.response;

import com.example.allinmarket.domain.order.entity.Order;
import com.example.allinmarket.domain.order.enums.OrderStatus;
import com.example.allinmarket.domain.orderitem.dto.response.OrderItemDetailResponse;

import java.math.BigDecimal;
import java.util.List;

public record SellerOrderGetResponse(
        Long id,
        Long buyerId,
        BigDecimal totalAmount,
        OrderStatus status,
        String trackingNumber,
        String recipient,
        String phone,
        String address,
        List<OrderItemDetailResponse> items
) {
    public static SellerOrderGetResponse from(Order order, List<OrderItemDetailResponse> items) {
        return new SellerOrderGetResponse(
                order.getId(),
                order.getBuyer().getId(),
                order.getTotalAmount(),
                order.getStatus(),
                order.getTrackingNumber(),
                order.getRecipient(),
                order.getPhone(),
                order.getAddress(),
                items
        );
    }
}
