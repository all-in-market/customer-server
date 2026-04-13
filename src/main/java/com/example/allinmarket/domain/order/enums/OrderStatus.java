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

    public boolean canTransitToTargetStatus(OrderStatus targetStatus) {
        if(targetStatus == null){
            return false;
        }

        return switch (this) {
            case CREATED -> targetStatus == PAID || targetStatus == FAILED;
            case PAID -> targetStatus == SHIPPED || targetStatus == REFUNDED;
            case SHIPPED -> targetStatus == DELIVERED;
            case DELIVERED -> targetStatus == REFUNDED;
            case REFUNDED, FAILED -> false;
        };
    }
}

