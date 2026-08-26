package com.example.allinmarket.domain.refund.entity;

import com.example.allinmarket.buyer.entity.Buyer;
import com.example.allinmarket.domain.payment.entity.Payment;
import com.example.allinmarket.domain.refund.enums.ReasonEnum;
import com.example.allinmarket.domain.refund.enums.RefundStatus;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

class RefundTest {

    @Nested
    @DisplayName("환불 생성")
    class OfTest {

        @Test
        @DisplayName("환불을 생성하면 상태는 PENDING이고 처리시각은 null이다")
        void of_whenCreated_thenStatusIsPendingAndProcessedAtIsNull() {
            // given
            Buyer buyer = mock(Buyer.class);
            Payment payment = mock(Payment.class);

            // when
            Refund refund = Refund.of(buyer, payment, ReasonEnum.CHANGE_OF_MIND, "단순 변심");

            // then
            assertThat(refund.getStatus()).isEqualTo(RefundStatus.PENDING);
            assertThat(refund.getProcessedAt()).isNull();
        }
    }

    @Nested
    @DisplayName("환불 성공 처리")
    class SuccessTest {

        @Test
        @DisplayName("PROCESSING 상태에서는 SUCCESS로 전이되고 처리시각이 설정된다")
        void success_whenProcessing_thenTransitToSuccessAndSetProcessedAt() {
            // given
            Refund refund = createRefund(RefundStatus.PROCESSING);

            // when
            refund.success();

            // then
            assertThat(refund.getStatus()).isEqualTo(RefundStatus.SUCCESS);
            assertThat(refund.getProcessedAt()).isNotNull();
        }

        @Test
        @DisplayName("PENDING 상태에서는 SUCCESS로 전이할 수 없어 상태가 유지된다")
        void success_whenPending_thenNoTransition() {
            // given
            Refund refund = createRefund(RefundStatus.PENDING);

            // when
            refund.success();

            // then
            assertThat(refund.getStatus()).isEqualTo(RefundStatus.PENDING);
            assertThat(refund.getProcessedAt()).isNull();
        }
    }

    @Nested
    @DisplayName("환불 대기 처리")
    class PendingTest {

        @Test
        @DisplayName("FAILED 상태에서는 PENDING으로 전이된다")
        void pending_whenFailed_thenTransitToPending() {
            // given
            Refund refund = createRefund(RefundStatus.FAILED);

            // when
            refund.pending();

            // then
            assertThat(refund.getStatus()).isEqualTo(RefundStatus.PENDING);
        }

        @Test
        @DisplayName("PROCESSING 상태에서는 PENDING으로 전이할 수 없어 상태가 유지된다")
        void pending_whenProcessing_thenNoTransition() {
            // given
            Refund refund = createRefund(RefundStatus.PROCESSING);

            // when
            refund.pending();

            // then
            assertThat(refund.getStatus()).isEqualTo(RefundStatus.PROCESSING);
        }
    }

    @Nested
    @DisplayName("환불 실패 처리")
    class FailTest {

        @Test
        @DisplayName("PROCESSING 상태에서는 FAILED로 전이된다")
        void fail_whenProcessing_thenTransitToFailed() {
            // given
            Refund refund = createRefund(RefundStatus.PROCESSING);

            // when
            refund.fail();

            // then
            assertThat(refund.getStatus()).isEqualTo(RefundStatus.FAILED);
        }

        @Test
        @DisplayName("PENDING 상태에서는 FAILED로 전이할 수 없어 상태가 유지된다")
        void fail_whenPending_thenNoTransition() {
            // given
            Refund refund = createRefund(RefundStatus.PENDING);

            // when
            refund.fail();

            // then
            assertThat(refund.getStatus()).isEqualTo(RefundStatus.PENDING);
        }
    }

    @Nested
    @DisplayName("환불 사유/설명 변경")
    class UpdateTest {

        @Test
        @DisplayName("환불 사유를 변경하면 반영된다")
        void updateReason_whenCalled_thenChangeReason() {
            // given
            Refund refund = createRefund(RefundStatus.PENDING);

            // when
            refund.updateReason(ReasonEnum.DAMAGED);

            // then
            assertThat(refund.getReason()).isEqualTo(ReasonEnum.DAMAGED);
        }

        @Test
        @DisplayName("환불 설명을 변경하면 반영된다")
        void updateDescription_whenCalled_thenChangeDescription() {
            // given
            Refund refund = createRefund(RefundStatus.PENDING);

            // when
            refund.updateDescription("변경된 설명");

            // then
            assertThat(refund.getDescription()).isEqualTo("변경된 설명");
        }
    }

    private Refund createRefund(RefundStatus status) {
        Buyer buyer = mock(Buyer.class);
        Payment payment = mock(Payment.class);
        Refund refund = Refund.of(buyer, payment, ReasonEnum.CHANGE_OF_MIND, "단순 변심");
        ReflectionTestUtils.setField(refund, "status", status);
        return refund;
    }
}
