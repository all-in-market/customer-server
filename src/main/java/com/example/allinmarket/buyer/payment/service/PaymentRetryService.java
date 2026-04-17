package com.example.allinmarket.buyer.payment.service;

import com.example.allinmarket.buyer.payment.client.dto.PortOnePaymentResponse;
import com.example.allinmarket.buyer.payment.dto.response.PaymentDetailResponse;
import com.example.allinmarket.common.enums.ErrorEnum;
import com.example.allinmarket.common.exception.BaseException;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Recover;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class PaymentRetryService {

    private final BuyerPaymentService buyerPaymentService;

    @Retryable(retryFor = OptimisticLockingFailureException.class, maxAttempts = 3, backoff = @Backoff(delay = 100, multiplier = 2))
    public PaymentDetailResponse retryConfirmPayment(Long currentUserId, String paymentId, PortOnePaymentResponse payment) {
        return buyerPaymentService.confirmPayment(currentUserId, paymentId, payment);
    }

    // 지정된 횟수만큼 시도 후 모두 실패하면 전파된 OptimisticLockingFailureException을 받아 처리하여 500 응답 대신 지정된 에러 응답 반환
    @Recover
    public PaymentDetailResponse recoverConfirmPayment(OptimisticLockingFailureException e, Long currentUserId, String paymentId,
                                                       PortOnePaymentResponse payment) {
        throw new BaseException(ErrorEnum.PAYMENT_FAILED); // 적절한 에러 응답
    }

}
