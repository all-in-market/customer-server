package com.example.allinmarket.domain.transactionhistory.service;

import com.example.allinmarket.domain.payment.entity.Payment;
import com.example.allinmarket.domain.refund.entity.Refund;
import com.example.allinmarket.domain.transactionhistory.entity.TransactionHistory;
import com.example.allinmarket.domain.transactionhistory.repository.TransactionHistoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class TransactionHistoryService {

    private final TransactionHistoryRepository transactionHistoryRepository;

    public void savePaymentHistory(Payment payment) {
        TransactionHistory transactionHistory = TransactionHistory.of(payment);
        transactionHistoryRepository.save(transactionHistory);
    }

    public void saveRefundHistory(Refund refund) {
        TransactionHistory transactionHistory = TransactionHistory.of(refund);
        transactionHistoryRepository.save(transactionHistory);
    }
}
