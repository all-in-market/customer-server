package com.example.allinmarket.buyer.payment.client;

import com.example.allinmarket.buyer.payment.client.dto.PortOnePaymentResponse;

public interface PaymentGateway {
    PortOnePaymentResponse getPayment(String paymentId);
}
