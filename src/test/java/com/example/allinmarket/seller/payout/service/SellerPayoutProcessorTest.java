package com.example.allinmarket.seller.payout.service;

import com.example.allinmarket.common.enums.ErrorEnum;
import com.example.allinmarket.common.exception.BaseException;
import com.example.allinmarket.domain.banking.BankingGateway;
import com.example.allinmarket.domain.banking.dto.BankingResponse;
import com.example.allinmarket.domain.payout.entity.Payout;
import com.example.allinmarket.domain.payout.enums.PayoutStatus;
import com.example.allinmarket.domain.payout.repository.PayoutRepository;
import com.example.allinmarket.domain.settlement.entity.Settlement;
import com.example.allinmarket.domain.settlement.enums.SettlementStatus;
import com.example.allinmarket.domain.settlement.enums.SettlementType;
import com.example.allinmarket.domain.settlement.repository.SettlementRepository;
import com.example.allinmarket.seller.entity.Seller;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class SellerPayoutProcessorTest {

    @Mock
    private BankingGateway bankingGateway;

    @Mock
    private SettlementRepository settlementRepository;

    @Mock
    private PayoutRepository payoutRepository;

    @InjectMocks
    private SellerPayoutProcessor sellerPayoutProcessor;

    @Nested
    @DisplayName("단건 지급 처리")
    class ProcessSinglePayoutTest {

        @Test
        @DisplayName("지급 내역이 없으면 PAYOUT_NOT_FOUND 예외가 발생한다")
        void processSinglePayout_whenPayoutNotFound_thenThrowsPayoutNotFound() {
            // given
            given(payoutRepository.findByIdForUpdate(1L)).willReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> sellerPayoutProcessor.processSinglePayout(1L))
                    .isInstanceOf(BaseException.class)
                    .extracting("errorEnum")
                    .isEqualTo(ErrorEnum.PAYOUT_NOT_FOUND);
        }

        @Test
        @DisplayName("PROCESSING으로 전이할 수 없는 상태이면 markProcessing에서 예외가 발생한다")
        void processSinglePayout_whenStatusCannotTransitToProcessing_thenThrowsPayoutStatusInvalid() {
            // given
            Payout payout = payout(1L, PayoutStatus.SUCCESS);
            given(payoutRepository.findByIdForUpdate(1L)).willReturn(Optional.of(payout));

            // when & then
            assertThatThrownBy(() -> sellerPayoutProcessor.processSinglePayout(1L))
                    .isInstanceOf(BaseException.class)
                    .extracting("errorEnum")
                    .isEqualTo(ErrorEnum.PAYOUT_STATUS_INVALID);

            verify(bankingGateway, never()).getBanking(any(), any(), any(), any());
        }

        @Test
        @DisplayName("뱅킹 응답 성공 시 멱등키가 일치하지 않으면 PAYOUT_MISMATCH 예외가 발생한다")
        void processSinglePayout_whenPayoutKeyMismatch_thenThrowsPayoutMismatch() {
            // given
            Payout payout = payout(1L, PayoutStatus.PENDING);
            given(payoutRepository.findByIdForUpdate(1L)).willReturn(Optional.of(payout));
            given(bankingGateway.getBanking(payout.getPayoutKey(), "KOOKMIN", "110-1234-5678", BigDecimal.valueOf(10000)))
                    .willReturn(bankingResponse(true, "OTHER_KEY", BigDecimal.valueOf(10000)));

            // when & then
            assertThatThrownBy(() -> sellerPayoutProcessor.processSinglePayout(1L))
                    .isInstanceOf(BaseException.class)
                    .extracting("errorEnum")
                    .isEqualTo(ErrorEnum.PAYOUT_MISMATCH);
        }

        @Test
        @DisplayName("뱅킹 응답 성공 시 금액이 null이면 PAYOUT_AMOUNT_INVALID 예외가 발생한다")
        void processSinglePayout_whenResponseAmountNull_thenThrowsPayoutAmountInvalid() {
            // given
            Payout payout = payout(1L, PayoutStatus.PENDING);
            given(payoutRepository.findByIdForUpdate(1L)).willReturn(Optional.of(payout));
            given(bankingGateway.getBanking(payout.getPayoutKey(), "KOOKMIN", "110-1234-5678", BigDecimal.valueOf(10000)))
                    .willReturn(bankingResponse(true, payout.getPayoutKey(), null));

            // when & then
            assertThatThrownBy(() -> sellerPayoutProcessor.processSinglePayout(1L))
                    .isInstanceOf(BaseException.class)
                    .extracting("errorEnum")
                    .isEqualTo(ErrorEnum.PAYOUT_AMOUNT_INVALID);
        }

        @Test
        @DisplayName("뱅킹 응답 성공 시 금액이 일치하지 않으면 PAYOUT_AMOUNT_MISMATCH 예외가 발생한다")
        void processSinglePayout_whenAmountMismatch_thenThrowsPayoutAmountMismatch() {
            // given
            Payout payout = payout(1L, PayoutStatus.PENDING);
            given(payoutRepository.findByIdForUpdate(1L)).willReturn(Optional.of(payout));
            given(bankingGateway.getBanking(payout.getPayoutKey(), "KOOKMIN", "110-1234-5678", BigDecimal.valueOf(10000)))
                    .willReturn(bankingResponse(true, payout.getPayoutKey(), BigDecimal.valueOf(9999)));

            // when & then
            assertThatThrownBy(() -> sellerPayoutProcessor.processSinglePayout(1L))
                    .isInstanceOf(BaseException.class)
                    .extracting("errorEnum")
                    .isEqualTo(ErrorEnum.PAYOUT_AMOUNT_MISMATCH);
        }

        @Test
        @DisplayName("검증을 모두 통과하면 지급을 성공 처리하고 정산을 지급 완료로 변경한다")
        void processSinglePayout_whenValidationPasses_thenMarksPayoutSuccessAndSettlementDone() {
            // given
            Payout payout = payout(1L, PayoutStatus.PENDING);
            Settlement settlement = settlement(1L, SettlementStatus.PAYOUT_READY);
            given(payoutRepository.findByIdForUpdate(1L)).willReturn(Optional.of(payout));
            given(bankingGateway.getBanking(payout.getPayoutKey(), "KOOKMIN", "110-1234-5678", BigDecimal.valueOf(10000)))
                    .willReturn(bankingResponse(true, payout.getPayoutKey(), BigDecimal.valueOf(10000)));
            given(settlementRepository.findById(1L)).willReturn(Optional.of(settlement));

            // when
            sellerPayoutProcessor.processSinglePayout(1L);

            // then
            assertThat(payout.getStatus()).isEqualTo(PayoutStatus.SUCCESS);
            assertThat(payout.getProcessedAt()).isNotNull();
            assertThat(settlement.getStatus()).isEqualTo(SettlementStatus.PAYOUT_DONE);
        }

        @Test
        @DisplayName("검증은 통과했으나 정산이 없으면 SETTLEMENT_NOT_FOUND 예외가 발생한다")
        void processSinglePayout_whenValidationPassesButSettlementNotFound_thenThrowsSettlementNotFound() {
            // given
            Payout payout = payout(1L, PayoutStatus.PENDING);
            given(payoutRepository.findByIdForUpdate(1L)).willReturn(Optional.of(payout));
            given(bankingGateway.getBanking(payout.getPayoutKey(), "KOOKMIN", "110-1234-5678", BigDecimal.valueOf(10000)))
                    .willReturn(bankingResponse(true, payout.getPayoutKey(), BigDecimal.valueOf(10000)));
            given(settlementRepository.findById(1L)).willReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> sellerPayoutProcessor.processSinglePayout(1L))
                    .isInstanceOf(BaseException.class)
                    .extracting("errorEnum")
                    .isEqualTo(ErrorEnum.SETTLEMENT_NOT_FOUND);
        }

        @Test
        @DisplayName("뱅킹 응답이 실패면 지급을 실패 처리하고 정산은 건드리지 않는다")
        void processSinglePayout_whenBankingResponseFails_thenMarksPayoutFailedAndSkipsSettlement() {
            // given
            Payout payout = payout(1L, PayoutStatus.PENDING);
            given(payoutRepository.findByIdForUpdate(1L)).willReturn(Optional.of(payout));
            given(bankingGateway.getBanking(payout.getPayoutKey(), "KOOKMIN", "110-1234-5678", BigDecimal.valueOf(10000)))
                    .willReturn(bankingResponse(false, payout.getPayoutKey(), null));

            // when
            sellerPayoutProcessor.processSinglePayout(1L);

            // then
            assertThat(payout.getStatus()).isEqualTo(PayoutStatus.FAILED);
            assertThat(payout.getProcessedAt()).isNotNull();
            verify(settlementRepository, never()).findById(any());
        }
    }

    @Nested
    @DisplayName("단건 지급 생성")
    class CreateSinglePayoutTest {

        @Test
        @DisplayName("정산이 없으면 SETTLEMENT_NOT_FOUND 예외가 발생한다")
        void createSinglePayout_whenSettlementNotFound_thenThrowsSettlementNotFound() {
            // given
            given(settlementRepository.findById(1L)).willReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> sellerPayoutProcessor.createSinglePayout(1L))
                    .isInstanceOf(BaseException.class)
                    .extracting("errorEnum")
                    .isEqualTo(ErrorEnum.SETTLEMENT_NOT_FOUND);

            verify(payoutRepository, never()).save(any());
        }

        @Test
        @DisplayName("정산 정보로 지급을 PENDING 상태로 생성하고 정산을 지급 준비 상태로 변경한다")
        void createSinglePayout_whenSettlementExists_thenSavesPendingPayoutAndMarksSettlementReady() {
            // given
            Settlement settlement = settlement(1L, SettlementStatus.COMPLETED);
            given(settlementRepository.findById(1L)).willReturn(Optional.of(settlement));

            // when
            sellerPayoutProcessor.createSinglePayout(1L);

            // then
            ArgumentCaptor<Payout> captor = ArgumentCaptor.forClass(Payout.class);
            verify(payoutRepository).save(captor.capture());

            Payout savedPayout = captor.getValue();
            assertThat(savedPayout.getPayoutKey()).isEqualTo("PAYOUT_1");
            assertThat(savedPayout.getStatus()).isEqualTo(PayoutStatus.PENDING);
            assertThat(savedPayout.getAmount()).isEqualByComparingTo(settlement.getAmount());
            assertThat(savedPayout.getFee()).isEqualByComparingTo(settlement.getFee());
            assertThat(savedPayout.getSeller()).isEqualTo(settlement.getSeller());

            assertThat(settlement.getStatus()).isEqualTo(SettlementStatus.PAYOUT_READY);
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

    private Settlement settlement(Long id, SettlementStatus status) {
        Settlement settlement = Settlement.of(
                seller(),
                BigDecimal.valueOf(10000),
                BigDecimal.valueOf(500),
                status,
                SettlementType.MID,
                LocalDate.of(2024, 1, 1),
                LocalDate.of(2024, 1, 15),
                null
        );
        ReflectionTestUtils.setField(settlement, "id", id);
        return settlement;
    }

    private BankingResponse bankingResponse(boolean success, String payoutKey, BigDecimal amount) {
        return new BankingResponse(success, "txn-1", payoutKey, amount, null);
    }
}
