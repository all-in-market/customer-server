package com.example.allinmarket.domain.refund.enums;

public enum RefundStatus {

    NONE,
    PENDING,
    SUCCESS,
    FAILED,
    DENIED;

    public boolean refundCanTransitToTargetStatus(RefundStatus targetStatus) {
        if (targetStatus == null) {
            return false;
        }

        return switch (this) {
            case NONE -> targetStatus == PENDING;
            case PENDING ->  targetStatus == SUCCESS || targetStatus == FAILED || targetStatus == DENIED;
            case FAILED -> targetStatus == PENDING;
            case SUCCESS, DENIED -> false;
        };
    }
}
