package com.example.allinmarket.common.scheduler;

import com.example.allinmarket.domain.settlement.enums.SettlementType;
import com.example.allinmarket.seller.settlement.service.SellerSettlementService;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;

@Component
@RequiredArgsConstructor
public class SettlementScheduler {
    private final SellerSettlementService sellerSettlementService;

    @Scheduled(cron = "0 20 0 16 * *") // 매월 16일 00:20 실행
    public void midSettlement() {
        LocalDate now = LocalDate.now();

        LocalDate start = now.withDayOfMonth(1);

        LocalDate end = now.withDayOfMonth(15);

        sellerSettlementService.createSettlement(start, end, SettlementType.MID);
    }

    @Scheduled(cron = "0 20 0 1 * *") // 매월 1일 00:20 실행
    public void endSettlement() {
        LocalDate targetDate = LocalDate.now().minusMonths(1);

        LocalDate start = targetDate.withDayOfMonth(16);

        LocalDate end = targetDate.withDayOfMonth(targetDate.lengthOfMonth());

        sellerSettlementService.createSettlement(start, end, SettlementType.END);
    }
}
