package com.example.allinmarket.common.scheduler;

import com.example.allinmarket.seller.payout.service.SellerPayoutService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class PayoutScheduler {
    private final SellerPayoutService sellerPayoutService;

    @Scheduled(cron = "0 0 2 1,16 * *") // 매월 1일, 16일 새벽 2시
    public void runCreatePayout() {
        try {
            sellerPayoutService.createPayout();

            log.info("지급 내역 생성 완료");

        } catch (Exception e) {
            log.error("지급 내역 생성 중 에러 발생: {}", e.getMessage());
        }
    }

    @Scheduled(cron = "0 0 3 1,16 * *") // 매월 1일, 16일 새벽 3시
    public void runProcessPayout() {
        try {
            sellerPayoutService.processPayout();

            log.info("뱅킹 API 지급 실행 완료");

        } catch (Exception e) {
            log.error("지급 실행 중 에러 발생: {}", e.getMessage());
        }
    }
}
