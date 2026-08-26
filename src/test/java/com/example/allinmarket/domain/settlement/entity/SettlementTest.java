package com.example.allinmarket.domain.settlement.entity;

import com.example.allinmarket.common.enums.ErrorEnum;
import com.example.allinmarket.common.exception.BaseException;
import com.example.allinmarket.domain.settlement.enums.SettlementStatus;
import com.example.allinmarket.domain.settlement.enums.SettlementType;
import com.example.allinmarket.seller.entity.Seller;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;

class SettlementTest {

    @Nested
    @DisplayName("정산 생성")
    class OfTest {

        @Test
        @DisplayName("금액과 수수료가 null이면 0으로 초기화된다")
        void of_whenAmountAndFeeAreNull_thenDefaultToZero() {
            // given
            Seller seller = mock(Seller.class);

            // when
            Settlement settlement = Settlement.of(
                    seller, null, null, SettlementStatus.COMPLETED, SettlementType.MID,
                    LocalDate.of(2026, 1, 1), LocalDate.of(2026, 1, 31), null
            );

            // then
            assertThat(settlement.getAmount()).isEqualByComparingTo(BigDecimal.ZERO);
            assertThat(settlement.getFee()).isEqualByComparingTo(BigDecimal.ZERO);
        }

        @Test
        @DisplayName("전달된 값 그대로 필드에 매핑된다")
        void of_whenValuesProvided_thenMapFieldsAsIs() {
            // given
            Seller seller = mock(Seller.class);
            LocalDate start = LocalDate.of(2026, 1, 1);
            LocalDate end = LocalDate.of(2026, 1, 31);

            // when
            Settlement settlement = Settlement.of(
                    seller, BigDecimal.valueOf(1000), BigDecimal.valueOf(50),
                    SettlementStatus.COMPLETED, SettlementType.END, start, end, null
            );

            // then
            assertThat(settlement.getSeller()).isEqualTo(seller);
            assertThat(settlement.getAmount()).isEqualByComparingTo(BigDecimal.valueOf(1000));
            assertThat(settlement.getFee()).isEqualByComparingTo(BigDecimal.valueOf(50));
            assertThat(settlement.getStatus()).isEqualTo(SettlementStatus.COMPLETED);
            assertThat(settlement.getType()).isEqualTo(SettlementType.END);
            assertThat(settlement.getPeriodStart()).isEqualTo(start);
            assertThat(settlement.getPeriodEnd()).isEqualTo(end);
        }
    }

    @Nested
    @DisplayName("지급 준비 처리")
    class MarkPayoutReadyTest {

        @Test
        @DisplayName("완료 상태이면 지급 준비 상태로 전이된다")
        void markPayoutReady_whenCompleted_thenTransitToPayoutReady() {
            // given
            Settlement settlement = createSettlement(SettlementStatus.COMPLETED);

            // when
            settlement.markPayoutReady();

            // then
            assertThat(settlement.getStatus()).isEqualTo(SettlementStatus.PAYOUT_READY);
        }

        @Test
        @DisplayName("완료 상태가 아니면 예외가 발생한다")
        void markPayoutReady_whenNotCompleted_thenThrowNotCompleted() {
            // given
            Settlement settlement = createSettlement(SettlementStatus.FAILED);

            // when & then
            assertThatThrownBy(settlement::markPayoutReady)
                    .isInstanceOf(BaseException.class)
                    .extracting("errorEnum")
                    .isEqualTo(ErrorEnum.SETTLEMENT_NOT_COMPLETED);

            assertThat(settlement.getStatus()).isEqualTo(SettlementStatus.FAILED);
        }
    }

    @Nested
    @DisplayName("지급 완료 처리")
    class MarkPayoutDoneTest {

        @Test
        @DisplayName("지급 준비 상태이면 지급 완료 상태로 전이된다")
        void markPayoutDone_whenPayoutReady_thenTransitToPayoutDone() {
            // given
            Settlement settlement = createSettlement(SettlementStatus.PAYOUT_READY);

            // when
            settlement.markPayoutDone();

            // then
            assertThat(settlement.getStatus()).isEqualTo(SettlementStatus.PAYOUT_DONE);
        }

        @Test
        @DisplayName("이미 지급 완료 상태이면 예외 없이 그대로 유지된다")
        void markPayoutDone_whenAlreadyPayoutDone_thenNoOp() {
            // given
            Settlement settlement = createSettlement(SettlementStatus.PAYOUT_DONE);

            // when
            settlement.markPayoutDone();

            // then
            assertThat(settlement.getStatus()).isEqualTo(SettlementStatus.PAYOUT_DONE);
        }

        @Test
        @DisplayName("지급 준비 상태가 아니면 예외가 발생한다")
        void markPayoutDone_whenNotPayoutReady_thenThrowNotPayoutReady() {
            // given
            Settlement settlement = createSettlement(SettlementStatus.COMPLETED);

            // when & then
            assertThatThrownBy(settlement::markPayoutDone)
                    .isInstanceOf(BaseException.class)
                    .extracting("errorEnum")
                    .isEqualTo(ErrorEnum.SETTLEMENT_NOT_PAYOUT_READY);

            assertThat(settlement.getStatus()).isEqualTo(SettlementStatus.COMPLETED);
        }
    }

    private Settlement createSettlement(SettlementStatus status) {
        Seller seller = mock(Seller.class);
        Settlement settlement = Settlement.of(
                seller, BigDecimal.valueOf(1000), BigDecimal.valueOf(50),
                status, SettlementType.MID,
                LocalDate.of(2026, 1, 1), LocalDate.of(2026, 1, 31), null
        );
        ReflectionTestUtils.setField(settlement, "status", status);
        return settlement;
    }
}
