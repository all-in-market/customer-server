package com.example.allinmarket.domain.payout.entity;

import com.example.allinmarket.common.entity.CreatableEntity;
import com.example.allinmarket.common.enums.ErrorEnum;
import com.example.allinmarket.common.exception.BaseException;
import com.example.allinmarket.domain.payout.enums.PayoutStatus;
import com.example.allinmarket.seller.entity.Seller;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Entity
@Table(name = "payouts")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Payout extends CreatableEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "seller_id", nullable = false)
    private Seller seller;

    @Column(nullable = false, unique = true)
    private Long settlementId;

    @Column(nullable = false)
    private BigDecimal amount;

    @Column(nullable = false)
    private BigDecimal fee;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private PayoutStatus status;

    @Column(nullable = false, unique = true)
    private String payoutKey;

    @Column(name = "processed_at")
    private LocalDateTime processedAt;

    @Column(nullable = false)
    private int retryCount;

    public static Payout of (Seller seller, Long settlementId, BigDecimal amount, BigDecimal fee, PayoutStatus status, String payoutKey) {
        Payout payout = new Payout();
        payout.seller = seller;
        payout.settlementId = settlementId;
        payout.amount = amount;
        payout.fee = fee;
        payout.status = status;
        payout.payoutKey = payoutKey;

        return payout;
    }

    public void success() {
        if (this.status == PayoutStatus.SUCCESS) {
            throw new BaseException(ErrorEnum.PAYOUT_ALREADY_SUCCESS);
        }

        if (!this.status.payoutCanTransitToTargetStatus(PayoutStatus.SUCCESS)) {
            throw new BaseException(ErrorEnum.PAYOUT_ALREADY_FAILED);
        }

        this.status = PayoutStatus.SUCCESS;
        this.processedAt = LocalDateTime.now();
    }

    public void fail() {
        if (this.status == PayoutStatus.FAILED) {
            throw new BaseException(ErrorEnum.PAYOUT_ALREADY_FAILED);
        }

        if (!this.status.payoutCanTransitToTargetStatus(PayoutStatus.FAILED)) {
            throw new BaseException(ErrorEnum.PAYOUT_ALREADY_SUCCESS);
        }

        this.status = PayoutStatus.FAILED;
        this.processedAt = LocalDateTime.now();
    }

    public void increaseRetryCount() {
        this.retryCount++;
    }

    public void markProcessing() {
        if (!this.status.payoutCanTransitToTargetStatus(PayoutStatus.PROCESSING)) {
            throw new BaseException(ErrorEnum.PAYOUT_PENDING_NOT_FOUND);
        }
        this.status = PayoutStatus.PROCESSING;
    }
}
