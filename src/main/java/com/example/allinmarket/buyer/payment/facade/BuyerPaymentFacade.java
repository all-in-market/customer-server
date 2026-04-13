package com.example.allinmarket.buyer.payment.facade;

import com.example.allinmarket.buyer.payment.client.PaymentGateway;
import com.example.allinmarket.buyer.payment.client.dto.PortOnePaymentResponse;
import com.example.allinmarket.buyer.payment.dto.request.PaymentCreateRequest;
import com.example.allinmarket.buyer.payment.dto.response.PaymentDetailResponse;
import com.example.allinmarket.buyer.payment.service.BuyerPaymentService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class BuyerPaymentFacade {

    private final BuyerPaymentService buyerPaymentService;
    private final PaymentGateway paymentGateway;

    public PaymentDetailResponse processPayment(Long currentUserId, PaymentCreateRequest request) {
        PaymentDetailResponse paymentCreateResult = buyerPaymentService.createPayment(currentUserId, request);

        // 결제가 이 부분에서 이루어졌다고 가정
        // 실제 결제는 FE 에서 결제창을 호출하여 실행

        // 결제 이력 조회
        PortOnePaymentResponse payment = paymentGateway.getPayment(paymentCreateResult.impUid());

        return buyerPaymentService.confirmPayment(currentUserId, paymentCreateResult.impUid(), payment);
    }
}
