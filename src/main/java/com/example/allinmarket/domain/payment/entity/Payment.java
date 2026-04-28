package com.example.allinmarket.domain.payment.entity;

import com.example.allinmarket.common.entity.ModifiableEntity;
import com.example.allinmarket.domain.order.entity.Order;
import com.example.allinmarket.domain.payment.enums.MethodEnum;
import com.example.allinmarket.domain.payment.enums.PaymentStatus;
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
    private String merchantUid;

    @Column
    private String impUid;

    @PositiveOrZero
    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal amount = BigDecimal.ZERO;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private MethodEnum method;

    @NotNull
    @Enumerated(EnumType.STRING)
    private PaymentStatus status = PaymentStatus.PENDING;

    @Column(name = "paid_at")
    private LocalDateTime paidAt;

    @Version
    private Long version;

    public static Payment of(Order order, String merchantUid, BigDecimal amount, MethodEnum method) {
        Payment payment = new Payment();
        payment.order = order;
        payment.merchantUid = merchantUid;
        payment.amount = amount != null ? amount : BigDecimal.ZERO;
        payment.method = method;
        payment.status = PaymentStatus.PENDING;
        payment.paidAt = null;
        return payment;
    }

    public void success(LocalDateTime paidAt) {
        if (this.status.paymentCanTransitToTargetStatus(PaymentStatus.SUCCESS)) {
            this.status = PaymentStatus.SUCCESS;
            this.paidAt = paidAt;
        }
    }

    public void fail() {
        if (this.status.paymentCanTransitToTargetStatus(PaymentStatus.FAILED)) {
            this.status = PaymentStatus.FAILED;
        }
    }

    public void refund() {
        if (this.status.paymentCanTransitToTargetStatus(PaymentStatus.REFUNDED)) {
            this.status = PaymentStatus.REFUNDED;
        }
    }
}
