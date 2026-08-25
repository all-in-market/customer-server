package com.example.allinmarket.domain.sellerdashboard.entity;

import com.example.allinmarket.seller.entity.Seller;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

class SellerDashboardTest {

    @Nested
    @DisplayName("정산 금액 계산")
    class OfTest {

        @Test
        @DisplayName("환불이 없으면 수수료와 정산액이 매출 기준으로 계산된다")
        void of_whenNoRefund_thenCalculateFeeAndSettlementFromTotalSales() {
            // given
            Seller seller = mock(Seller.class);
            LocalDate statDate = LocalDate.of(2026, 1, 1);
            BigDecimal totalSales = BigDecimal.valueOf(1000);
            BigDecimal refundAmount = BigDecimal.ZERO;

            // when
            SellerDashboard dashboard = SellerDashboard.of(
                    seller, statDate, 10, 20, 0, totalSales, refundAmount
            );

            // then
            assertThat(dashboard.getFeeAmount()).isEqualByComparingTo(BigDecimal.valueOf(50));
            assertThat(dashboard.getSettlementAmount()).isEqualByComparingTo(BigDecimal.valueOf(950));
        }

        @Test
        @DisplayName("환불액이 매출액과 같으면 수수료와 정산액은 0이다")
        void of_whenRefundEqualsTotalSales_thenFeeAndSettlementAreZero() {
            // given
            Seller seller = mock(Seller.class);
            BigDecimal totalSales = BigDecimal.valueOf(1000);
            BigDecimal refundAmount = BigDecimal.valueOf(1000);

            // when
            SellerDashboard dashboard = SellerDashboard.of(
                    seller, null, 5, 5, 1, totalSales, refundAmount
            );

            // then
            assertThat(dashboard.getFeeAmount()).isEqualByComparingTo(BigDecimal.ZERO);
            assertThat(dashboard.getSettlementAmount()).isEqualByComparingTo(BigDecimal.ZERO);
        }

        @Test
        @DisplayName("환불액이 매출액을 초과하면 수수료와 정산액이 음수로 계산된다")
        void of_whenRefundExceedsTotalSales_thenFeeAndSettlementAreNegative() {
            // given
            Seller seller = mock(Seller.class);
            BigDecimal totalSales = BigDecimal.valueOf(1000);
            BigDecimal refundAmount = BigDecimal.valueOf(1500);

            // when
            SellerDashboard dashboard = SellerDashboard.of(
                    seller, null, 1, 1, 2, totalSales, refundAmount
            );

            // then
            assertThat(dashboard.getFeeAmount()).isEqualByComparingTo(BigDecimal.valueOf(-25));
            assertThat(dashboard.getSettlementAmount()).isEqualByComparingTo(BigDecimal.valueOf(-475));
        }

        @Test
        @DisplayName("매출이 0원이면 수수료와 정산액도 0이다")
        void of_whenTotalSalesIsZero_thenFeeAndSettlementAreZero() {
            // given
            Seller seller = mock(Seller.class);

            // when
            SellerDashboard dashboard = SellerDashboard.of(
                    seller, null, 0, 0, 0, BigDecimal.ZERO, BigDecimal.ZERO
            );

            // then
            assertThat(dashboard.getFeeAmount()).isEqualByComparingTo(BigDecimal.ZERO);
            assertThat(dashboard.getSettlementAmount()).isEqualByComparingTo(BigDecimal.ZERO);
        }

        @Test
        @DisplayName("statDate가 null이면 오늘 날짜로 초기화된다")
        void of_whenStatDateIsNull_thenUseToday() {
            // given
            Seller seller = mock(Seller.class);

            // when
            SellerDashboard dashboard = SellerDashboard.of(
                    seller, null, 0, 0, 0, null, null
            );

            // then
            assertThat(dashboard.getStatDate()).isEqualTo(LocalDate.now());
            assertThat(dashboard.getTotalSales()).isEqualByComparingTo(BigDecimal.ZERO);
            assertThat(dashboard.getRefundAmount()).isEqualByComparingTo(BigDecimal.ZERO);
        }
    }

    @Nested
    @DisplayName("대시보드 초기화")
    class ResetTest {

        @Test
        @DisplayName("reset을 호출하면 주문수, 판매수량, 금액이 모두 0으로 초기화된다")
        void reset_whenCalled_thenClearAllAccumulatedFields() {
            // given
            Seller seller = mock(Seller.class);
            SellerDashboard dashboard = SellerDashboard.of(
                    seller,
                    LocalDate.of(2026, 1, 1),
                    10,
                    20,
                    3,
                    BigDecimal.valueOf(1000),
                    BigDecimal.valueOf(100)
            );

            // when
            dashboard.reset();

            // then
            assertThat(dashboard.getTotalOrders()).isZero();
            assertThat(dashboard.getTotalProductsSold()).isZero();
            assertThat(dashboard.getTotalRefunds()).isZero();
            assertThat(dashboard.getTotalSales()).isEqualByComparingTo(BigDecimal.ZERO);
            assertThat(dashboard.getRefundAmount()).isEqualByComparingTo(BigDecimal.ZERO);
            assertThat(dashboard.getFeeAmount()).isEqualByComparingTo(BigDecimal.ZERO);
            assertThat(dashboard.getSettlementAmount()).isEqualByComparingTo(BigDecimal.ZERO);
            assertThat(dashboard.getStatDate()).isEqualTo(LocalDate.now());
        }
    }
}
