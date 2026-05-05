package com.example.allinmarket.domain.restocksubscription.event;

import com.example.allinmarket.domain.restocksubscription.dto.RestockEventRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

@Component
@RequiredArgsConstructor
@Slf4j
public class RestockEventListener {

    private final RestClient restClient;

    @Value("${notification-server.url}")
    private String notificationServerUrl;

    @Retryable(
            retryFor = RestClientException.class,
            maxAttempts = 4,
            backoff = @Backoff(multiplier = 2, random = true)
    )
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleRestockEvent(RestockEvent event) {
        try{
            restClient.post()
                    .uri(notificationServerUrl + "/internal/notifications/restock")
                    .body(new RestockEventRequest(event.getProductId()))
                    .retrieve()
                    .toBodilessEntity();
        } catch (RestClientException e) {
            log.warn("재입고 알림 전송 실패. productId={}", event.getProductId(), e);
        }

    }
}
