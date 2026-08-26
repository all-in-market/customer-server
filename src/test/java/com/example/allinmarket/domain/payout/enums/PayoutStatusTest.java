package com.example.allinmarket.domain.payout.enums;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.EnumSource;

import static org.assertj.core.api.Assertions.assertThat;

class PayoutStatusTest {

    @Nested
    @DisplayName("정산 상태 전이 매트릭스")
    class PayoutCanTransitToTargetStatusTest {

        @ParameterizedTest(name = "{0} → {1} 은 {2}")
        @DisplayName("코드에 정의된 전이 규칙을 그대로 검증한다")
        @CsvSource({
                "PENDING,    PENDING,    false",
                "PENDING,    PROCESSING, true",
                "PENDING,    SUCCESS,    false",
                "PENDING,    FAILED,     true",

                "PROCESSING, PENDING,    false",
                "PROCESSING, PROCESSING, false",
                "PROCESSING, SUCCESS,    true",
                "PROCESSING, FAILED,     true",

                "SUCCESS,    PENDING,    false",
                "SUCCESS,    PROCESSING, false",
                "SUCCESS,    SUCCESS,    false",
                "SUCCESS,    FAILED,     false",

                "FAILED,     PENDING,    false",
                "FAILED,     PROCESSING, false",
                "FAILED,     SUCCESS,    false",
                "FAILED,     FAILED,     false",
        })
        void payoutCanTransitToTargetStatus_matrix(PayoutStatus from, PayoutStatus to, boolean expected) {
            // when
            boolean result = from.payoutCanTransitToTargetStatus(to);

            // then
            assertThat(result).isEqualTo(expected);
        }

        @ParameterizedTest(name = "{0} → null 은 항상 false")
        @DisplayName("목표 상태가 null이면 어떤 상태에서도 전이가 불가능하다")
        @EnumSource(PayoutStatus.class)
        void payoutCanTransitToTargetStatus_whenTargetIsNull_thenFalse(PayoutStatus from) {
            // when
            boolean result = from.payoutCanTransitToTargetStatus(null);

            // then
            assertThat(result).isFalse();
        }
    }

    @Test
    @DisplayName("SUCCESS, FAILED는 종료 상태이므로 어떤 상태로도 전이할 수 없다")
    void payoutCanTransitToTargetStatus_terminalStates_alwaysFalse() {
        for (PayoutStatus target : PayoutStatus.values()) {
            assertThat(PayoutStatus.SUCCESS.payoutCanTransitToTargetStatus(target)).isFalse();
            assertThat(PayoutStatus.FAILED.payoutCanTransitToTargetStatus(target)).isFalse();
        }
    }
}
