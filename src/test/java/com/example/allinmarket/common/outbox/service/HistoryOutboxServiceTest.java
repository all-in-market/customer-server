package com.example.allinmarket.common.outbox.service;

import com.example.allinmarket.common.enums.ErrorEnum;
import com.example.allinmarket.common.exception.BaseException;
import com.example.allinmarket.common.outbox.dto.HistoryOutBoxPayload;
import com.example.allinmarket.common.outbox.entity.HistoryOutbox;
import com.example.allinmarket.common.outbox.repository.HistoryOutboxRepository;
import com.example.allinmarket.domain.order.entity.Order;
import com.example.allinmarket.domain.payment.entity.Payment;
import com.example.allinmarket.domain.payment.enums.MethodEnum;
import com.example.allinmarket.domain.payment.enums.PaymentStatus;
import com.example.allinmarket.domain.refund.entity.Refund;
import com.example.allinmarket.domain.refund.enums.ReasonEnum;
import com.example.allinmarket.domain.refund.enums.RefundStatus;
import com.example.allinmarket.domain.transactionhistory.entity.TransactionHistory;
import com.example.allinmarket.domain.transactionhistory.enums.TransactionType;
import com.example.allinmarket.domain.transactionhistory.repository.TransactionHistoryRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
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
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

@ExtendWith(MockitoExtension.class)
class HistoryOutboxServiceTest {

    @Mock
    private HistoryOutboxRepository historyOutBoxRepository;

    @Mock
    private TransactionHistoryRepository transactionHistoryRepository;

    @Mock
    private ObjectMapper objectMapper;

    @InjectMocks
    private HistoryOutboxService historyOutboxService;

    @Nested
    @DisplayName("결제 이력 아웃박스 저장")
    class SavePayment {

        @Test
        @DisplayName("결제 아웃박스는 PAYMENT 타입으로 직렬화되어 저장된다")
        void save_payment_savesWithPaymentType() throws JsonProcessingException {
            // given
            Payment payment = createPayment(1L, new BigDecimal("10000"), PaymentStatus.SUCCESS);
            String json = "{\"transactionId\":1}";
            given(objectMapper.writeValueAsString(any(HistoryOutBoxPayload.class))).willReturn(json);

            // when
            historyOutboxService.save(payment);

            // then
            ArgumentCaptor<HistoryOutbox> captor = ArgumentCaptor.forClass(HistoryOutbox.class);
            verify(historyOutBoxRepository).save(captor.capture());

            HistoryOutbox saved = captor.getValue();
            assertThat(saved.getType()).isEqualTo(TransactionType.PAYMENT);
            assertThat(saved.getPayload()).isEqualTo(json);
            assertThat(saved.isProcessed()).isFalse();
        }

        @Test
        @DisplayName("직렬화 실패 시 PAYLOAD_SERIALIZATION_FAILED 예외를 던지고 저장하지 않는다")
        void save_payment_serializationFails_throwsException() throws JsonProcessingException {
            // given
            Payment payment = createPayment(1L, new BigDecimal("10000"), PaymentStatus.SUCCESS);
            given(objectMapper.writeValueAsString(any(HistoryOutBoxPayload.class)))
                    .willThrow(new JsonProcessingException("fail") {});

            // when & then
            assertThatThrownBy(() -> historyOutboxService.save(payment))
                    .isInstanceOf(BaseException.class)
                    .extracting("errorEnum")
                    .isEqualTo(ErrorEnum.PAYLOAD_SERIALIZATION_FAILED);

            verify(historyOutBoxRepository, never()).save(any());
        }
    }

    @Nested
    @DisplayName("환불 이력 아웃박스 저장")
    class SaveRefund {

        @Test
        @DisplayName("환불 아웃박스는 REFUND 타입으로 직렬화되어 저장된다")
        void save_refund_savesWithRefundType() throws JsonProcessingException {
            // given
            Payment payment = createPayment(1L, new BigDecimal("10000"), PaymentStatus.SUCCESS);
            Refund refund = createRefund(2L, payment);
            String json = "{\"transactionId\":2}";
            given(objectMapper.writeValueAsString(any(HistoryOutBoxPayload.class))).willReturn(json);

            // when
            historyOutboxService.save(refund);

            // then
            ArgumentCaptor<HistoryOutbox> captor = ArgumentCaptor.forClass(HistoryOutbox.class);
            verify(historyOutBoxRepository).save(captor.capture());

            HistoryOutbox saved = captor.getValue();
            assertThat(saved.getType()).isEqualTo(TransactionType.REFUND);
            assertThat(saved.getPayload()).isEqualTo(json);
        }

        @Test
        @DisplayName("직렬화 실패 시 PAYLOAD_SERIALIZATION_FAILED 예외를 던지고 저장하지 않는다")
        void save_refund_serializationFails_throwsException() throws JsonProcessingException {
            // given
            Payment payment = createPayment(1L, new BigDecimal("10000"), PaymentStatus.SUCCESS);
            Refund refund = createRefund(2L, payment);
            given(objectMapper.writeValueAsString(any(HistoryOutBoxPayload.class)))
                    .willThrow(new JsonProcessingException("fail") {});

            // when & then
            assertThatThrownBy(() -> historyOutboxService.save(refund))
                    .isInstanceOf(BaseException.class)
                    .extracting("errorEnum")
                    .isEqualTo(ErrorEnum.PAYLOAD_SERIALIZATION_FAILED);

            verify(historyOutBoxRepository, never()).save(any());
        }
    }

