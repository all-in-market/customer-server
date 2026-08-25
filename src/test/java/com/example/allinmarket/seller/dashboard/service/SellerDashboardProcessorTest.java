package com.example.allinmarket.seller.dashboard.service;

import com.example.allinmarket.common.enums.ErrorEnum;
import com.example.allinmarket.common.exception.BaseException;
import com.example.allinmarket.domain.order.entity.Order;
import com.example.allinmarket.domain.orderitem.entity.OrderItem;
import com.example.allinmarket.domain.sellerdashboard.repository.SellerDashboardRepository;
import com.example.allinmarket.seller.entity.Seller;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static com.example.allinmarket.seller.consts.SellerConsts.COMMISSION_RATE;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

@ExtendWith(MockitoExtension.class)
class SellerDashboardProcessorTest {

    @Mock
    private SellerDashboardRepository sellerDashboardRepository;

    @Mock
    private DashboardRowCreatorService dashboardRowCreatorService;

    @Mock
    private RedisTemplate<String, Object> redisTemplate;

    @InjectMocks
    private SellerDashboardProcessor sellerDashboardProcessor;

    @Nested
    @DisplayName("판매자 단위 대시보드 집계")
    class ProcessSingleSeller {

        @Test
        @DisplayName("대시보드 row가 이미 있으면 원자적 UPDATE로 누적하고 캐시를 무효화한다")
        void processSingleSeller_rowExists_updatesAndInvalidatesCache() {
            // given
            Long orderId = 1L;
            Seller seller = createSeller(10L);
            LocalDate statDate = LocalDate.of(2026, 8, 25);
            List<OrderItem> items = List.of(
                    createOrderItem(seller, new BigDecimal("10000"), 2),
                    createOrderItem(seller, new BigDecimal("5000"), 1)
            );
            BigDecimal expectedSales = new BigDecimal("25000");
            String expectedKey = "dashboard:10:2026-08-25";

            given(sellerDashboardRepository.addOrder(eq(10L), eq(expectedSales), eq(3), eq(COMMISSION_RATE), eq(statDate)))
                    .willReturn(1);

            // when
            sellerDashboardProcessor.processSingleSeller(seller, items, statDate, orderId);

            // then
            verify(sellerDashboardRepository, times(1))
                    .addOrder(eq(10L), eq(expectedSales), eq(3), eq(COMMISSION_RATE), eq(statDate));
            verify(dashboardRowCreatorService, never()).createDashboardIfNotExists(any(), any());
            verify(redisTemplate).delete(expectedKey);
        }

        @Test
        @DisplayName("주문 아이템이 비어있으면 매출 0, 판매 수량 0으로 집계한다")
        void processSingleSeller_emptyItems_aggregatesToZero() {
            // given
            Long orderId = 1L;
            Seller seller = createSeller(10L);
            LocalDate statDate = LocalDate.of(2026, 8, 25);
            List<OrderItem> items = List.of();

            given(sellerDashboardRepository.addOrder(eq(10L), eq(BigDecimal.ZERO), eq(0), eq(COMMISSION_RATE), eq(statDate)))
                    .willReturn(1);

            // when
            sellerDashboardProcessor.processSingleSeller(seller, items, statDate, orderId);

            // then
            verify(sellerDashboardRepository, times(1))
                    .addOrder(eq(10L), eq(BigDecimal.ZERO), eq(0), eq(COMMISSION_RATE), eq(statDate));
            verify(redisTemplate).delete("dashboard:10:2026-08-25");
        }

