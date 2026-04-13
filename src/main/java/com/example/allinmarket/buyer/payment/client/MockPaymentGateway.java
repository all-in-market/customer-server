package com.example.allinmarket.buyer.payment.client;

import com.example.allinmarket.buyer.payment.client.dto.PortOnePaymentResponse;
import com.example.allinmarket.common.enums.ErrorEnum;
import com.example.allinmarket.common.exception.BaseException;
import com.example.allinmarket.domain.payment.entity.Payment;
import com.example.allinmarket.domain.payment.repository.PaymentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Component
@RequiredArgsConstructor
public class MockPaymentGateway implements PaymentGateway {

    private final PaymentRepository paymentRepository;

    /**
     * isPaid 가 항상 true가 나오는 PortOnePaymentResponse 객체 반환
     */
    @Override
    public PortOnePaymentResponse getPayment(String paymentId) {

        Payment payment = paymentRepository.findByImpUid(paymentId).orElseThrow(
                () -> new BaseException(ErrorEnum.PAYMENT_NOT_FOUND)
        );

        BigDecimal amount = payment.getAmount();

        PortOnePaymentResponse.Amount responseAmount =
                new PortOnePaymentResponse.Amount(
                        amount,
                        BigDecimal.ZERO,
                        BigDecimal.ZERO,
                        BigDecimal.ZERO,
                        BigDecimal.ZERO,
                        amount,
                        BigDecimal.ZERO,
                        BigDecimal.ZERO
                );

        return new PortOnePaymentResponse(
                "PAID",
                paymentId,
                "tx_" + paymentId,
                "merchant_mock",
                "store_mock",
                null,
                null,
                "v1",
                now(),
                now(),
                now(),
                "mock order",
                responseAmount,
                "KRW",
                null,
                now(),
                "pg_tx_mock"
        );
    }

    private String now() {
        return String.valueOf(System.currentTimeMillis());
    }
}
