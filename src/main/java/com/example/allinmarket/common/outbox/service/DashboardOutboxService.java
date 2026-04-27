package com.example.allinmarket.common.outbox.service;

import com.example.allinmarket.common.outbox.entity.DashboardOutbox;
import com.example.allinmarket.common.outbox.enums.OutboxEventType;
import com.example.allinmarket.common.outbox.payload.DashboardUpdatePayload;
import com.example.allinmarket.common.outbox.repository.DashboardOutboxRepository;
import com.example.allinmarket.domain.sellerdashboard.service.DashboardService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;


@Slf4j
@Service
@RequiredArgsConstructor
public class DashboardOutboxService {
    private final DashboardOutboxRepository dashboardOutboxRepository;
    private final DashboardService dashboardService;
    private final ObjectMapper objectMapper;
    private static final int MAX_RETRY = 5;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void processSingleEvent(DashboardOutbox dashboardOutbox) throws JsonProcessingException {
        try {
            if (OutboxEventType.DASHBOARD_UPDATE.equals(dashboardOutbox.getEventType())) {
                DashboardUpdatePayload dashboardUpdatePayload = objectMapper.readValue(
                        dashboardOutbox.getPayload(),
                        DashboardUpdatePayload.class
                );

                dashboardService.updateSellerDashboard(
                        dashboardUpdatePayload.orderId(),
                        dashboardUpdatePayload.statDate()
                );
            }

            dashboardOutbox.markProcessed();

            dashboardOutboxRepository.saveAndFlush(dashboardOutbox);

        } catch (Exception e) {
            log.error("Outbox 처리 실패 eventId = {}", dashboardOutbox.getId(), e);

            dashboardOutbox.increaseRetryCount();

            if (dashboardOutbox.getRetryCount() >= MAX_RETRY) {
                log.error("Outbox 재시도 횟수 초과 eventId = {}", dashboardOutbox.getId());

                dashboardOutbox.markProcessed();

                dashboardOutboxRepository.saveAndFlush(dashboardOutbox);

                return;
            }

            dashboardOutboxRepository.saveAndFlush(dashboardOutbox);

            throw e;
        }
    }
}