package com.example.allinmarket.domain.transactionhistory.service;

import com.example.allinmarket.domain.payment.entity.Payment;
import com.example.allinmarket.domain.refund.entity.Refund;
import com.example.allinmarket.domain.transactionhistory.entity.TransactionHistory;
import com.example.allinmarket.domain.transactionhistory.repository.TransactionHistoryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(propagation = Propagation.REQUIRES_NEW)
public class TransactionHistoryService {

    private final TransactionHistoryRepository transactionHistoryRepository;

    public void savePaymentHistory(Payment payment) {
            TransactionHistory transactionHistory = TransactionHistory.of(payment);
            transactionHistoryRepository.saveAndFlush(transactionHistory);
            log.info("결제 생성 이력 저장 성공: paymentId = {}", payment.getId());
    }

    public void saveRefundHistory(Refund refund) {
            TransactionHistory transactionHistory = TransactionHistory.of(refund);
            transactionHistoryRepository.saveAndFlush(transactionHistory);
            log.info("환불 생성 이력 저장 성공: refundId = {}", refund.getId());
    }
}
