package com.example.allinmarket.common.outbox.service;

import com.example.allinmarket.common.enums.ErrorEnum;
import com.example.allinmarket.common.exception.BaseException;
import com.example.allinmarket.common.outbox.entity.DashboardOutbox;
import com.example.allinmarket.common.outbox.payload.DashboardUpdatePayload;
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
    private static final int MAX_RETRY = 5;
    private final DashboardService dashboardService;
    private final ObjectMapper objectMapper;
    private final DashboardOutboxStatusService dashboardOutboxStatusService;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void processSingleEvent(DashboardOutbox dashboardOutbox) throws JsonProcessingException {
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

            // 대시보드 업데이트 성공 시 processed = true 설정
            // 추후 status 값으로 변경 할지는 판단 필요함
            dashboardOutboxStatusService.markProcessed(dashboardOutbox);

        } catch (Exception e) {
            log.error("Outbox 처리 실패 eventId = {}", dashboardOutbox.getId(), e);

            int retryCount = dashboardOutboxStatusService.increaseRetry(dashboardOutbox);

            if (retryCount >= MAX_RETRY) {
                log.error("Outbox 재시도 횟수 초과 eventId = {}", dashboardOutbox.getId());

                dashboardOutboxStatusService.markProcessed(dashboardOutbox);

                return;
            }

            throw e;
        }
    }
}