package com.example.allinmarket.buyer.payment.dto.response;

import com.example.allinmarket.domain.payment.entity.Payment;
import com.example.allinmarket.domain.payment.enums.MethodEnum;
import com.example.allinmarket.domain.payment.enums.PaymentStatus;
import com.example.allinmarket.domain.payment.repository.PaymentRepository;
import com.example.allinmarket.domain.transactionhistory.enums.TransactionStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record PaymentDetailResponse(
        Long paymentId,
        Long orderId,
        String impUid,
        BigDecimal amount,
        MethodEnum method,
        PaymentStatus status,
        LocalDateTime paidAt
) {
    public static PaymentDetailResponse from(Payment payment) {
        return new PaymentDetailResponse(
                payment.getId(),
                payment.getOrder().getId(),
                payment.getImpUid(),
                payment.getAmount(),
                payment.getMethod(),
                payment.getStatus(),
                payment.getPaidAt()
        );
    }
}
