package com.example.allinmarket.domain.refund.enums;

public enum RefundStatus {

    NONE,
    PENDING,
    PROCESSING,
    SUCCESS,
    FAILED,
    DENIED;

    public boolean refundCanTransitToTargetStatus(RefundStatus targetStatus) {
        if (targetStatus == null) {
            return false;
        }

        return switch (this) {
            case NONE -> targetStatus == PENDING;
            case PENDING ->  targetStatus == PROCESSING || targetStatus == DENIED;
            case PROCESSING -> targetStatus == SUCCESS || targetStatus == FAILED;
            case FAILED -> targetStatus == PENDING || targetStatus == PROCESSING;
            case SUCCESS, DENIED -> false;
        };
    }
}
