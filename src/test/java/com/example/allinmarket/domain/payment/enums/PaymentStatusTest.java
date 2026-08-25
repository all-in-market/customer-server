package com.example.allinmarket.domain.payment.enums;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.EnumSource;

import static org.assertj.core.api.Assertions.assertThat;

class PaymentStatusTest {

    @Nested
    @DisplayName("결제 상태 전이 매트릭스")
    class PaymentCanTransitToTargetStatusTest {

        @ParameterizedTest(name = "{0} → {1} 은 {2}")
        @DisplayName("코드에 정의된 전이 규칙을 그대로 검증한다")
        @CsvSource({
                "PENDING,  PENDING,  false",
                "PENDING,  SUCCESS,  true",
                "PENDING,  FAILED,   true",
                "PENDING,  REFUNDED, true",

                "SUCCESS,  PENDING,  false",
                "SUCCESS,  SUCCESS,  false",
                "SUCCESS,  FAILED,   false",
                "SUCCESS,  REFUNDED, true",

                "FAILED,   PENDING,  false",
                "FAILED,   SUCCESS,  false",
                "FAILED,   FAILED,   false",
                "FAILED,   REFUNDED, true",

                "REFUNDED, PENDING,  false",
                "REFUNDED, SUCCESS,  false",
                "REFUNDED, FAILED,   false",
                "REFUNDED, REFUNDED, false",
        })
        void paymentCanTransitToTargetStatus_matrix(PaymentStatus from, PaymentStatus to, boolean expected) {
            // when
            boolean result = from.paymentCanTransitToTargetStatus(to);

            // then
            assertThat(result).isEqualTo(expected);
        }

        @ParameterizedTest(name = "{0} → null 은 항상 false")
        @DisplayName("목표 상태가 null이면 어떤 상태에서도 전이가 불가능하다")
        @EnumSource(PaymentStatus.class)
        void paymentCanTransitToTargetStatus_whenTargetIsNull_thenFalse(PaymentStatus from) {
            // when
            boolean result = from.paymentCanTransitToTargetStatus(null);

            // then
            assertThat(result).isFalse();
        }
    }

    @Test
    @DisplayName("REFUNDED는 종료 상태이므로 어떤 상태로도 전이할 수 없다")
    void paymentCanTransitToTargetStatus_refundedIsTerminal_alwaysFalse() {
        for (PaymentStatus target : PaymentStatus.values()) {
            assertThat(PaymentStatus.REFUNDED.paymentCanTransitToTargetStatus(target)).isFalse();
        }
    }
}
