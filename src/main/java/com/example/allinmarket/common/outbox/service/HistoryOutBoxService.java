package com.example.allinmarket.common.outbox.service;

import com.example.allinmarket.common.enums.ErrorEnum;
import com.example.allinmarket.common.exception.BaseException;
import com.example.allinmarket.common.outbox.entity.HistoryOutBox;
import com.example.allinmarket.common.outbox.repository.HistoryOutBoxRepository;
import com.example.allinmarket.domain.payment.entity.Payment;
import com.example.allinmarket.domain.payment.repository.PaymentRepository;
import com.example.allinmarket.domain.refund.entity.Refund;
import com.example.allinmarket.domain.refund.repository.RefundRepository;
import com.example.allinmarket.domain.transactionhistory.entity.TransactionHistory;
import com.example.allinmarket.domain.transactionhistory.enums.TransactionType;
import com.example.allinmarket.domain.transactionhistory.repository.TransactionHistoryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class HistoryOutBoxService {

    private final HistoryOutBoxRepository historyOutBoxRepository;
    private final PaymentRepository paymentRepository;
    private final RefundRepository refundRepository;
    private final TransactionHistoryRepository transactionHistoryRepository;

    @Transactional
    public void save(Long transactionId, TransactionType type) {
        HistoryOutBox outBox = HistoryOutBox.of(transactionId, type);
        historyOutBoxRepository.save(outBox);
        log.info("OutboxEvent 저장 성공: transactionId={}, type={}", transactionId, type);
    }

    private TransactionHistory createTransactionHistory(HistoryOutBox outBox) {
        return switch (outBox.getType()) {
            case PAYMENT -> {
                Payment payment = paymentRepository.findById(outBox.getTransactionId())
                        .orElseThrow(() -> new BaseException(ErrorEnum.PAYMENT_NOT_FOUND));
                yield TransactionHistory.of(payment);
            }
            case REFUND -> {
                Refund refund = refundRepository.findById(outBox.getTransactionId())
                        .orElseThrow(() -> new BaseException(ErrorEnum.REFUND_NOT_FOUND));
                yield TransactionHistory.of(refund);
            }
        };
    }

    @Transactional
    public void process(HistoryOutBox outBox) {
        try {
            TransactionHistory history = createTransactionHistory(outBox);
            transactionHistoryRepository.save(history);
            outBox.markProcessed();
            log.info("OutboxEvent 처리 성공: outboxId={}", outBox.getId());
        } catch (Exception e) {
            outBox.incrementRetryCount();
            log.error("OutboxEvent 처리 실패: outboxId={}, retryCount={}", outBox.getId(), outBox.getRetryCount(), e);
        }
    }
}
