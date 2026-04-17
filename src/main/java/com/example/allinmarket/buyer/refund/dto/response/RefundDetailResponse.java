package com.example.allinmarket.buyer.refund.dto.response;

import com.example.allinmarket.domain.refund.entity.Refund;
import com.example.allinmarket.domain.refund.enums.ReasonEnum;
import com.example.allinmarket.domain.refund.enums.RefundStatus;
import com.example.allinmarket.domain.transactionhistory.enums.TransactionStatus;

public record RefundDetailResponse(
        Long refundId,
        Long buyerId,
        Long paymentId,
        ReasonEnum reason,
        RefundStatus status
) {
    public static RefundDetailResponse from(Refund refund) {
        return new RefundDetailResponse(
                refund.getId(),
                refund.getBuyer().getId(),
                refund.getPayment().getId(),
                refund.getReason(),
                refund.getStatus()
        );
    }
}
