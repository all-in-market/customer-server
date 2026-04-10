package com.example.allinmarket.domain.order.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum OrderStatus {
    CREATED("CREATED"),
    PAID("PAID"),
    SHIPPED("SHIPPED"),
    DELIVERED("DELIVERED"),
    REFUNDED("REFUNDED"),
    FAILED("FAILED");

    private final String status;
}

