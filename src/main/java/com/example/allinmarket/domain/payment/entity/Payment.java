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

    @Column(unique = true)
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

    public static Payment of(Order order, BigDecimal amount, MethodEnum method) {
        Payment payment = new Payment();
        payment.order = order;
        payment.impUid = null;
        payment.amount = amount != null ? amount : BigDecimal.ZERO;
        payment.method = method;
        payment.status = TransactionStatus.PENDING;
        payment.paidAt = null;
        return payment;
    }

    public void complete(String impUid) {
        this.impUid = impUid;
        this.status = TransactionStatus.SUCCESS;
        this.paidAt = LocalDateTime.now();
    }

    public void fail() {
        this.status = TransactionStatus.FAILED;
    }

    public void cancel() {
        this.status = TransactionStatus.REFUNDED;
    }
}
