package com.example.allinmarket.common.outbox.dto;

import com.example.allinmarket.domain.payment.entity.Payment;
import com.example.allinmarket.domain.payment.enums.PaymentStatus;
import com.example.allinmarket.domain.refund.entity.Refund;
import com.example.allinmarket.domain.refund.enums.RefundStatus;
import com.example.allinmarket.domain.transactionhistory.enums.TransactionType;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.math.BigDecimal;

public record HistoryOutBoxPayload(
        Long transactionId,
        TransactionType type,
        PaymentStatus paymentStatus,
        RefundStatus refundStatus,
        BigDecimal amount
) {
    @JsonCreator
    public HistoryOutBoxPayload(
            @JsonProperty("transactionId") Long transactionId,
            @JsonProperty("type") TransactionType type,
            @JsonProperty("paymentStatus") PaymentStatus paymentStatus,
            @JsonProperty("refundStatus") RefundStatus refundStatus,
            @JsonProperty("amount") BigDecimal amount
    ) {
        this.transactionId = transactionId;
        this.type = type;
        this.paymentStatus = paymentStatus;
        this.refundStatus = refundStatus;
        this.amount = amount;
    }

    public static HistoryOutBoxPayload from(Payment payment) {
        return new HistoryOutBoxPayload(
                payment.getId(),
                TransactionType.PAYMENT,
                payment.getStatus(),
                RefundStatus.NONE,
                payment.getAmount()
        );
    }

    public static HistoryOutBoxPayload from(Refund refund) {
        return new HistoryOutBoxPayload(
                refund.getId(),
                TransactionType.REFUND,
                refund.getPayment().getStatus(),
                refund.getStatus(),
                refund.getPayment().getAmount()
        );
    }
}
