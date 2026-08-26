package com.example.allinmarket.buyer.payment.service;

import com.example.allinmarket.buyer.entity.Buyer;
import com.example.allinmarket.buyer.payment.client.dto.PortOnePaymentResponse;
import com.example.allinmarket.buyer.payment.dto.response.PaymentDetailResponse;
import com.example.allinmarket.common.enums.ErrorEnum;
import com.example.allinmarket.common.exception.BaseException;
import com.example.allinmarket.domain.order.entity.Order;
import com.example.allinmarket.domain.payment.entity.Payment;
import com.example.allinmarket.domain.payment.enums.MethodEnum;
import com.example.allinmarket.domain.payment.repository.PaymentRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

@ExtendWith(MockitoExtension.class)
class PaymentRetryServiceTest {

    @Mock
    private BuyerPaymentService buyerPaymentService;

    @Mock
    private PaymentRepository paymentRepository;

    @Mock
    private PaymentStateService paymentStateService;

    @InjectMocks
    private PaymentRetryService paymentRetryService;

    private static final Long CURRENT_USER_ID = 1L;
    private static final String PAYMENT_ID = "merchant_uid_1";

    @Nested
    @DisplayName("결제 승인 재시도")
    class RetryConfirmPaymentTest {

        @Test
        @DisplayName("buyerPaymentService.confirmPayment에 그대로 위임하고 결과를 반환한다")
        void retryConfirmPayment_delegatesToConfirmPaymentAndReturnsResult() throws JsonProcessingException {
            // given
            PortOnePaymentResponse payment = mock(PortOnePaymentResponse.class);
            PaymentDetailResponse expected = mock(PaymentDetailResponse.class);

            given(buyerPaymentService.confirmPayment(CURRENT_USER_ID, PAYMENT_ID, payment))
                    .willReturn(expected);

            // when
            PaymentDetailResponse result = paymentRetryService.retryConfirmPayment(CURRENT_USER_ID, PAYMENT_ID, payment);

            // then
            assertThat(result).isEqualTo(expected);
            verify(buyerPaymentService).confirmPayment(CURRENT_USER_ID, PAYMENT_ID, payment);
        }
    }

    @Nested
    @DisplayName("결제 승인 재시도 실패 복구")
    class RecoverConfirmPaymentTest {

        @Test
        @DisplayName("결제 내역이 없으면 예외를 던진다")
        void recoverConfirmPayment_whenPaymentNotFound_thenThrowPaymentNotFound() {
            // given
            OptimisticLockingFailureException exception = new OptimisticLockingFailureException("optimistic lock fail");
            PortOnePaymentResponse payment = mock(PortOnePaymentResponse.class);

            given(paymentRepository.findByMerchantUidWithOrder(PAYMENT_ID)).willReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() ->
                    paymentRetryService.recoverConfirmPayment(exception, CURRENT_USER_ID, PAYMENT_ID, payment))
                    .isInstanceOf(BaseException.class)
                    .extracting("errorEnum")
                    .isEqualTo(ErrorEnum.PAYMENT_NOT_FOUND);

            verifyNoInteractions(paymentStateService);
        }

        @Test
        @DisplayName("결제 내역이 있으면 실패 처리 후 PAYMENT_FAILED 예외를 던진다")
        void recoverConfirmPayment_whenPaymentExists_thenFailAndSaveHistoryThenThrowPaymentFailed() {
            // given
            OptimisticLockingFailureException exception = new OptimisticLockingFailureException("optimistic lock fail");
            PortOnePaymentResponse payment = mock(PortOnePaymentResponse.class);

            Payment dbPayment = createPayment(PAYMENT_ID);

            given(paymentRepository.findByMerchantUidWithOrder(PAYMENT_ID)).willReturn(Optional.of(dbPayment));

            // when & then
            assertThatThrownBy(() ->
                    paymentRetryService.recoverConfirmPayment(exception, CURRENT_USER_ID, PAYMENT_ID, payment))
                    .isInstanceOf(BaseException.class)
                    .extracting("errorEnum")
                    .isEqualTo(ErrorEnum.PAYMENT_FAILED);

            verify(paymentStateService).failAndSaveHistory(dbPayment);
        }
    }

    private Payment createPayment(String merchantUid) {
        Buyer buyer = mock(Buyer.class);
        Order order = Order.of(buyer, BigDecimal.TEN, null, "홍길동", "010-0000-0000", "서울시 강남구");
        ReflectionTestUtils.setField(order, "id", 1L);

        Payment payment = Payment.of(order, merchantUid, BigDecimal.TEN, MethodEnum.MOCK);
        ReflectionTestUtils.setField(payment, "id", 1L);
        return payment;
    }
}
