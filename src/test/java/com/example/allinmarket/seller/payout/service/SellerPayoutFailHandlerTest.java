package com.example.allinmarket.seller.payout.service;

import com.example.allinmarket.common.enums.ErrorEnum;
import com.example.allinmarket.common.exception.BaseException;
import com.example.allinmarket.domain.payout.entity.Payout;
import com.example.allinmarket.domain.payout.enums.PayoutStatus;
import com.example.allinmarket.domain.payout.repository.PayoutRepository;
import com.example.allinmarket.seller.entity.Seller;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class SellerPayoutFailHandlerTest {

    @Mock
    private PayoutRepository payoutRepository;

    @InjectMocks
    private SellerPayoutFailHandler sellerPayoutFailHandler;

    @Nested
    @DisplayName("지급 실패 처리")
    class HandlePayoutFailTest {

        @Test
        @DisplayName("지급 내역이 없으면 PAYOUT_NOT_FOUND 예외가 발생한다")
        void handlePayoutFail_whenPayoutNotFound_thenThrowsPayoutNotFound() {
            // given
            given(payoutRepository.findByIdForUpdate(1L)).willReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> sellerPayoutFailHandler.handlePayoutFail(1L, true))
                    .isInstanceOf(BaseException.class)
                    .extracting("errorEnum")
                    .isEqualTo(ErrorEnum.PAYOUT_NOT_FOUND);

            verify(payoutRepository).findByIdForUpdate(1L);
        }

        @Test
        @DisplayName("검증 실패(isValidation=true)면 재시도 횟수 증가 없이 즉시 실패 처리한다")
        void handlePayoutFail_whenValidationError_thenFailsImmediatelyWithoutRetryIncrement() {
            // given
            Payout payout = payout(1L, PayoutStatus.PROCESSING);
            given(payoutRepository.findByIdForUpdate(1L)).willReturn(Optional.of(payout));

            // when
            sellerPayoutFailHandler.handlePayoutFail(1L, true);

            // then
            assertThat(payout.getStatus()).isEqualTo(PayoutStatus.FAILED);
            assertThat(payout.getRetryCount()).isZero();
        }

        @Test
        @DisplayName("검증 실패가 아니고 재시도 임계치(5회) 미만이면 재시도 횟수만 증가하고 실패 처리하지 않는다")
        void handlePayoutFail_whenNotValidationAndBelowRetryThreshold_thenOnlyIncreasesRetryCount() {
            // given
            Payout payout = payout(1L, PayoutStatus.PROCESSING);
            given(payoutRepository.findByIdForUpdate(1L)).willReturn(Optional.of(payout));

            // when
            sellerPayoutFailHandler.handlePayoutFail(1L, false);

            // then
            assertThat(payout.getRetryCount()).isEqualTo(1);
            assertThat(payout.getStatus()).isEqualTo(PayoutStatus.PROCESSING);
        }

        @Test
        @DisplayName("검증 실패가 아니고 재시도 횟수가 임계치(5회) 바로 아래(4회)면 실패 처리하지 않는다")
        void handlePayoutFail_whenRetryCountJustBelowThreshold_thenDoesNotFail() {
            // given
            Payout payout = payout(1L, PayoutStatus.PROCESSING);
            ReflectionTestUtils.setField(payout, "retryCount", 3);
            given(payoutRepository.findByIdForUpdate(1L)).willReturn(Optional.of(payout));

            // when
            sellerPayoutFailHandler.handlePayoutFail(1L, false);

            // then
            assertThat(payout.getRetryCount()).isEqualTo(4);
            assertThat(payout.getStatus()).isEqualTo(PayoutStatus.PROCESSING);
        }

        @Test
        @DisplayName("검증 실패가 아니고 재시도 횟수가 임계치(5회)에 도달하면 실패 처리한다")
        void handlePayoutFail_whenRetryCountReachesThreshold_thenFails() {
            // given
            Payout payout = payout(1L, PayoutStatus.PROCESSING);
            ReflectionTestUtils.setField(payout, "retryCount", 4);
            given(payoutRepository.findByIdForUpdate(1L)).willReturn(Optional.of(payout));

            // when
            sellerPayoutFailHandler.handlePayoutFail(1L, false);

            // then
            assertThat(payout.getRetryCount()).isEqualTo(5);
            assertThat(payout.getStatus()).isEqualTo(PayoutStatus.FAILED);
        }
    }

    private Seller seller() {
        return Seller.of(
                "seller@test.com",
                "password",
                "판매자",
                "010-0000-0000",
                "store",
                "123-45-67890",
                "KOOKMIN",
                "110-1234-5678"
        );
    }

    private Payout payout(Long id, PayoutStatus status) {
        Payout payout = Payout.of(
                seller(),
                id,
                BigDecimal.valueOf(10000),
                BigDecimal.valueOf(500),
                status,
                "PAYOUT_" + id
        );
        ReflectionTestUtils.setField(payout, "id", id);
        return payout;
    }
}
