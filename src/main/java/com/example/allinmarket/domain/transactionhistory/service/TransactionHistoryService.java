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
public class TransactionHistoryService {

    private final TransactionHistoryRepository transactionHistoryRepository;

    // 실패 시 상위 메서드인 confirmPayment() 까지 롤백 (데이터 정합성)
    @Transactional
    public void saveSucceededHistory(Payment succeededPayment) {
        TransactionHistory transactionHistory = TransactionHistory.of(succeededPayment);
        transactionHistoryRepository.save(transactionHistory);
    }

    // 별도의 트랜잭션에서 실행하여, 실패하더라도 상위 메서드까지 롤백시키지 않음
    // 대신 상위 메서드에서 try-catch로 잡아 로그를 남김
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void saveFailedHistory(Payment failedPayment) {
        TransactionHistory transactionHistory = TransactionHistory.of(failedPayment);
        transactionHistoryRepository.save(transactionHistory);
    }

    @Transactional
    public void saveRefundHistory(Refund refund) {
        TransactionHistory transactionHistory = TransactionHistory.of(refund);
        transactionHistoryRepository.save(transactionHistory);
    }
}
