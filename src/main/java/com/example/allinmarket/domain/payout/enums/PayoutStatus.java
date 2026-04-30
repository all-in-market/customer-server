package com.example.allinmarket.domain.payout.enums;

public enum PayoutStatus {
    PENDING,
    PROCESSING,
    SUCCESS,
    FAILED;

    public boolean payoutCanTransitToTargetStatus(PayoutStatus targetStatus) {
        if (targetStatus == null) {
            return false;
        }

        return switch (this) {
            case PENDING -> targetStatus == PROCESSING || targetStatus == FAILED;
            case PROCESSING -> targetStatus == SUCCESS || targetStatus == FAILED;
            case SUCCESS, FAILED -> false;
        };
    }
}
