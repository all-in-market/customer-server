package com.example.allinmarket.common.outbox.service;

import com.example.allinmarket.common.outbox.consts.DashboardOutBoxConsts;
import com.example.allinmarket.common.outbox.entity.DashboardOutbox;
import com.example.allinmarket.common.outbox.enums.OutboxEventType;
import com.example.allinmarket.common.outbox.payload.DashboardUpdatePayload;
import com.example.allinmarket.common.outbox.repository.DashboardOutboxRepository;
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
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

@ExtendWith(MockitoExtension.class)
class DashboardOutboxServiceTest {

    @Mock
    private DashboardOutboxRepository dashboardOutboxRepository;

    @Mock
    private DashboardService dashboardService;

    @Mock
    private ObjectMapper objectMapper;

    @InjectMocks
    private DashboardOutboxService dashboardOutboxService;

    @Nested
    @DisplayName("DASHBOARD_UPDATE 이벤트 정상 처리")
    class ProcessDashboardUpdateEvent {

        @Test
        @DisplayName("페이로드를 역직렬화하여 대시보드를 갱신하고 처리 완료로 표시한다")
        void processSingleEvent_dashboardUpdate_success_marksProcessed() throws JsonProcessingException {
            // given
            Long orderId = 10L;
            LocalDate statDate = LocalDate.of(2026, 8, 25);
            DashboardOutbox outbox = createOutbox(1L, OutboxEventType.DASHBOARD_UPDATE, "{\"orderId\":10,\"statDate\":\"2026-08-25\"}");
            DashboardUpdatePayload payload = new DashboardUpdatePayload(orderId, statDate);

            given(dashboardOutboxRepository.findByIdForUpdate(1L)).willReturn(Optional.of(outbox));
            given(objectMapper.readValue(outbox.getPayload(), DashboardUpdatePayload.class)).willReturn(payload);

            // when
            dashboardOutboxService.processSingleEvent(1L);

            // then
            verify(dashboardService).updateSellerDashboard(orderId, statDate);
            assertThat(outbox.isProcessed()).isTrue();
            assertThat(outbox.getRetryCount()).isZero();
        }
    }

    @Nested
    @DisplayName("엔티티를 찾을 수 없는 경우")
    class OutboxNotFound {

        @Test
        @DisplayName("존재하지 않는 outboxId면 예외 없이 조용히 반환하고 대시보드 서비스와 상호작용하지 않는다")
        void processSingleEvent_outboxNotFound_returnsSilently() {
            // given
            given(dashboardOutboxRepository.findByIdForUpdate(999L)).willReturn(Optional.empty());

            // when
            assertThatCode(() -> dashboardOutboxService.processSingleEvent(999L))
                    .doesNotThrowAnyException();

            // then
            verifyNoInteractions(dashboardService);
            verifyNoInteractions(objectMapper);
        }
    }

    @Nested
    @DisplayName("이미 처리된 이벤트")
    class AlreadyProcessed {

        @Test
        @DisplayName("processed가 true면 처리 로직을 건너뛰고 대시보드 서비스와 상호작용하지 않는다")
        void processSingleEvent_alreadyProcessed_skipsProcessing() {
            // given
            DashboardOutbox outbox = createOutbox(1L, OutboxEventType.DASHBOARD_UPDATE, "{}");
            outbox.markProcessed();
            given(dashboardOutboxRepository.findByIdForUpdate(1L)).willReturn(Optional.of(outbox));

            // when
            dashboardOutboxService.processSingleEvent(1L);

            // then
            verifyNoInteractions(dashboardService);
            verifyNoInteractions(objectMapper);
        }
    }

    @Nested
    @DisplayName("처리 중 예외 발생")
    class ProcessingFails {

        @Test
        @DisplayName("대시보드 갱신 실패 시 예외를 삼키고 재시도 횟수만 증가시킨다")
        void processSingleEvent_dashboardUpdateFails_swallowsExceptionAndIncreasesRetry() throws JsonProcessingException {
            // given
            DashboardOutbox outbox = createOutbox(1L, OutboxEventType.DASHBOARD_UPDATE, "{\"orderId\":10,\"statDate\":\"2026-08-25\"}");
            DashboardUpdatePayload payload = new DashboardUpdatePayload(10L, LocalDate.of(2026, 8, 25));

            given(dashboardOutboxRepository.findByIdForUpdate(1L)).willReturn(Optional.of(outbox));
            given(objectMapper.readValue(outbox.getPayload(), DashboardUpdatePayload.class)).willReturn(payload);
            willThrow(new RuntimeException("dashboard update failed"))
                    .given(dashboardService).updateSellerDashboard(anyLong(), org.mockito.ArgumentMatchers.any());

            // when
            assertThatCode(() -> dashboardOutboxService.processSingleEvent(1L))
                    .doesNotThrowAnyException();

            // then
            assertThat(outbox.getRetryCount()).isEqualTo(1);
            assertThat(outbox.isProcessed()).isFalse();
        }

        @Test
        @DisplayName("페이로드 역직렬화 실패 시 예외를 삼키고 재시도 횟수만 증가시킨다")
        void processSingleEvent_payloadDeserializationFails_swallowsExceptionAndIncreasesRetry() throws JsonProcessingException {
            // given
            DashboardOutbox outbox = createOutbox(1L, OutboxEventType.DASHBOARD_UPDATE, "invalid-json");
            given(dashboardOutboxRepository.findByIdForUpdate(1L)).willReturn(Optional.of(outbox));
            given(objectMapper.readValue(anyString(), eq(DashboardUpdatePayload.class)))
                    .willThrow(new JsonProcessingException("invalid json") {});

            // when
            assertThatCode(() -> dashboardOutboxService.processSingleEvent(1L))
                    .doesNotThrowAnyException();

            // then
            assertThat(outbox.getRetryCount()).isEqualTo(1);
            assertThat(outbox.isProcessed()).isFalse();
            verifyNoInteractions(dashboardService);
        }
    }

    @Nested
    @DisplayName("재시도 한계 회귀 방지 (MAX_RETRY_COUNT = 5)")
    class RetryLimitRegression {

        @Test
        @DisplayName("재시도 횟수가 이미 최대치여도 실패 시 처리 완료로 표시하지 않는다")
        void processSingleEvent_atMaxRetryCount_stillFailingDoesNotMarkProcessed() throws JsonProcessingException {
            // given
            DashboardOutbox outbox = createOutbox(1L, OutboxEventType.DASHBOARD_UPDATE, "{}");
            ReflectionTestUtils.setField(outbox, "retryCount", DashboardOutBoxConsts.MAX_RETRY_COUNT);

            given(dashboardOutboxRepository.findByIdForUpdate(1L)).willReturn(Optional.of(outbox));
            given(objectMapper.readValue(anyString(), eq(DashboardUpdatePayload.class)))
                    .willThrow(new RuntimeException("still failing"));

            // when
            assertThatCode(() -> dashboardOutboxService.processSingleEvent(1L))
                    .doesNotThrowAnyException();

            // then: 예전 코드는 여기서 markProcessed()를 호출해 실패 건을 성공으로 위장시켰다.
            assertThat(outbox.isProcessed()).isFalse();
            assertThat(outbox.getRetryCount()).isEqualTo(DashboardOutBoxConsts.MAX_RETRY_COUNT + 1);
        }
    }

    private DashboardOutbox createOutbox(Long id, OutboxEventType eventType, String payload) {
        DashboardOutbox outbox = DashboardOutbox.of(eventType, 100L, payload);
        ReflectionTestUtils.setField(outbox, "id", id);
        return outbox;
    }
}
