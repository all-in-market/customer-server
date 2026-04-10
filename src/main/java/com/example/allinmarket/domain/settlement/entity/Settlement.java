package com.example.allinmarket.domain.settlement.entity;

import com.example.allinmarket.common.entity.ModifiableEntity;
import com.example.allinmarket.domain.settlement.enums.SettlementStatus;
import com.example.allinmarket.domain.settlement.enums.SettlementType;
import com.example.allinmarket.seller.entity.Seller;
import jakarta.persistence.*;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Getter
@Entity
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "settlements")
public class Settlement extends ModifiableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "seller_id", nullable = false)
    private Seller seller;

    @PositiveOrZero
    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal amount;

    @PositiveOrZero
    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal fee = BigDecimal.ZERO;

    @Enumerated(EnumType.STRING)
    @Column(length = 20, nullable = false)
    private SettlementStatus status = SettlementStatus.COMPLETED;

    @Enumerated(EnumType.STRING)
    @Column(length = 20, nullable = false)
    private SettlementType type = SettlementType.MID;

    @Column(name = "period_start", nullable = false)
    private LocalDate periodStart;

    @Column(name = "period_end", nullable = false)
    private LocalDate periodEnd;

    @Column(name = "completed_at")
    private LocalDateTime completedAt;

    public static Settlement of(
            Seller seller,
            BigDecimal amount,
            BigDecimal fee,
            SettlementStatus status,
            SettlementType type,
            LocalDate periodStart,
            LocalDate periodEnd
    ) {
        Settlement settlement = new Settlement();
        settlement.seller = seller;
        settlement.amount = amount != null ? amount : BigDecimal.ZERO;
        settlement.fee = fee != null ? fee : BigDecimal.ZERO;
        settlement.status = status;
        settlement.type = type;
        settlement.periodStart = periodStart;
        settlement.periodEnd = periodEnd;
        return settlement;
    }
}