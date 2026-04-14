package com.example.allinmarket.domain.refund.entity;

import com.example.allinmarket.buyer.entity.Buyer;
import com.example.allinmarket.common.entity.ModifiableEntity;
import com.example.allinmarket.domain.payment.entity.Payment;
import com.example.allinmarket.domain.refund.enums.ReasonEnum;
import com.example.allinmarket.domain.transactionhistory.enums.TransactionStatus;
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
public class Refund extends ModifiableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "buyer_id", nullable = false)
    private Buyer buyer;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "payment_id", nullable = false, unique = true)
    private Payment payment;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private ReasonEnum reason;

    private String description;

    @Column(name = "denied_reason")
    private String deniedReason;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private TransactionStatus status = TransactionStatus.PENDING;

    @Column(name = "processed_at")
    private LocalDateTime processedAt;

    public static Refund of(Buyer buyer, Payment payment, ReasonEnum reasonEnum, String description) {
        Refund refund = new Refund();
        refund.buyer = buyer;
        refund.payment = payment;
        refund.reason = reasonEnum;
        refund.description = description;
        refund.deniedReason = null;
        refund.status = TransactionStatus.PENDING;
        refund.processedAt = null;
        return refund;
    }

    public void updateReason(ReasonEnum reason) {
        this.reason = reason;
    }

    public void updateDescription(String description) {
        this.description = description;
    }

    public void success() {
        if(this.status.refundCanTransitToTargetStatus(TransactionStatus.SUCCESS)) {
            this.status = TransactionStatus.SUCCESS;
            this.processedAt = LocalDateTime.now();
        }
    }

    public void pending() {
        if(this.status.refundCanTransitToTargetStatus(TransactionStatus.PENDING)) {
            this.status = TransactionStatus.PENDING;
        }
    }

    public void fail() {
        if(this.status.refundCanTransitToTargetStatus(TransactionStatus.FAILED)) {
            this.status = TransactionStatus.FAILED;
        }
    }

    public void denied() {
        if(this.status.refundCanTransitToTargetStatus(TransactionStatus.DENIED)) {
            this.status = TransactionStatus.DENIED;
        }
    }
}
