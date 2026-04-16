package com.example.allinmarket.domain.payment.entity;

import com.example.allinmarket.common.entity.ModifiableEntity;
import com.example.allinmarket.domain.order.entity.Order;
import com.example.allinmarket.domain.payment.enums.MethodEnum;
import com.example.allinmarket.domain.transactionhistory.enums.TransactionStatus;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Entity
@Table(name = "payments")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Payment extends ModifiableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id", nullable = false)
    private Order order;

    @Column(nullable = false, unique = true)
    private String impUid;

    @PositiveOrZero
    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal amount = BigDecimal.ZERO;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private MethodEnum method;

    @NotNull
    @Enumerated(EnumType.STRING)
    private TransactionStatus status = TransactionStatus.PENDING;

    @Column(name = "paid_at")
    private LocalDateTime paidAt;

    @Version
    private Long version;

    public static Payment of(Order order, String impUid, BigDecimal amount, MethodEnum method) {
        Payment payment = new Payment();
        payment.order = order;
        payment.impUid = impUid;
        payment.amount = amount != null ? amount : BigDecimal.ZERO;
        payment.method = method;
        payment.status = TransactionStatus.PENDING;
        payment.paidAt = null;
        return payment;
    }

    public void success(LocalDateTime paidAt) {
        if (this.status.paymentCanTransitToTargetStatus(TransactionStatus.SUCCESS)) {
            this.status = TransactionStatus.SUCCESS;
            this.paidAt = paidAt;
        }
    }

    public void fail() {
        if (this.status.paymentCanTransitToTargetStatus(TransactionStatus.FAILED)) {
            this.status = TransactionStatus.FAILED;
        }
    }

    public void refund() {
        if (this.status.paymentCanTransitToTargetStatus(TransactionStatus.REFUNDED)) {
            this.status = TransactionStatus.REFUNDED;
        }
    }
}
