package com.example.allinmarket.common.outbox.service;

import com.example.allinmarket.common.outbox.entity.DashboardOutbox;
import com.example.allinmarket.common.outbox.repository.DashboardOutboxRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class DashboardOutboxStatusService {
    private final DashboardOutboxRepository dashboardOutboxRepository;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public int increaseRetry(DashboardOutbox dashboardOutbox) {
        dashboardOutbox.increaseRetryCount();

        dashboardOutboxRepository.saveAndFlush(dashboardOutbox);

        return dashboardOutbox.getRetryCount();
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void markProcessed(DashboardOutbox dashboardOutbox) {
        dashboardOutbox.markProcessed();

        dashboardOutboxRepository.saveAndFlush(dashboardOutbox);
    }
}
