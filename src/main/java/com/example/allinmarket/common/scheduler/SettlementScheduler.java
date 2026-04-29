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

    @Scheduled(cron = "0 20 0 15 * *") // 매월 15일 00:20 실행
    public void midSettlement() {
        LocalDate now = LocalDate.now();

        LocalDate start = now.withDayOfMonth(1);

        LocalDate end = now.withDayOfMonth(14);

        sellerSettlementService.createSettlement(start, end, SettlementType.MID);
    }

    @Scheduled(cron = "0 20 0 L * *") // 매월 말일 00:20 실행
    public void endSettlement() {
        LocalDate now = LocalDate.now();

        LocalDate start = now.withDayOfMonth(15);

        LocalDate end = now.withDayOfMonth(now.lengthOfMonth());

        sellerSettlementService.createSettlement(start, end, SettlementType.END);
    }
}
