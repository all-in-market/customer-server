package com.example.allinmarket.common.outbox.service;

import com.example.allinmarket.common.outbox.entity.DashboardOutbox;
import com.example.allinmarket.common.outbox.repository.DashboardOutboxRepository;
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

    @Scheduled(fixedDelay = 2000)
    @Transactional
    public void processOutbox() {
        Pageable pageable = PageRequest.of(0, 100);
        List<DashboardOutbox> dashboardOutboxes = dashboardOutboxRepository.findTop100ForUpdate(pageable);

        for (DashboardOutbox dashboardOutbox : dashboardOutboxes) {
            try {

                dashboardOutboxService.processSingleEvent(dashboardOutbox);

            } catch (Exception e) {
                log.error("Outbox 처리 실패 eventId = {}", dashboardOutbox.getId(), e);
            }
        }
    }
}
