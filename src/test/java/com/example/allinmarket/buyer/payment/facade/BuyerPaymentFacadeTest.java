package com.example.allinmarket.buyer.payment.facade;

import com.example.allinmarket.buyer.payment.client.PaymentGateway;
import com.example.allinmarket.buyer.payment.client.dto.PortOnePaymentResponse;
import com.example.allinmarket.buyer.payment.dto.request.PaymentCreateRequest;
import com.example.allinmarket.buyer.payment.dto.response.PaymentDetailResponse;
import com.example.allinmarket.buyer.payment.service.BuyerPaymentService;
import com.example.allinmarket.buyer.payment.service.PaymentRetryService;
import com.example.allinmarket.domain.payment.enums.MethodEnum;
import com.example.allinmarket.domain.payment.enums.PaymentStatus;
import com.fasterxml.jackson.core.JsonProcessingException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class BuyerPaymentFacadeTest {

    @Mock
    private BuyerPaymentService buyerPaymentService;

    @Mock
    private PaymentRetryService paymentRetryService;

    @Mock
    private PaymentGateway paymentGateway;

    @InjectMocks
    private BuyerPaymentFacade buyerPaymentFacade;

    private static final Long CURRENT_USER_ID = 1L;

    @Nested
    @DisplayName("결제 처리")
    class ProcessPaymentTest {

        @Test
        @DisplayName("결제 생성 -> PG 조회 -> 재시도 승인 순서로 위임하고 최종 결과를 반환한다")
        void processPayment_delegatesInOrderAndReturnsRetryConfirmResult() throws JsonProcessingException {
            // given
            ReflectionTestUtils.setField(buyerPaymentFacade, "mockLatencyEnabled", false);

            PaymentCreateRequest request = new PaymentCreateRequest(1L, MethodEnum.MOCK);
            String merchantUid = "payment_1_abcd";

            PaymentDetailResponse createResult = new PaymentDetailResponse(
                    1L, 1L, merchantUid, null, BigDecimal.TEN, MethodEnum.MOCK, PaymentStatus.PENDING, null
            );

            PortOnePaymentResponse pgResponse = mock(PortOnePaymentResponse.class);

            PaymentDetailResponse finalResult = new PaymentDetailResponse(
                    1L, 1L, merchantUid, "imp_uid_1", BigDecimal.TEN, MethodEnum.MOCK, PaymentStatus.SUCCESS, null
            );

            given(buyerPaymentService.createPayment(CURRENT_USER_ID, request)).willReturn(createResult);
            given(paymentGateway.getPayment(eq(merchantUid), anyString())).willReturn(pgResponse);
            given(paymentRetryService.retryConfirmPayment(CURRENT_USER_ID, merchantUid, pgResponse))
                    .willReturn(finalResult);

            // when
            PaymentDetailResponse result = buyerPaymentFacade.processPayment(CURRENT_USER_ID, request);

            // then
            InOrder inOrder = inOrder(buyerPaymentService, paymentGateway, paymentRetryService);
            inOrder.verify(buyerPaymentService).createPayment(CURRENT_USER_ID, request);
            inOrder.verify(paymentGateway).getPayment(eq(merchantUid), anyString());
            inOrder.verify(paymentRetryService).retryConfirmPayment(CURRENT_USER_ID, merchantUid, pgResponse);

            assertThat(result).isEqualTo(finalResult);
        }

        @Test
        @DisplayName("paymentGateway에 전달되는 impUid는 merchantUid로 시작한다")
        void processPayment_impUidPassedToGateway_startsWithMerchantUid() throws JsonProcessingException {
            // given
            ReflectionTestUtils.setField(buyerPaymentFacade, "mockLatencyEnabled", false);

            PaymentCreateRequest request = new PaymentCreateRequest(1L, MethodEnum.MOCK);
            String merchantUid = "payment_1_abcd";

            PaymentDetailResponse createResult = new PaymentDetailResponse(
                    1L, 1L, merchantUid, null, BigDecimal.TEN, MethodEnum.MOCK, PaymentStatus.PENDING, null
            );

            PortOnePaymentResponse pgResponse = mock(PortOnePaymentResponse.class);
            PaymentDetailResponse finalResult = mock(PaymentDetailResponse.class);

            given(buyerPaymentService.createPayment(CURRENT_USER_ID, request)).willReturn(createResult);
            given(paymentGateway.getPayment(anyString(), anyString())).willReturn(pgResponse);
            given(paymentRetryService.retryConfirmPayment(eq(CURRENT_USER_ID), eq(merchantUid), any()))
                    .willReturn(finalResult);

            // when
            buyerPaymentFacade.processPayment(CURRENT_USER_ID, request);

            // then
            ArgumentCaptor<String> impUidCaptor = ArgumentCaptor.forClass(String.class);
            verify(paymentGateway).getPayment(eq(merchantUid), impUidCaptor.capture());

            assertThat(impUidCaptor.getValue()).startsWith(merchantUid);
            assertThat(impUidCaptor.getValue()).hasSizeGreaterThan(merchantUid.length());
        }
    }
}