    @Nested
    @DisplayName("아웃박스 이벤트 처리")
    class Process {

        @Test
        @DisplayName("존재하지 않는 아웃박스면 HISTORY_OUTBOX_NOT_FOUND 예외를 던진다")
        void process_notFound_throwsException() {
            // given
            Long outBoxId = 99L;
            given(historyOutBoxRepository.findByIdForUpdate(outBoxId)).willReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> historyOutboxService.process(outBoxId))
                    .isInstanceOf(BaseException.class)
                    .extracting("errorEnum")
                    .isEqualTo(ErrorEnum.HISTORY_OUTBOX_NOT_FOUND);

            verifyNoInteractions(transactionHistoryRepository);
        }

        @Test
        @DisplayName("이미 처리된 아웃박스는 조용히 스킵하고 TransactionHistory를 저장하지 않는다")
        void process_alreadyProcessed_skipsSilently() {
            // given
            Long outBoxId = 1L;
            HistoryOutbox outBox = HistoryOutbox.of(TransactionType.PAYMENT, "{}");
            ReflectionTestUtils.setField(outBox, "id", outBoxId);
            outBox.markProcessed();

            given(historyOutBoxRepository.findByIdForUpdate(outBoxId)).willReturn(Optional.of(outBox));

            // when
            historyOutboxService.process(outBoxId);

            // then
            verifyNoInteractions(transactionHistoryRepository);
            assertThat(outBox.getRetryCount()).isZero();
        }

        @Test
        @DisplayName("정상 처리 시 TransactionHistory를 저장하고 처리 완료로 표시한다")
        void process_success_savesHistoryAndMarksProcessed() throws JsonProcessingException {
            // given
            Long outBoxId = 1L;
            String payloadJson = "{\"transactionId\":10}";
            HistoryOutbox outBox = HistoryOutbox.of(TransactionType.PAYMENT, payloadJson);
            ReflectionTestUtils.setField(outBox, "id", outBoxId);

            HistoryOutBoxPayload payload = new HistoryOutBoxPayload(
                    10L, TransactionType.PAYMENT, PaymentStatus.SUCCESS, RefundStatus.NONE, new BigDecimal("10000")
            );

            given(historyOutBoxRepository.findByIdForUpdate(outBoxId)).willReturn(Optional.of(outBox));
            given(objectMapper.readValue(payloadJson, HistoryOutBoxPayload.class)).willReturn(payload);

            // when
            historyOutboxService.process(outBoxId);

            // then
            ArgumentCaptor<TransactionHistory> captor = ArgumentCaptor.forClass(TransactionHistory.class);
            verify(transactionHistoryRepository).saveAndFlush(captor.capture());

            TransactionHistory savedHistory = captor.getValue();
            assertThat(savedHistory.getTransactionId()).isEqualTo(10L);
            assertThat(savedHistory.getType()).isEqualTo(TransactionType.PAYMENT);
            assertThat(savedHistory.getPaymentStatus()).isEqualTo(PaymentStatus.SUCCESS);
            assertThat(savedHistory.getRefundStatus()).isEqualTo(RefundStatus.NONE);
            assertThat(savedHistory.getAmount()).isEqualByComparingTo("10000");

            assertThat(outBox.isProcessed()).isTrue();
            assertThat(outBox.getRetryCount()).isZero();
        }

        @Test
        @DisplayName("처리 중 예외가 발생하면 예외를 던지지 않고 재시도 카운트만 증가시킨다")
        void process_exceptionDuringProcessing_incrementsRetryCountWithoutThrowing() throws JsonProcessingException {
            // given
            Long outBoxId = 1L;
            String payloadJson = "invalid-json";
            HistoryOutbox outBox = HistoryOutbox.of(TransactionType.PAYMENT, payloadJson);
            ReflectionTestUtils.setField(outBox, "id", outBoxId);

            given(historyOutBoxRepository.findByIdForUpdate(outBoxId)).willReturn(Optional.of(outBox));
            given(objectMapper.readValue(payloadJson, HistoryOutBoxPayload.class))
                    .willThrow(new JsonProcessingException("invalid json") {});

            // when & then
            assertThatCode(() -> historyOutboxService.process(outBoxId))
                    .doesNotThrowAnyException();

            assertThat(outBox.getRetryCount()).isEqualTo(1);
            assertThat(outBox.isProcessed()).isFalse();
            verifyNoInteractions(transactionHistoryRepository);
        }
    }

    private Payment createPayment(Long id, BigDecimal amount, PaymentStatus status) {
        Order order = mock(Order.class);
        Payment payment = Payment.of(order, "merchant-" + id, amount, MethodEnum.MOCK);
        ReflectionTestUtils.setField(payment, "id", id);
        ReflectionTestUtils.setField(payment, "status", status);
        return payment;
    }

    private Refund createRefund(Long id, Payment payment) {
        Refund refund = Refund.of(mock(com.example.allinmarket.buyer.entity.Buyer.class), payment, ReasonEnum.CHANGE_OF_MIND, "설명");
        ReflectionTestUtils.setField(refund, "id", id);
        return refund;
    }
}
