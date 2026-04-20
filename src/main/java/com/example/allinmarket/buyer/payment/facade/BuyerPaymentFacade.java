package com.example.allinmarket.buyer.payment.facade;

import com.example.allinmarket.buyer.payment.client.PaymentGateway;
import com.example.allinmarket.buyer.payment.client.dto.PortOnePaymentResponse;
import com.example.allinmarket.buyer.payment.dto.request.PaymentCreateRequest;
import com.example.allinmarket.buyer.payment.dto.response.PaymentDetailResponse;
import com.example.allinmarket.buyer.payment.service.BuyerPaymentService;
import com.example.allinmarket.buyer.payment.service.PaymentRetryService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class BuyerPaymentFacade {

    private final BuyerPaymentService buyerPaymentService;
    private final PaymentRetryService paymentRetryService;
    private final PaymentGateway paymentGateway;

    public PaymentDetailResponse processPayment(Long currentUserId, PaymentCreateRequest request) {
        PaymentDetailResponse paymentCreateResult = buyerPaymentService.createPayment(currentUserId, request);

        // 결제가 이 부분에서 이루어졌다고 가정
        // 실제 결제는 FE 에서 결제창을 호출하여 실행

        // 결제 이력 조회 (지금은 연동 전이므로 항상 결제 완료 상태를 반환한다고 가정)
        PortOnePaymentResponse payment = paymentGateway.getPayment(paymentCreateResult.impUid());

        return paymentRetryService.retryConfirmPayment(currentUserId, paymentCreateResult.impUid(), payment);
    }
}
