package com.example.allinmarket.domain.transactionhistory.enums;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.EnumSource;

import static org.assertj.core.api.Assertions.assertThat;

class TransactionStatusTest {

    @Nested
    @DisplayName("결제 트랜잭션 상태 전이 매트릭스 (paymentCanTransitToTargetStatus)")
    class PaymentCanTransitToTargetStatusTest {

        @ParameterizedTest(name = "{0} → {1} 은 {2}")
        @DisplayName("코드에 정의된 전이 규칙을 그대로 검증한다")
        @CsvSource({
                "PENDING,  PENDING,  false",
                "PENDING,  SUCCESS,  true",
                "PENDING,  FAILED,   true",
                "PENDING,  DENIED,   false",
                "PENDING,  REFUNDED, true",

                "SUCCESS,  PENDING,  false",
                "SUCCESS,  SUCCESS,  false",
                "SUCCESS,  FAILED,   false",
                "SUCCESS,  DENIED,   false",
                "SUCCESS,  REFUNDED, true",

                "FAILED,   PENDING,  false",
                "FAILED,   SUCCESS,  false",
                "FAILED,   FAILED,   false",
                "FAILED,   DENIED,   false",
                "FAILED,   REFUNDED, true",

                "DENIED,   PENDING,  false",
                "DENIED,   SUCCESS,  false",
                "DENIED,   FAILED,   false",
                "DENIED,   DENIED,   false",
                "DENIED,   REFUNDED, false",

                "REFUNDED, PENDING,  false",
                "REFUNDED, SUCCESS,  false",
                "REFUNDED, FAILED,   false",
                "REFUNDED, DENIED,   false",
                "REFUNDED, REFUNDED, false",
        })
        void paymentCanTransitToTargetStatus_matrix(TransactionStatus from, TransactionStatus to, boolean expected) {
            // when
            boolean result = from.paymentCanTransitToTargetStatus(to);

            // then
            assertThat(result).isEqualTo(expected);
        }

        @ParameterizedTest(name = "{0} → null 은 항상 false")
        @DisplayName("목표 상태가 null이면 어떤 상태에서도 전이가 불가능하다")
        @EnumSource(TransactionStatus.class)
        void paymentCanTransitToTargetStatus_whenTargetIsNull_thenFalse(TransactionStatus from) {
            // when
            boolean result = from.paymentCanTransitToTargetStatus(null);

            // then
            assertThat(result).isFalse();
        }
    }

    @Nested
    @DisplayName("환불 트랜잭션 상태 전이 매트릭스 (refundCanTransitToTargetStatus)")
    class RefundCanTransitToTargetStatusTest {

        @ParameterizedTest(name = "{0} → {1} 은 {2}")
        @DisplayName("코드에 정의된 전이 규칙을 그대로 검증한다 (FAILED → PENDING 재시도 포함)")
        @CsvSource({
                "PENDING,  PENDING,  false",
                "PENDING,  SUCCESS,  true",
                "PENDING,  FAILED,   true",
                "PENDING,  DENIED,   true",
                "PENDING,  REFUNDED, false",

                "FAILED,   PENDING,  true",
                "FAILED,   SUCCESS,  false",
                "FAILED,   FAILED,   false",
                "FAILED,   DENIED,   false",
                "FAILED,   REFUNDED, false",

                "SUCCESS,  PENDING,  false",
                "SUCCESS,  SUCCESS,  false",
                "SUCCESS,  FAILED,   false",
                "SUCCESS,  DENIED,   false",
                "SUCCESS,  REFUNDED, false",

                "REFUNDED, PENDING,  false",
                "REFUNDED, SUCCESS,  false",
                "REFUNDED, FAILED,   false",
                "REFUNDED, DENIED,   false",
                "REFUNDED, REFUNDED, false",

                "DENIED,   PENDING,  false",
                "DENIED,   SUCCESS,  false",
                "DENIED,   FAILED,   false",
                "DENIED,   DENIED,   false",
                "DENIED,   REFUNDED, false",
        })
        void refundCanTransitToTargetStatus_matrix(TransactionStatus from, TransactionStatus to, boolean expected) {
            // when
            boolean result = from.refundCanTransitToTargetStatus(to);

            // then
            assertThat(result).isEqualTo(expected);
        }

        @ParameterizedTest(name = "{0} → null 은 항상 false")
        @DisplayName("목표 상태가 null이면 어떤 상태에서도 전이가 불가능하다")
        @EnumSource(TransactionStatus.class)
        void refundCanTransitToTargetStatus_whenTargetIsNull_thenFalse(TransactionStatus from) {
            // when
            boolean result = from.refundCanTransitToTargetStatus(null);

            // then
            assertThat(result).isFalse();
        }
    }

    @Test
    @DisplayName("refundCanTransitToTargetStatus에서 FAILED는 PENDING으로만 되돌아갈 수 있다")
    void refundCanTransitToTargetStatus_failedAllowsRetryOnlyToPending() {
        assertThat(TransactionStatus.FAILED.refundCanTransitToTargetStatus(TransactionStatus.PENDING)).isTrue();
        assertThat(TransactionStatus.FAILED.refundCanTransitToTargetStatus(TransactionStatus.SUCCESS)).isFalse();
        assertThat(TransactionStatus.FAILED.refundCanTransitToTargetStatus(TransactionStatus.FAILED)).isFalse();
        assertThat(TransactionStatus.FAILED.refundCanTransitToTargetStatus(TransactionStatus.DENIED)).isFalse();
        assertThat(TransactionStatus.FAILED.refundCanTransitToTargetStatus(TransactionStatus.REFUNDED)).isFalse();
    }
}
