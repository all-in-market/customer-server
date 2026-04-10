package com.example.allinmarket.domain.refund.entity;

import com.example.allinmarket.buyer.entity.Buyer;
import com.example.allinmarket.common.entity.BaseEntity;
import com.example.allinmarket.domain.payment.entity.Payment;
import com.example.allinmarket.domain.refund.enums.ReasonEnum;
import com.example.allinmarket.domain.refund.enums.RefundStatus;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Getter
@Entity
@Table(name = "refunds")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Refund extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "buyer_id", nullable = false)
    private Buyer buyer;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "payment_id", nullable = false)
    private Payment payment;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private ReasonEnum reason;

    @NotBlank
    @Column(nullable = false)
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private RefundStatus status;

    @Column(name = "processed_at", nullable = false)
    private LocalDateTime processedAt;

    public static Refund of(Buyer buyer, Payment payment, ReasonEnum reasonEnum, String description) {
        Refund refund = new Refund();
        refund.buyer = buyer;
        refund.payment = payment;
        refund.reason = reasonEnum;
        refund.description = description;
        refund.status = RefundStatus.REQUESTED;
        refund.processedAt = null;
        return refund;
    }
}
