package com.example.allinmarket.buyer.payment.facade;

import com.example.allinmarket.buyer.payment.client.PaymentGateway;
import com.example.allinmarket.buyer.payment.client.dto.PortOnePaymentResponse;
import com.example.allinmarket.buyer.payment.dto.request.PaymentCreateRequest;
import com.example.allinmarket.buyer.payment.dto.response.PaymentDetailResponse;
import com.example.allinmarket.buyer.payment.service.BuyerPaymentService;
import com.example.allinmarket.buyer.payment.service.PaymentRetryService;
import com.fasterxml.jackson.core.JsonProcessingException;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

@Service
@RequiredArgsConstructor
public class BuyerPaymentFacade {

    private final BuyerPaymentService buyerPaymentService;
    private final PaymentRetryService paymentRetryService;
    private final PaymentGateway paymentGateway;

    @Value("${payment.mock.latency.enabled:false}")
    private boolean mockLatencyEnabled;

    public PaymentDetailResponse processPayment(Long currentUserId, PaymentCreateRequest request) throws JsonProcessingException {
        PaymentDetailResponse paymentCreateResult = buyerPaymentService.createPayment(currentUserId, request);

        // 실제 결제는 FE 에서 결제창을 호출하여 실행
        // 결제 이력 조회. 실연동 시 PortOne이 생성한 impUid를 별도로 받아야 함.
        String impUid = paymentCreateResult.merchantUid() + UUID.randomUUID();

        PortOnePaymentResponse payment = paymentGateway.getPayment(paymentCreateResult.merchantUid(), impUid);
        if (mockLatencyEnabled) {
            simulateExternalLatency();
        }

        return paymentRetryService.retryConfirmPayment(currentUserId, paymentCreateResult.merchantUid(), payment);
    }

    private void simulateExternalLatency() {
        try {
            Thread.sleep(
                    ThreadLocalRandom.current()
                            .nextLong(300, 700) // 응답 지연 시간 300~700ms 사이 랜덤하게 지정
            );
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException(e);
        }
    }
}
