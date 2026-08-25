package com.example.allinmarket.common.scheduler;

import com.example.allinmarket.buyer.order.service.StockReleaseService;
import com.example.allinmarket.domain.order.entity.Order;
import com.example.allinmarket.domain.order.enums.OrderStatus;
import com.example.allinmarket.domain.order.repository.OrderRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Pageable;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

@ExtendWith(MockitoExtension.class)
class OrderExpirySchedulerTest {

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private StockReleaseService stockReleaseService;

    @InjectMocks
    private OrderExpiryScheduler orderExpiryScheduler;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(orderExpiryScheduler, "chunkSize", 100);
        ReflectionTestUtils.setField(orderExpiryScheduler, "maxLoops", 10);
    }

    @Nested
    @DisplayName("조회 결과가 없는 경우")
    class EmptyResult {

        @Test
        @DisplayName("만료 대상이 없으면 즉시 반환하고 재고 복구 서비스와 상호작용하지 않는다")
        void expireUnpaidOrders_emptyResult_returnsImmediately() {
            // given
            ReflectionTestUtils.setField(orderExpiryScheduler, "chunkSize", 2);
            given(orderRepository.findByStatusAndCreatedAtBefore(eq(OrderStatus.CREATED), any(LocalDateTime.class), any(Pageable.class)))
                    .willReturn(List.of());

            // when
            orderExpiryScheduler.expireUnpaidOrders();

            // then
            verify(orderRepository, times(1)).findByStatusAndCreatedAtBefore(any(), any(), any());
            verifyNoInteractions(stockReleaseService);
        }
    }

    @Nested
    @DisplayName("한 청크로 끝나는 경우")
    class SingleSmallerChunk {

        @Test
        @DisplayName("결과 크기가 chunkSize보다 작으면 한 번만 조회하고 종료한다")
        void expireUnpaidOrders_singleSmallerChunk_queriesOnce() {
            // given
            ReflectionTestUtils.setField(orderExpiryScheduler, "chunkSize", 5);
            Order order1 = mockOrder(1L);
            Order order2 = mockOrder(2L);
            Order order3 = mockOrder(3L);
            given(orderRepository.findByStatusAndCreatedAtBefore(eq(OrderStatus.CREATED), any(LocalDateTime.class), any(Pageable.class)))
                    .willReturn(List.of(order1, order2, order3));

            // when
            orderExpiryScheduler.expireUnpaidOrders();

            // then
            verify(orderRepository, times(1)).findByStatusAndCreatedAtBefore(any(), any(), any());
            verify(stockReleaseService).releaseStockAndFailOrder(1L);
            verify(stockReleaseService).releaseStockAndFailOrder(2L);
            verify(stockReleaseService).releaseStockAndFailOrder(3L);
        }
    }

    @Nested
    @DisplayName("무한 루프 회귀 방지")
    class InfiniteLoopRegression {

        @Test
        @DisplayName("모든 건이 실패해 진행이 없으면 재조회 없이 한 번만 조회하고 종료한다")
        void expireUnpaidOrders_allReleasesFail_stopsAfterFirstQuery() {
            // given
            ReflectionTestUtils.setField(orderExpiryScheduler, "chunkSize", 2);
            Order order1 = mockOrder(1L);
            Order order2 = mockOrder(2L);
            given(orderRepository.findByStatusAndCreatedAtBefore(eq(OrderStatus.CREATED), any(LocalDateTime.class), any(Pageable.class)))
                    .willReturn(List.of(order1, order2));
            willThrow(new RuntimeException("release failed"))
                    .given(stockReleaseService).releaseStockAndFailOrder(anyLong());

            // when
            assertThatCode(() -> orderExpiryScheduler.expireUnpaidOrders())
                    .doesNotThrowAnyException();

            // then: 예전 코드는 여기서 영원히 재조회를 반복했다.
            verify(orderRepository, times(1)).findByStatusAndCreatedAtBefore(any(), any(), any());
            verify(stockReleaseService, times(2)).releaseStockAndFailOrder(anyLong());
        }

        @Test
        @DisplayName("모든 청크가 가득 차고 매번 성공해도 조회 횟수는 maxLoops를 넘지 않는다")
        void expireUnpaidOrders_maxLoopsReached_stopsQueryingAfterLimit() {
            // given
            ReflectionTestUtils.setField(orderExpiryScheduler, "chunkSize", 2);
            ReflectionTestUtils.setField(orderExpiryScheduler, "maxLoops", 3);
            Order order1 = mockOrder(1L);
            Order order2 = mockOrder(2L);
            given(orderRepository.findByStatusAndCreatedAtBefore(eq(OrderStatus.CREATED), any(LocalDateTime.class), any(Pageable.class)))
                    .willReturn(List.of(order1, order2));

            // when
            orderExpiryScheduler.expireUnpaidOrders();

            // then
            verify(orderRepository, times(3)).findByStatusAndCreatedAtBefore(any(), any(), any());
        }
    }

    @Nested
    @DisplayName("건별 예외 격리")
    class PerOrderFailureIsolation {

        @Test
        @DisplayName("한 건이 실패해도 나머지 건은 계속 처리된다")
        void expireUnpaidOrders_partialFailureInChunk_processesRemainingOrders() {
            // given
            ReflectionTestUtils.setField(orderExpiryScheduler, "chunkSize", 5);
            Order order1 = mockOrder(1L);
            Order order2 = mockOrder(2L);
            Order order3 = mockOrder(3L);
            given(orderRepository.findByStatusAndCreatedAtBefore(eq(OrderStatus.CREATED), any(LocalDateTime.class), any(Pageable.class)))
                    .willReturn(List.of(order1, order2, order3));
            willThrow(new RuntimeException("release failed"))
                    .given(stockReleaseService).releaseStockAndFailOrder(2L);

            // when
            assertThatCode(() -> orderExpiryScheduler.expireUnpaidOrders())
                    .doesNotThrowAnyException();

            // then
            verify(stockReleaseService).releaseStockAndFailOrder(1L);
            verify(stockReleaseService).releaseStockAndFailOrder(2L);
            verify(stockReleaseService).releaseStockAndFailOrder(3L);
            verify(orderRepository, times(1)).findByStatusAndCreatedAtBefore(any(), any(), any());
        }
    }

    private Order mockOrder(Long id) {
        Order order = mock(Order.class);
        given(order.getId()).willReturn(id);
        return order;
    }
}
