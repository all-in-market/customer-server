package com.example.allinmarket.domain.transactionhistory.enums;

public enum TransactionStatus {
    PENDING,
    SUCCESS,
    FAILED,
    DENIED,
    REFUNDED;

    public boolean paymentCanTransitToTargetStatus(TransactionStatus targetStatus) {
        if (targetStatus == null) {
            return false;
        }

        return switch (this) {
            case PENDING -> targetStatus == SUCCESS || targetStatus == FAILED || targetStatus == REFUNDED;
            case SUCCESS, FAILED -> targetStatus == REFUNDED;
            case REFUNDED, DENIED -> false;
        };
    }

//    public boolean refundCanTransitToTargetStatus(TransactionStatus targetStatus) {
//        if (targetStatus == null) {
//            return false;
//        }
//
//        return switch (this) {
//
//        };
//    }
}
