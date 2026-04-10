package com.example.allinmarket.domain.transactionhistory.entity;

import com.example.allinmarket.common.entity.CreatableEntity;
import com.example.allinmarket.domain.payment.entity.Payment;
import com.example.allinmarket.domain.refund.entity.Refund;
import com.example.allinmarket.domain.transactionhistory.enums.TransactionStatus;
import com.example.allinmarket.domain.transactionhistory.enums.TransactionType;
import jakarta.persistence.*;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Entity
@Table(name = "transaction_histories")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Getter
public class TransactionHistory extends CreatableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long transactionId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TransactionType type;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TransactionStatus status;

    @PositiveOrZero
    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal amount;

    public static TransactionHistory of(Payment payment) {
        TransactionHistory transactionHistory = new TransactionHistory();

        transactionHistory.transactionId = payment.getId();
        transactionHistory.type = TransactionType.PAYMENT;
        transactionHistory.status = payment.getStatus();
        transactionHistory.amount = payment.getAmount();

        return transactionHistory;
    }

    public static TransactionHistory of(Refund refund) {
        TransactionHistory transactionHistory = new TransactionHistory();

        transactionHistory.transactionId = refund.getId();
        transactionHistory.type = TransactionType.REFUND;
        transactionHistory.status = refund.getStatus();
        transactionHistory.amount = refund.getPayment().getAmount();

        return transactionHistory;
    }
}
