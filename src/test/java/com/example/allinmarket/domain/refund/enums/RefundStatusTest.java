package com.example.allinmarket.domain.refund.enums;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.EnumSource;

import static org.assertj.core.api.Assertions.assertThat;

class RefundStatusTest {

    @Nested
    @DisplayName("환불 상태 전이 매트릭스")
    class RefundCanTransitToTargetStatusTest {

        @ParameterizedTest(name = "{0} → {1} 은 {2}")
        @DisplayName("코드에 정의된 전이 규칙을 그대로 검증한다 (FAILED에서의 재시도 전이 포함)")
        @CsvSource({
                "NONE,       NONE,       false",
                "NONE,       PENDING,    true",
                "NONE,       PROCESSING, false",
                "NONE,       SUCCESS,    false",
                "NONE,       FAILED,     false",
                "NONE,       DENIED,     false",

                "PENDING,    NONE,       false",
                "PENDING,    PENDING,    false",
                "PENDING,    PROCESSING, true",
                "PENDING,    SUCCESS,    false",
                "PENDING,    FAILED,     false",
                "PENDING,    DENIED,     true",

                "PROCESSING, NONE,       false",
                "PROCESSING, PENDING,    false",
                "PROCESSING, PROCESSING, false",
                "PROCESSING, SUCCESS,    true",
                "PROCESSING, FAILED,     true",
                "PROCESSING, DENIED,     false",

                "FAILED,     NONE,       false",
                "FAILED,     PENDING,    true",
                "FAILED,     PROCESSING, true",
                "FAILED,     SUCCESS,    false",
                "FAILED,     FAILED,     false",
                "FAILED,     DENIED,     false",

                "SUCCESS,    NONE,       false",
                "SUCCESS,    PENDING,    false",
                "SUCCESS,    PROCESSING, false",
                "SUCCESS,    SUCCESS,    false",
                "SUCCESS,    FAILED,     false",
                "SUCCESS,    DENIED,     false",

                "DENIED,     NONE,       false",
                "DENIED,     PENDING,    false",
                "DENIED,     PROCESSING, false",
                "DENIED,     SUCCESS,    false",
                "DENIED,     FAILED,     false",
                "DENIED,     DENIED,     false",
        })
        void refundCanTransitToTargetStatus_matrix(RefundStatus from, RefundStatus to, boolean expected) {
            // when
            boolean result = from.refundCanTransitToTargetStatus(to);

            // then
            assertThat(result).isEqualTo(expected);
        }

        @ParameterizedTest(name = "{0} → null 은 항상 false")
        @DisplayName("목표 상태가 null이면 어떤 상태에서도 전이가 불가능하다")
        @EnumSource(RefundStatus.class)
        void refundCanTransitToTargetStatus_whenTargetIsNull_thenFalse(RefundStatus from) {
            // when
            boolean result = from.refundCanTransitToTargetStatus(null);

            // then
            assertThat(result).isFalse();
        }
    }

    @Test
    @DisplayName("SUCCESS, DENIED는 종료 상태이므로 어떤 상태로도 전이할 수 없다")
    void refundCanTransitToTargetStatus_terminalStates_alwaysFalse() {
        for (RefundStatus target : RefundStatus.values()) {
            assertThat(RefundStatus.SUCCESS.refundCanTransitToTargetStatus(target)).isFalse();
            assertThat(RefundStatus.DENIED.refundCanTransitToTargetStatus(target)).isFalse();
        }
    }

    @Test
    @DisplayName("FAILED에서는 PENDING, PROCESSING으로 되돌아가는(재시도) 전이가 허용된다")
    void refundCanTransitToTargetStatus_failedAllowsRetryBackToPendingOrProcessing() {
        assertThat(RefundStatus.FAILED.refundCanTransitToTargetStatus(RefundStatus.PENDING)).isTrue();
        assertThat(RefundStatus.FAILED.refundCanTransitToTargetStatus(RefundStatus.PROCESSING)).isTrue();
    }
}
