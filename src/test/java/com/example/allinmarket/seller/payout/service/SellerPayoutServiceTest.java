package com.example.allinmarket.seller.payout.service;

import com.example.allinmarket.common.enums.ErrorEnum;
import com.example.allinmarket.common.exception.BaseException;
import com.example.allinmarket.domain.payout.entity.Payout;
import com.example.allinmarket.domain.payout.enums.PayoutStatus;
import com.example.allinmarket.domain.payout.repository.PayoutRepository;
import com.example.allinmarket.domain.settlement.entity.Settlement;
import com.example.allinmarket.domain.settlement.enums.SettlementStatus;
import com.example.allinmarket.domain.settlement.enums.SettlementType;
import com.example.allinmarket.domain.settlement.repository.SettlementRepository;
import com.example.allinmarket.seller.entity.Seller;
import org.hibernate.exception.ConstraintViolationException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Pageable;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class SellerPayoutServiceTest {

    @Mock
    private PayoutRepository payoutRepository;

    @Mock
    private SettlementRepository settlementRepository;

    @Mock
    private SellerPayoutProcessor sellerPayoutProcessor;

    @Mock
    private SellerPayoutFailHandler sellerPayoutFailHandler;

    @InjectMocks
    private SellerPayoutService sellerPayoutService;

    @Nested
    @DisplayName("지급 데이터 생성 배치")
    class CreatePayoutTest {

        @Test
        @DisplayName("대상 정산이 없으면 즉시 루프를 종료하고 processor를 호출하지 않는다")
        void createPayout_whenNoSettlements_thenLoopStopsImmediately() {
            // given
            given(settlementRepository.findWithoutPayout(eq(SettlementStatus.COMPLETED), anyLong(), any(Pageable.class)))
                    .willReturn(List.of());

            // when
            sellerPayoutService.createPayout();

            // then
            verify(sellerPayoutProcessor, never()).createSinglePayout(anyLong());
        }

        @Test
        @DisplayName("정상 정산 건들에 대해 createSinglePayout을 각각 호출한다")
        void createPayout_whenSettlementsExist_thenCallsProcessorForEachSettlement() {
            // given
            Settlement settlement1 = settlement(1L);
            Settlement settlement2 = settlement(2L);
            given(settlementRepository.findWithoutPayout(eq(SettlementStatus.COMPLETED), anyLong(), any(Pageable.class)))
                    .willReturn(List.of(settlement1, settlement2), List.of());

            // when
            sellerPayoutService.createPayout();

            // then
            verify(sellerPayoutProcessor).createSinglePayout(1L);
            verify(sellerPayoutProcessor).createSinglePayout(2L);
        }

        @Test
        @DisplayName("uk_payout_settlement_id 제약 위반이면 해당 건만 스킵하고 다음 건을 계속 처리한다")
        void createPayout_whenDuplicateConstraintViolation_thenSkipAndContinue() {
            // given
            Settlement settlement1 = settlement(1L);
            Settlement settlement2 = settlement(2L);
            given(settlementRepository.findWithoutPayout(eq(SettlementStatus.COMPLETED), anyLong(), any(Pageable.class)))
                    .willReturn(List.of(settlement1, settlement2), List.of());

            ConstraintViolationException constraintViolation = mock(ConstraintViolationException.class);
            given(constraintViolation.getConstraintName()).willReturn("uk_payout_settlement_id");
            DataIntegrityViolationException duplicateException =
                    new DataIntegrityViolationException("duplicate", constraintViolation);

            willThrow(duplicateException).given(sellerPayoutProcessor).createSinglePayout(1L);

            // when & then
            assertThatCode(() -> sellerPayoutService.createPayout()).doesNotThrowAnyException();

            verify(sellerPayoutProcessor).createSinglePayout(1L);
            verify(sellerPayoutProcessor).createSinglePayout(2L);
        }

        @Test
        @DisplayName("uk_payout_settlement_id가 아닌 다른 제약 위반이면 예외가 전파된다")
        void createPayout_whenOtherConstraintViolation_thenPropagatesException() {
            // given
            Settlement settlement1 = settlement(1L);
            given(settlementRepository.findWithoutPayout(eq(SettlementStatus.COMPLETED), anyLong(), any(Pageable.class)))
                    .willReturn(List.of(settlement1));

            ConstraintViolationException constraintViolation = mock(ConstraintViolationException.class);
            given(constraintViolation.getConstraintName()).willReturn("uk_some_other_constraint");
            DataIntegrityViolationException otherException =
                    new DataIntegrityViolationException("violation", constraintViolation);

            willThrow(otherException).given(sellerPayoutProcessor).createSinglePayout(1L);

            // when & then
            assertThatThrownBy(() -> sellerPayoutService.createPayout())
                    .isInstanceOf(DataIntegrityViolationException.class);
        }

        @Test
        @DisplayName("일반 RuntimeException이 발생해도 로깅만 하고 다음 건을 계속 처리한다")
        void createPayout_whenGenericRuntimeException_thenLogsAndContinues() {
            // given
            Settlement settlement1 = settlement(1L);
            Settlement settlement2 = settlement(2L);
            given(settlementRepository.findWithoutPayout(eq(SettlementStatus.COMPLETED), anyLong(), any(Pageable.class)))
                    .willReturn(List.of(settlement1, settlement2), List.of());

            willThrow(new RuntimeException("boom")).given(sellerPayoutProcessor).createSinglePayout(1L);

            // when & then
            assertThatCode(() -> sellerPayoutService.createPayout()).doesNotThrowAnyException();

            verify(sellerPayoutProcessor).createSinglePayout(1L);
            verify(sellerPayoutProcessor).createSinglePayout(2L);
        }
    }

    @Nested
    @DisplayName("지급 처리 배치")
    class ProcessPayoutTest {

        @Test
        @DisplayName("대상 지급 건이 없으면 즉시 루프를 종료하고 아무것도 호출하지 않는다")
        void processPayout_whenNoPayouts_thenLoopStopsImmediately() {
            // given
            given(payoutRepository.findBatch(eq(PayoutStatus.PENDING), eq(5), anyLong(), any(Pageable.class)))
                    .willReturn(List.of());

            // when
            sellerPayoutService.processPayout();

            // then
            verify(sellerPayoutProcessor, never()).processSinglePayout(anyLong());
            verify(sellerPayoutFailHandler, never()).handlePayoutFail(anyLong(), anyBoolean());
        }

        @Test
        @DisplayName("PAYOUT_MISMATCH 검증 예외가 발생하면 handlePayoutFail을 isValidation=true로 호출한다")
        void processPayout_whenPayoutMismatch_thenHandlePayoutFailWithValidationTrue() {
            // given
            Payout payout = payout(1L, PayoutStatus.PENDING);
            given(payoutRepository.findBatch(eq(PayoutStatus.PENDING), eq(5), anyLong(), any(Pageable.class)))
                    .willReturn(List.of(payout), List.of());

            willThrow(new BaseException(ErrorEnum.PAYOUT_MISMATCH))
                    .given(sellerPayoutProcessor).processSinglePayout(1L);

            // when
            sellerPayoutService.processPayout();

            // then
            verify(sellerPayoutFailHandler).handlePayoutFail(1L, true);
        }

        @Test
        @DisplayName("PAYOUT_AMOUNT_INVALID 검증 예외가 발생하면 handlePayoutFail을 isValidation=true로 호출한다")
        void processPayout_whenPayoutAmountInvalid_thenHandlePayoutFailWithValidationTrue() {
            // given
            Payout payout = payout(1L, PayoutStatus.PENDING);
            given(payoutRepository.findBatch(eq(PayoutStatus.PENDING), eq(5), anyLong(), any(Pageable.class)))
                    .willReturn(List.of(payout), List.of());

            willThrow(new BaseException(ErrorEnum.PAYOUT_AMOUNT_INVALID))
                    .given(sellerPayoutProcessor).processSinglePayout(1L);

            // when
            sellerPayoutService.processPayout();

            // then
            verify(sellerPayoutFailHandler).handlePayoutFail(1L, true);
        }

        @Test
        @DisplayName("PAYOUT_AMOUNT_MISMATCH 검증 예외가 발생하면 handlePayoutFail을 isValidation=true로 호출한다")
        void processPayout_whenPayoutAmountMismatch_thenHandlePayoutFailWithValidationTrue() {
            // given
            Payout payout = payout(1L, PayoutStatus.PENDING);
            given(payoutRepository.findBatch(eq(PayoutStatus.PENDING), eq(5), anyLong(), any(Pageable.class)))
                    .willReturn(List.of(payout), List.of());

            willThrow(new BaseException(ErrorEnum.PAYOUT_AMOUNT_MISMATCH))
                    .given(sellerPayoutProcessor).processSinglePayout(1L);

            // when
            sellerPayoutService.processPayout();

            // then
            verify(sellerPayoutFailHandler).handlePayoutFail(1L, true);
        }

        @Test
        @DisplayName("검증 예외가 아닌 다른 BaseException이 발생하면 handlePayoutFail을 isValidation=false로 호출한다")
        void processPayout_whenOtherBaseException_thenHandlePayoutFailWithValidationFalse() {
            // given
            Payout payout = payout(1L, PayoutStatus.PENDING);
            given(payoutRepository.findBatch(eq(PayoutStatus.PENDING), eq(5), anyLong(), any(Pageable.class)))
                    .willReturn(List.of(payout), List.of());

            willThrow(new BaseException(ErrorEnum.SETTLEMENT_NOT_FOUND))
                    .given(sellerPayoutProcessor).processSinglePayout(1L);

            // when
            sellerPayoutService.processPayout();

            // then
            verify(sellerPayoutFailHandler).handlePayoutFail(1L, false);
        }

        @Test
        @DisplayName("일반 RuntimeException이 발생하면 handlePayoutFail을 isValidation=false로 호출한다")
        void processPayout_whenGenericRuntimeException_thenHandlePayoutFailWithValidationFalse() {
            // given
            Payout payout = payout(1L, PayoutStatus.PENDING);
            given(payoutRepository.findBatch(eq(PayoutStatus.PENDING), eq(5), anyLong(), any(Pageable.class)))
                    .willReturn(List.of(payout), List.of());

            willThrow(new RuntimeException("timeout"))
                    .given(sellerPayoutProcessor).processSinglePayout(1L);

            // when
            sellerPayoutService.processPayout();

            // then
            verify(sellerPayoutFailHandler).handlePayoutFail(1L, false);
        }

        @Test
        @DisplayName("조회 결과가 100건 미만이면 추가 조회 없이 루프를 종료한다")
        void processPayout_whenBatchLessThan100_thenNoAdditionalFetch() {
            // given
            Payout payout = payout(1L, PayoutStatus.PENDING);
            given(payoutRepository.findBatch(eq(PayoutStatus.PENDING), eq(5), anyLong(), any(Pageable.class)))
                    .willReturn(List.of(payout));

            // when
            sellerPayoutService.processPayout();

            // then
            verify(payoutRepository, times(1))
                    .findBatch(eq(PayoutStatus.PENDING), eq(5), anyLong(), any(Pageable.class));
        }

        @Test
        @DisplayName("조회 결과가 100건이면 다음 배치를 추가로 조회한다")
        void processPayout_whenBatchIs100_thenFetchesNextBatch() {
            // given
            List<Payout> fullBatch = IntStream.rangeClosed(1, 100)
                    .mapToObj(i -> payout((long) i, PayoutStatus.PENDING))
                    .toList();
            given(payoutRepository.findBatch(eq(PayoutStatus.PENDING), eq(5), anyLong(), any(Pageable.class)))
                    .willReturn(fullBatch, List.of());

            // when
            sellerPayoutService.processPayout();

            // then
            verify(payoutRepository, times(2))
                    .findBatch(eq(PayoutStatus.PENDING), eq(5), anyLong(), any(Pageable.class));
            verify(sellerPayoutProcessor, times(100)).processSinglePayout(anyLong());
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

    private Settlement settlement(Long id) {
        Settlement settlement = Settlement.of(
                seller(),
                BigDecimal.valueOf(10000),
                BigDecimal.valueOf(500),
                SettlementStatus.COMPLETED,
                SettlementType.MID,
                LocalDate.of(2024, 1, 1),
                LocalDate.of(2024, 1, 15),
                null
        );
        ReflectionTestUtils.setField(settlement, "id", id);
        return settlement;
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
