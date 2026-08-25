package com.example.allinmarket.common.outbox.service;

import com.example.allinmarket.common.enums.ErrorEnum;
import com.example.allinmarket.common.exception.BaseException;
import com.example.allinmarket.common.outbox.entity.DashboardOutbox;
import com.example.allinmarket.common.outbox.payload.DashboardUpdatePayload;
import com.example.allinmarket.common.outbox.repository.DashboardOutboxRepository;
import com.example.allinmarket.seller.dashboard.service.DashboardService;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class DashboardOutboxService {

    private final DashboardOutboxRepository dashboardOutboxRepository;
    private final DashboardService dashboardService;
    private final ObjectMapper objectMapper;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void processSingleEvent(Long outboxId) {
        DashboardOutbox dashboardOutbox = dashboardOutboxRepository.findByIdForUpdate(outboxId)
                .orElse(null);

        if (dashboardOutbox == null) {
            log.warn("존재하지 않는 Dashboard Outbox 이벤트 eventId = {}", outboxId);
            return;
        }

        if (dashboardOutbox.isProcessed()) {
            log.debug("이미 처리된 Outbox 이벤트 스킵: eventId={}", outboxId);
            return;
        }

        try {
            switch (dashboardOutbox.getEventType()) {
                case DASHBOARD_UPDATE -> {
                    // 저장 시 문자열로 직렬화 했기 때문에 처리 시 역직렬화 필요
                    DashboardUpdatePayload dashboardUpdatePayload = objectMapper.readValue(
                            dashboardOutbox.getPayload(),
                            DashboardUpdatePayload.class
                    );

                    dashboardService.updateSellerDashboard(
                            dashboardUpdatePayload.orderId(),
                            dashboardUpdatePayload.statDate()
                    );
                }

                default -> {
                    log.error("알 수 없는 Outbox 이벤트 타입 eventType = {}, eventId = {}", dashboardOutbox.getEventType(), dashboardOutbox.getId());

                    throw new BaseException(ErrorEnum.OUTBOX_EVENT_TYPE_NOT_FOUND);
                }
            }

            dashboardOutbox.markProcessed();
            log.info("Outbox 처리 성공 eventId = {}", dashboardOutbox.getId());
        } catch (Exception e) {
            dashboardOutbox.increaseRetryCount();
            log.error("Outbox 처리 실패 eventId = {}, retryCount = {}", dashboardOutbox.getId(), dashboardOutbox.getRetryCount(), e);
        }
    }
}
