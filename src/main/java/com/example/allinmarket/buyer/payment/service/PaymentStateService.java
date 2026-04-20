package com.example.allinmarket.buyer.payment.service;

import com.example.allinmarket.domain.payment.entity.Payment;
import com.example.allinmarket.domain.payment.repository.PaymentRepository;
import com.example.allinmarket.domain.transactionhistory.service.TransactionHistoryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class PaymentStateService {

    private final TransactionHistoryService transactionHistoryService;
    private final PaymentRepository paymentRepository;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void failAndSaveHistory(Payment payment) {
        payment.fail();
        paymentRepository.saveAndFlush(payment);
        log.info("결제 승인 실패: paymentId = {}", payment.getId());

        try {
            transactionHistoryService.savePaymentHistory(payment);
            log.info("결제 실패 이력 저장 성공: paymentId = {}", payment.getId());
        } catch (Exception e) {
            log.error("결제 실패 이력 저장 실패: paymentId = {}, reason = {}", payment.getId(), e.getMessage());
        }
    }
}
