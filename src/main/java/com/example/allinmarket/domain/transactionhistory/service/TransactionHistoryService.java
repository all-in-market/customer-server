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
        try{
            TransactionHistory transactionHistory = TransactionHistory.of(payment);
            transactionHistoryRepository.saveAndFlush(transactionHistory);
            log.info("결제 생성 이력 저장 성공: paymentId = {}", payment.getId());
        } catch(Exception e) {
            log.error("결제 생성 이력 저장 실패: paymentId = {}", payment.getId(), e);
        }
    }

    public void saveRefundHistory(Refund refund) {
        try{
            TransactionHistory transactionHistory = TransactionHistory.of(refund);
            transactionHistoryRepository.saveAndFlush(transactionHistory);
            log.info("환불 생성 이력 저장 성공: refundId = {}", refund.getId());
        } catch(Exception e) {
            log.error("환불 생성 이력 저장 실패 : refundId = {}", refund.getId(),e);
        }

    }
}
