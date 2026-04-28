package com.example.allinmarket.common.scheduler;

import com.example.allinmarket.common.outbox.entity.DashboardOutbox;
import com.example.allinmarket.common.outbox.repository.DashboardOutboxRepository;
import com.example.allinmarket.common.outbox.service.DashboardOutboxService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class DashboardOutboxScheduler {
    private final DashboardOutboxRepository dashboardOutboxRepository;
    private final DashboardOutboxService dashboardOutboxService;

    // Outbox Polling 스케줄러
    // 결제 트랜잭션과 대시보드 업데이트를 분리하기 위한 Outbox 패턴 사용
    // 일정 주기로 처리되지 않은 Outbox 이벤트를 조회하여 처리
    @Scheduled(fixedDelay = 2000)
    @Transactional // 비관적 락 적용을 위한 트랜잭션 추가
    public void processOutbox() {
        Pageable pageable = PageRequest.of(0, 100);
        List<DashboardOutbox> dashboardOutboxes = dashboardOutboxRepository.findTop100ForUpdate(pageable);

        for (DashboardOutbox dashboardOutbox : dashboardOutboxes) {
            try {
                // 단건 이벤트 처리 - 내부적으로 REQUIRES_NEW 사용
                dashboardOutboxService.processSingleEvent(dashboardOutbox);

            } catch (Exception e) {
                // 전체 중단 방지 -> 다음 이벤트 계속 처리 가능
                log.error("Outbox 처리 실패 eventId = {}", dashboardOutbox.getId(), e);
            }
        }
    }
}
