package com.example.allinmarket.common.outbox.service;

import com.example.allinmarket.common.enums.ErrorEnum;
import com.example.allinmarket.common.exception.BaseException;
import com.example.allinmarket.common.outbox.dto.HistoryOutBoxPayload;
import com.example.allinmarket.common.outbox.entity.HistoryOutbox;
import com.example.allinmarket.common.outbox.repository.HistoryOutBoxRepository;
import com.example.allinmarket.domain.payment.entity.Payment;
import com.example.allinmarket.domain.refund.entity.Refund;
import com.example.allinmarket.domain.transactionhistory.entity.TransactionHistory;
import com.example.allinmarket.domain.transactionhistory.enums.TransactionType;
import com.example.allinmarket.domain.transactionhistory.repository.TransactionHistoryRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class HistoryOutBoxService {

    private final HistoryOutBoxRepository historyOutBoxRepository;
    private final TransactionHistoryRepository transactionHistoryRepository;
    private final ObjectMapper objectMapper;

    public void save(Payment payment) {
        String json;
        try {
            HistoryOutBoxPayload payload = HistoryOutBoxPayload.from(payment);
            json = objectMapper.writeValueAsString(payload);
        } catch (JsonProcessingException e) {
            throw new BaseException(ErrorEnum.PAYLOAD_SERIALIZATION_FAILED);
        }
        historyOutBoxRepository.save(HistoryOutbox.of(TransactionType.PAYMENT, json));
        log.info("OutboxEvent 저장 성공: transactionId={}, type={}", payment.getId(), TransactionType.PAYMENT);
    }

    public void save(Refund refund) {
        String json;
        try {
            HistoryOutBoxPayload payload = HistoryOutBoxPayload.from(refund);
            json = objectMapper.writeValueAsString(payload);
        } catch (JsonProcessingException e) {
            throw new BaseException(ErrorEnum.PAYLOAD_SERIALIZATION_FAILED);
        }
        historyOutBoxRepository.save(HistoryOutbox.of(TransactionType.REFUND, json));
        log.info("OutboxEvent 저장 성공: transactionId={}, type={}", refund.getId(), TransactionType.REFUND);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void process(Long outBoxId) {
        HistoryOutbox outBox = historyOutBoxRepository.findByIdForUpdate(outBoxId)
                .orElseThrow(() -> new BaseException(ErrorEnum.HISTORY_OUTBOX_NOT_FOUND));

        if (outBox.isProcessed()) {
            log.debug("이미 처리된 OutboxEvent 스킵: outboxId={}", outBoxId);
            return;
        }

        try {
            HistoryOutBoxPayload payload = objectMapper.readValue(outBox.getPayload(), HistoryOutBoxPayload.class);
            TransactionHistory history = TransactionHistory.of(
                    payload.transactionId(),
                    payload.type(),
                    payload.paymentStatus(),
                    payload.refundStatus(),
                    payload.amount()
            );
            transactionHistoryRepository.saveAndFlush(history);
            outBox.markProcessed();
            log.info("OutboxEvent 처리 성공: outboxId={}", outBox.getId());
        } catch (Exception e) {
            outBox.incrementRetryCount();
            log.error("OutboxEvent 처리 실패: outboxId={}, retryCount={}", outBox.getId(), outBox.getRetryCount(), e);
        }
    }
}