        @Test
        @DisplayName("대시보드 row가 없으면(updated=0) row를 생성한 뒤 재시도하여 성공한다")
        void processSingleSeller_rowMissing_createsRowThenRetriesSuccessfully() {
            // given
            Long orderId = 1L;
            Seller seller = createSeller(10L);
            LocalDate statDate = LocalDate.of(2026, 8, 25);
            List<OrderItem> items = List.of(createOrderItem(seller, new BigDecimal("10000"), 1));

            given(sellerDashboardRepository.addOrder(eq(10L), eq(new BigDecimal("10000")), eq(1), eq(COMMISSION_RATE), eq(statDate)))
                    .willReturn(0, 1);

            // when
            sellerDashboardProcessor.processSingleSeller(seller, items, statDate, orderId);

            // then
            verify(dashboardRowCreatorService, times(1)).createDashboardIfNotExists(seller, statDate);
            verify(sellerDashboardRepository, times(2))
                    .addOrder(eq(10L), eq(new BigDecimal("10000")), eq(1), eq(COMMISSION_RATE), eq(statDate));
            verify(redisTemplate).delete("dashboard:10:2026-08-25");
        }

        @Test
        @DisplayName("row 생성 후에도 업데이트가 계속 실패하면 DASHBOARD_UPDATE_FAILED 예외를 던지고 캐시를 무효화하지 않는다")
        void processSingleSeller_updateFailsAfterRowCreation_throwsException() {
            // given
            Long orderId = 1L;
            Seller seller = createSeller(10L);
            LocalDate statDate = LocalDate.of(2026, 8, 25);
            List<OrderItem> items = List.of(createOrderItem(seller, new BigDecimal("10000"), 1));

            given(sellerDashboardRepository.addOrder(eq(10L), eq(new BigDecimal("10000")), eq(1), eq(COMMISSION_RATE), eq(statDate)))
                    .willReturn(0, 0);

            // when & then
            assertThatThrownBy(() -> sellerDashboardProcessor.processSingleSeller(seller, items, statDate, orderId))
                    .isInstanceOf(BaseException.class)
                    .extracting("errorEnum")
                    .isEqualTo(ErrorEnum.DASHBOARD_UPDATE_FAILED);

            verify(dashboardRowCreatorService, times(1)).createDashboardIfNotExists(seller, statDate);
            verify(sellerDashboardRepository, times(2))
                    .addOrder(eq(10L), eq(new BigDecimal("10000")), eq(1), eq(COMMISSION_RATE), eq(statDate));
            verifyNoInteractions(redisTemplate);
        }

        @Test
        @DisplayName("addOrder 조회 중 예외가 발생하면 그대로 전파되고 캐시를 무효화하지 않는다")
        void processSingleSeller_repositoryThrows_exceptionPropagates() {
            // given
            Long orderId = 1L;
            Seller seller = createSeller(10L);
            LocalDate statDate = LocalDate.of(2026, 8, 25);
            List<OrderItem> items = List.of(createOrderItem(seller, new BigDecimal("10000"), 1));

            given(sellerDashboardRepository.addOrder(eq(10L), eq(new BigDecimal("10000")), eq(1), eq(COMMISSION_RATE), eq(statDate)))
                    .willThrow(new RuntimeException("DB 연결 실패"));

            // when & then
            assertThatThrownBy(() -> sellerDashboardProcessor.processSingleSeller(seller, items, statDate, orderId))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessage("DB 연결 실패");

            verify(dashboardRowCreatorService, never()).createDashboardIfNotExists(any(), any());
            verifyNoInteractions(redisTemplate);
        }
    }

    private Seller createSeller(Long id) {
        Seller seller = Seller.of(
                "seller" + id + "@test.com",
                "encodedPassword",
                "판매자" + id,
                "010-1234-567" + id,
                "테스트 스토어" + id,
                "123-45-6789" + id,
                "KOOKMIN",
                "123-456789-12-345"
        );
        ReflectionTestUtils.setField(seller, "id", id);
        return seller;
    }

    private OrderItem createOrderItem(Seller seller, BigDecimal unitPrice, int quantity) {
        Order order = mock(Order.class);
        return OrderItem.of(order, mock(), seller, "상품명", unitPrice, quantity);
    }
}
