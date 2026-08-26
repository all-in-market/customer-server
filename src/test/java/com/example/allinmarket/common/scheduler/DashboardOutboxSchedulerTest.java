package com.example.allinmarket.common.scheduler;

import com.example.allinmarket.common.outbox.repository.DashboardOutboxRepository;
import com.example.allinmarket.common.outbox.service.DashboardOutboxService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Pageable;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

@ExtendWith(MockitoExtension.class)
class DashboardOutboxSchedulerTest {

    @Mock
    private DashboardOutboxRepository dashboardOutboxRepository;

    @Mock
    private DashboardOutboxService dashboardOutboxService;

    @InjectMocks
    private DashboardOutboxScheduler dashboardOutboxScheduler;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(dashboardOutboxScheduler, "chunkSize", 100);
        ReflectionTestUtils.setField(dashboardOutboxScheduler, "maxRetryCount", 5);
    }

    @Nested
    @DisplayName("조회 조건")
    class QueryCondition {

        @Test
        @DisplayName("설정된 chunkSize와 maxRetryCount를 그대로 리포지토리 조회에 전달한다")
        void processOutbox_passesChunkSizeAndMaxRetryCountToRepository() {
            // given
            ReflectionTestUtils.setField(dashboardOutboxScheduler, "chunkSize", 20);
            ReflectionTestUtils.setField(dashboardOutboxScheduler, "maxRetryCount", 3);
            given(dashboardOutboxRepository.findUnprocessedIds(anyInt(), any(Pageable.class)))
                    .willReturn(List.of());

            // when
            dashboardOutboxScheduler.processOutbox();

            // then
            ArgumentCaptor<Integer> maxRetryCaptor = ArgumentCaptor.forClass(Integer.class);
            ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.forClass(Pageable.class);
            verify(dashboardOutboxRepository).findUnprocessedIds(maxRetryCaptor.capture(), pageableCaptor.capture());

            assertThat(maxRetryCaptor.getValue()).isEqualTo(3);
            assertThat(pageableCaptor.getValue().getPageSize()).isEqualTo(20);
            assertThat(pageableCaptor.getValue().getPageNumber()).isZero();
        }

        @Test
        @DisplayName("조회 결과가 비어 있으면 outbox 서비스와 상호작용하지 않는다")
        void processOutbox_emptyResult_doesNotInteractWithService() {
            // given
            given(dashboardOutboxRepository.findUnprocessedIds(anyInt(), any(Pageable.class)))
                    .willReturn(List.of());

            // when
            dashboardOutboxScheduler.processOutbox();

            // then
            verifyNoInteractions(dashboardOutboxService);
        }
    }

    @Nested
    @DisplayName("건별 처리")
    class PerEventProcessing {

        @Test
        @DisplayName("조회된 ID 각각에 대해 processSingleEvent를 한 번씩 호출한다")
        void processOutbox_threeIds_callsProcessSingleEventForEachId() {
            // given
            given(dashboardOutboxRepository.findUnprocessedIds(anyInt(), any(Pageable.class)))
                    .willReturn(List.of(1L, 2L, 3L));

            // when
            dashboardOutboxScheduler.processOutbox();

            // then
            verify(dashboardOutboxService).processSingleEvent(1L);
            verify(dashboardOutboxService).processSingleEvent(2L);
            verify(dashboardOutboxService).processSingleEvent(3L);
            verify(dashboardOutboxService, times(3)).processSingleEvent(anyLong());
        }

        @Test
        @DisplayName("한 이벤트 처리 중 예외가 발생해도 나머지 이벤트는 계속 처리된다")
        void processOutbox_oneEventThrows_othersStillProcessed() {
            // given
            given(dashboardOutboxRepository.findUnprocessedIds(anyInt(), any(Pageable.class)))
                    .willReturn(List.of(1L, 2L, 3L));
            willThrow(new RuntimeException("processing failed"))
                    .given(dashboardOutboxService).processSingleEvent(2L);

            // when
            assertThatCode(() -> dashboardOutboxScheduler.processOutbox())
                    .doesNotThrowAnyException();

            // then
            verify(dashboardOutboxService).processSingleEvent(1L);
            verify(dashboardOutboxService).processSingleEvent(2L);
            verify(dashboardOutboxService).processSingleEvent(3L);
        }
    }
}
