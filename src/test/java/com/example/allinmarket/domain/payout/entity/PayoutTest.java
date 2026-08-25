package com.example.allinmarket.domain.payout.entity;

import com.example.allinmarket.common.enums.ErrorEnum;
import com.example.allinmarket.common.exception.BaseException;
import com.example.allinmarket.domain.payout.enums.PayoutStatus;
import com.example.allinmarket.seller.entity.Seller;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;

class PayoutTest {

    @Nested
    @DisplayName("지급 성공 처리")
    class SuccessTest {

        @Test
        @DisplayName("이미 성공 상태이면 예외가 발생한다")
        void success_whenAlreadySuccess_thenThrowAlreadySuccess() {
            // given
            Payout payout = createPayout(PayoutStatus.SUCCESS);

            // when & then
            assertThatThrownBy(payout::success)
                    .isInstanceOf(BaseException.class)
                    .extracting("errorEnum")
                    .isEqualTo(ErrorEnum.PAYOUT_ALREADY_SUCCESS);
        }

        @Test
        @DisplayName("이미 실패로 종결된 상태이면 예외가 발생한다")
        void success_whenAlreadyFailed_thenThrowAlreadyFailed() {
            // given
            Payout payout = createPayout(PayoutStatus.FAILED);

            // when & then
            assertThatThrownBy(payout::success)
                    .isInstanceOf(BaseException.class)
                    .extracting("errorEnum")
                    .isEqualTo(ErrorEnum.PAYOUT_ALREADY_FAILED);
        }

        @Test
        @DisplayName("처리중 상태에서 성공 처리하면 상태가 SUCCESS로 바뀌고 처리시각이 설정된다")
        void success_whenProcessing_thenTransitToSuccessAndSetProcessedAt() {
            // given
            Payout payout = createPayout(PayoutStatus.PROCESSING);

            // when
            payout.success();

            // then
            assertThat(payout.getStatus()).isEqualTo(PayoutStatus.SUCCESS);
            assertThat(payout.getProcessedAt()).isNotNull();
        }
    }

    @Nested
    @DisplayName("지급 실패 처리")
    class FailTest {

        @Test
        @DisplayName("이미 실패 상태이면 예외가 발생한다")
        void fail_whenAlreadyFailed_thenThrowAlreadyFailed() {
            // given
            Payout payout = createPayout(PayoutStatus.FAILED);

            // when & then
            assertThatThrownBy(payout::fail)
                    .isInstanceOf(BaseException.class)
                    .extracting("errorEnum")
                    .isEqualTo(ErrorEnum.PAYOUT_ALREADY_FAILED);
        }

        @Test
        @DisplayName("이미 성공으로 종결된 상태이면 예외가 발생한다")
        void fail_whenAlreadySuccess_thenThrowAlreadySuccess() {
            // given
            Payout payout = createPayout(PayoutStatus.SUCCESS);

            // when & then
            assertThatThrownBy(payout::fail)
                    .isInstanceOf(BaseException.class)
                    .extracting("errorEnum")
                    .isEqualTo(ErrorEnum.PAYOUT_ALREADY_SUCCESS);
        }

        @Test
        @DisplayName("처리중 상태에서 실패 처리하면 상태가 FAILED로 바뀌고 처리시각이 설정된다")
        void fail_whenProcessing_thenTransitToFailedAndSetProcessedAt() {
            // given
            Payout payout = createPayout(PayoutStatus.PROCESSING);

            // when
            payout.fail();

            // then
            assertThat(payout.getStatus()).isEqualTo(PayoutStatus.FAILED);
            assertThat(payout.getProcessedAt()).isNotNull();
        }
    }

    @Nested
    @DisplayName("처리중 상태 전이")
    class MarkProcessingTest {

        @Test
        @DisplayName("대기 상태에서는 처리중 상태로 전이된다")
        void markProcessing_whenPending_thenTransitToProcessing() {
            // given
            Payout payout = createPayout(PayoutStatus.PENDING);

            // when
            payout.markProcessing();

            // then
            assertThat(payout.getStatus()).isEqualTo(PayoutStatus.PROCESSING);
        }

        @Test
        @DisplayName("이미 종결된 상태(SUCCESS)에서는 전이할 수 없어 예외가 발생한다")
        void markProcessing_whenSuccess_thenThrowStatusInvalid() {
            // given
            Payout payout = createPayout(PayoutStatus.SUCCESS);

            // when & then
            assertThatThrownBy(payout::markProcessing)
                    .isInstanceOf(BaseException.class)
                    .extracting("errorEnum")
                    .isEqualTo(ErrorEnum.PAYOUT_STATUS_INVALID);
        }
    }

    @Nested
    @DisplayName("재시도 횟수 증가")
    class IncreaseRetryCountTest {

        @Test
        @DisplayName("호출할 때마다 재시도 횟수가 1씩 증가한다")
        void increaseRetryCount_whenCalledTwice_thenIncreaseByOneEachTime() {
            // given
            Payout payout = createPayout(PayoutStatus.PENDING);

            // when
            payout.increaseRetryCount();
            payout.increaseRetryCount();

            // then
            assertThat(payout.getRetryCount()).isEqualTo(2);
        }
    }

    private Payout createPayout(PayoutStatus status) {
        Seller seller = mock(Seller.class);
        Payout payout = Payout.of(
                seller, 1L, BigDecimal.valueOf(1000), BigDecimal.valueOf(50), status, "payout-key"
        );
        ReflectionTestUtils.setField(payout, "status", status);
        return payout;
    }
}
