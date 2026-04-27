package com.example.allinmarket.common.outbox.service;

import com.example.allinmarket.common.enums.ErrorEnum;
import com.example.allinmarket.common.exception.BaseException;
import com.example.allinmarket.common.outbox.dto.HistoryOutBoxPayload;
import com.example.allinmarket.common.outbox.entity.HistoryOutBox;
import com.example.allinmarket.common.outbox.repository.HistoryOutBoxRepository;
import com.example.allinmarket.domain.payment.entity.Payment;
import com.example.allinmarket.domain.refund.entity.Refund;
import com.example.allinmarket.domain.transactionhistory.entity.TransactionHistory;
import com.example.allinmarket.domain.transactionhistory.enums.TransactionType;
import com.example.allinmarket.domain.transactionhistory.repository.TransactionHistoryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.fasterxml.jackson.databind.ObjectMapper;

@Slf4j
@Service
@RequiredArgsConstructor
public class HistoryOutBoxService {

    private final HistoryOutBoxRepository historyOutBoxRepository;
    private final TransactionHistoryRepository transactionHistoryRepository;
    private final ObjectMapper objectMapper;

    @Transactional
    public void save(Payment payment) {
        try {
            HistoryOutBoxPayload payload = HistoryOutBoxPayload.from(payment);
            String json = objectMapper.writeValueAsString(payload);
            historyOutBoxRepository.save(HistoryOutBox.of(TransactionType.PAYMENT, json));
            log.info("OutboxEvent 저장 성공: transactionId={}, type={}", payment.getId(), TransactionType.PAYMENT);
        } catch (Exception e) {
            log.error("OutboxEvent 저장 실패: transactionId={}, type={}", payment.getId(), TransactionType.PAYMENT, e);
        }
    }

    @Transactional
    public void save(Refund refund) {
        try {
            HistoryOutBoxPayload payload = HistoryOutBoxPayload.from(refund);
            String json = objectMapper.writeValueAsString(payload);
            historyOutBoxRepository.save(HistoryOutBox.of(TransactionType.REFUND, json));
            log.info("OutboxEvent 저장 성공: transactionId={}, type={}", refund.getId(), TransactionType.REFUND);
        } catch (Exception e) {
            log.error("OutboxEvent 저장 실패: transactionId={}, type={}", refund.getId(), TransactionType.REFUND, e);
        }
    }

    @Transactional
    public void process(Long outBoxId) {
        HistoryOutBox outBox = historyOutBoxRepository.findById(outBoxId)
                .orElseThrow(() -> new BaseException(ErrorEnum.HISTORY_OUTBOX_NOT_FOUND));

        try {
            HistoryOutBoxPayload payload = objectMapper.readValue(outBox.getPayload(), HistoryOutBoxPayload.class);
            TransactionHistory history = TransactionHistory.of(
                    payload.transactionId(),
                    payload.type(),
                    payload.paymentStatus(),
                    payload.refundStatus(),
                    payload.amount()
            );
            transactionHistoryRepository.save(history);
            outBox.markProcessed();
            log.info("OutboxEvent 처리 성공: outboxId={}", outBox.getId());
        } catch (Exception e) {
            outBox.incrementRetryCount();
            log.error("OutboxEvent 처리 실패: outboxId={}, retryCount={}", outBox.getId(), outBox.getRetryCount(), e);
        }
    }
}
