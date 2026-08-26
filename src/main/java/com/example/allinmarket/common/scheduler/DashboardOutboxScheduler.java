package com.example.allinmarket.common.scheduler;

import com.example.allinmarket.common.outbox.consts.DashboardOutBoxConsts;
import com.example.allinmarket.common.outbox.repository.DashboardOutboxRepository;
import com.example.allinmarket.common.outbox.service.DashboardOutboxService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class DashboardOutboxScheduler {

    private final DashboardOutboxRepository dashboardOutboxRepository;
    private final DashboardOutboxService dashboardOutboxService;

    @Value("${app.scheduling.outbox.chunk-size:100}")
    private int chunkSize;

    @Value("${app.scheduling.outbox.max-retry:" + DashboardOutBoxConsts.MAX_RETRY_COUNT + "}")
    private int maxRetryCount;

    // Outbox Polling 스케줄러
    // 결제 트랜잭션과 대시보드 업데이트를 분리하기 위한 Outbox 패턴 사용
    // 일정 주기로 처리되지 않은 Outbox 이벤트를 조회하여 처리
    @Scheduled(fixedDelay = 60000)
    public void processOutbox() {
        Pageable pageable = PageRequest.of(0, chunkSize);
        List<Long> outboxIds = dashboardOutboxRepository.findUnprocessedIds(maxRetryCount, pageable);

        if (outboxIds.isEmpty()) return;

        log.info("미처리 Dashboard Outbox 이벤트 수: {}", outboxIds.size());

        for (Long outboxId : outboxIds) {
            try {
                // 단건 이벤트 처리 - 내부적으로 REQUIRES_NEW 사용
                dashboardOutboxService.processSingleEvent(outboxId);
            } catch (Exception e) {
                // 전체 중단 방지 -> 다음 이벤트 계속 처리 가능
                log.error("Outbox 처리 실패 eventId = {}", outboxId, e);
            }
        }
    }
}
