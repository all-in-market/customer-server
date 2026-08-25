package com.example.allinmarket.common.outbox.service;

import com.example.allinmarket.common.enums.ErrorEnum;
import com.example.allinmarket.common.exception.BaseException;
import com.example.allinmarket.common.outbox.entity.DashboardOutbox;
import com.example.allinmarket.common.outbox.enums.OutboxEventType;
import com.example.allinmarket.common.outbox.payload.DashboardUpdatePayload;
import com.example.allinmarket.seller.dashboard.service.DashboardService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class DashboardOutboxServiceTest {

    @Mock
    private DashboardService dashboardService;

    @Mock
    private ObjectMapper objectMapper;

    @Mock
    private DashboardOutboxStatusService dashboardOutboxStatusService;

    @InjectMocks
    private DashboardOutboxService dashboardOutboxService;

    @Nested
    @DisplayName("DASHBOARD_UPDATE 이벤트 처리")
    class ProcessDashboardUpdateEvent {

        @Test
        @DisplayName("정상 처리 시 페이로드를 역직렬화하여 대시보드를 갱신하고 처리 완료로 표시한다")
        void processSingleEvent_dashboardUpdate_success() throws JsonProcessingException {
            // given
            Long orderId = 10L;
            LocalDate statDate = LocalDate.of(2026, 8, 25);
            DashboardOutbox outbox = createOutbox(1L, OutboxEventType.DASHBOARD_UPDATE, "{\"orderId\":10,\"statDate\":\"2026-08-25\"}");
            DashboardUpdatePayload payload = new DashboardUpdatePayload(orderId, statDate);

            given(objectMapper.readValue(outbox.getPayload(), DashboardUpdatePayload.class)).willReturn(payload);

            // when
            dashboardOutboxService.processSingleEvent(outbox);

            // then
            verify(dashboardService).updateSellerDashboard(orderId, statDate);
            verify(dashboardOutboxStatusService).markProcessed(outbox);
            verify(dashboardOutboxStatusService, never()).increaseRetry(outbox);
        }

        @Test
        @DisplayName("페이로드 역직렬화 실패 시 재시도 카운트를 증가시키고 재시도 임계값 미만이면 예외를 다시 던진다")
        void processSingleEvent_deserializationFails_belowMaxRetry_rethrows() throws JsonProcessingException {
            // given
            DashboardOutbox outbox = createOutbox(1L, OutboxEventType.DASHBOARD_UPDATE, "invalid-json");

            given(objectMapper.readValue(anyString(), eq(DashboardUpdatePayload.class)))
                    .willThrow(new JsonProcessingException("invalid json") {});
            given(dashboardOutboxStatusService.increaseRetry(outbox)).willReturn(1);

            // when & then
            assertThatThrownBy(() -> dashboardOutboxService.processSingleEvent(outbox))
                    .isInstanceOf(JsonProcessingException.class);

            verify(dashboardOutboxStatusService).increaseRetry(outbox);
            verify(dashboardOutboxStatusService, never()).markProcessed(outbox);
        }
    }

    @Nested
    @DisplayName("알 수 없는 이벤트 타입 처리")
    class ProcessUnknownEventType {

        // 주의: OutboxEventType은 현재 DASHBOARD_UPDATE 단일 값만 정의되어 있어
        // switch문의 default(OUTBOX_EVENT_TYPE_NOT_FOUND) 분기는 유효한 enum 값으로는 도달 불가능하다.
        // eventType이 null인 경우도 default가 아니라 switch 자체에서 NullPointerException이 발생하므로
        // 실제 코드 동작(NPE가 바깥 catch 블록에 걸려 재시도 로직을 태우는 것)을 그대로 검증한다.
        @Test
        @DisplayName("eventType이 null이면 switch에서 NPE가 발생하고 바깥 catch 블록의 재시도 로직으로 이어진다")
        void processSingleEvent_nullEventType_npeIsCaughtByRetryLogic() {
            // given
            DashboardOutbox outbox = org.mockito.Mockito.mock(DashboardOutbox.class);
            given(outbox.getEventType()).willReturn(null);
            given(outbox.getId()).willReturn(1L);
            given(dashboardOutboxStatusService.increaseRetry(outbox)).willReturn(1);

            // when & then
            assertThatThrownBy(() -> dashboardOutboxService.processSingleEvent(outbox))
                    .isInstanceOf(NullPointerException.class);

            verify(dashboardOutboxStatusService).increaseRetry(outbox);
            verify(dashboardOutboxStatusService, never()).markProcessed(outbox);
        }
    }

    @Nested
    @DisplayName("재시도 한계 처리 (MAX_RETRY = 5)")
    class RetryLimitHandling {

        @Test
        @DisplayName("재시도 횟수가 5 미만이면 예외를 다시 던지고 처리 완료로 표시하지 않는다")
        void processSingleEvent_retryBelowMax_throwsAndDoesNotMarkProcessed() throws JsonProcessingException {
            // given
            DashboardOutbox outbox = createOutbox(1L, OutboxEventType.DASHBOARD_UPDATE, "{}");
            given(objectMapper.readValue(anyString(), eq(DashboardUpdatePayload.class)))
                    .willThrow(new RuntimeException("update failed"));
            given(dashboardOutboxStatusService.increaseRetry(outbox)).willReturn(4);

            // when & then
            assertThatThrownBy(() -> dashboardOutboxService.processSingleEvent(outbox))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessage("update failed");

            verify(dashboardOutboxStatusService).increaseRetry(outbox);
            verify(dashboardOutboxStatusService, never()).markProcessed(outbox);
        }

        @Test
        @DisplayName("재시도 횟수가 5 이상이면 처리 완료로 표시하고 예외를 삼킨다")
        void processSingleEvent_retryAtMax_marksProcessedAndSwallowsException() throws JsonProcessingException {
            // given
            DashboardOutbox outbox = createOutbox(1L, OutboxEventType.DASHBOARD_UPDATE, "{}");
            given(objectMapper.readValue(anyString(), eq(DashboardUpdatePayload.class)))
                    .willThrow(new RuntimeException("update failed"));
            given(dashboardOutboxStatusService.increaseRetry(outbox)).willReturn(5);

            // when & then
            assertThatCode(() -> dashboardOutboxService.processSingleEvent(outbox))
                    .doesNotThrowAnyException();

            verify(dashboardOutboxStatusService).increaseRetry(outbox);
            verify(dashboardOutboxStatusService).markProcessed(outbox);
        }

        @Test
        @DisplayName("재시도 횟수가 5를 초과해도 처리 완료로 표시하고 예외를 삼킨다")
        void processSingleEvent_retryAboveMax_marksProcessedAndSwallowsException() throws JsonProcessingException {
            // given
            DashboardOutbox outbox = createOutbox(1L, OutboxEventType.DASHBOARD_UPDATE, "{}");
            given(objectMapper.readValue(anyString(), eq(DashboardUpdatePayload.class)))
                    .willThrow(new RuntimeException("update failed"));
            given(dashboardOutboxStatusService.increaseRetry(outbox)).willReturn(6);

            // when & then
            assertThatCode(() -> dashboardOutboxService.processSingleEvent(outbox))
                    .doesNotThrowAnyException();

            verify(dashboardOutboxStatusService).increaseRetry(outbox);
            verify(dashboardOutboxStatusService).markProcessed(outbox);
        }
    }

    private DashboardOutbox createOutbox(Long id, OutboxEventType eventType, String payload) {
        DashboardOutbox outbox = DashboardOutbox.of(eventType, 100L, payload);
        ReflectionTestUtils.setField(outbox, "id", id);
        return outbox;
    }
}
