package com.example.allinmarket.domain.settlement.dto.response;

import com.example.allinmarket.domain.settlement.entity.Settlement;
import com.example.allinmarket.domain.settlement.enums.SettlementStatus;
import com.example.allinmarket.domain.settlement.enums.SettlementType;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

public record SettlementDetailResponse(
    Long id,
    Long sellerId,
    BigDecimal amount,
    BigDecimal fee,
    SettlementStatus status,
    SettlementType type,
    LocalDate periodStart,
    LocalDate periodEnd,
    LocalDateTime completedAt
) {
    public static SettlementDetailResponse from(Settlement settlement) {
        return new SettlementDetailResponse(
                settlement.getId(),
                settlement.getSeller().getId(),
                settlement.getAmount(),
                settlement.getFee(),
                settlement.getStatus(),
                settlement.getType(),
                settlement.getPeriodStart(),
                settlement.getPeriodEnd(),
                settlement.getCompletedAt()
        );
    }
}
