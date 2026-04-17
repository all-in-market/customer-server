package com.example.allinmarket.domain.payment.enums;


public enum PaymentStatus {
    PENDING,
    SUCCESS,
    FAILED,
    REFUNDED;

    public boolean paymentCanTransitToTargetStatus(PaymentStatus targetStatus) {
        if (targetStatus == null) {
            return false;
        }

        return switch (this) {
            case PENDING -> targetStatus == SUCCESS || targetStatus == FAILED || targetStatus == REFUNDED;
            case SUCCESS, FAILED -> targetStatus == REFUNDED;
            case REFUNDED -> false;
        };
    }
}
