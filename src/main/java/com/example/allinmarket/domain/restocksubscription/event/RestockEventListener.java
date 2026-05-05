package com.example.allinmarket.domain.restocksubscription.event;

import com.example.allinmarket.common.security.HmacSigner;
import com.example.allinmarket.domain.restocksubscription.dto.RestockEventRequest;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.time.Instant;
import java.util.UUID;

@Component
@RequiredArgsConstructor
@Slf4j
public class RestockEventListener {

    private final RestClient restClient;
    private final ObjectMapper objectMapper;

    @Value("${notification-server.url}")
    private String notificationServerUrl;

    @Value("${notification.auth.client-id}")
    private String clientId;

    @Value("${notification.auth.secret}")
    private String secret;

    @Retryable(
            retryFor = RestClientException.class,
            maxAttempts = 4,
            backoff = @Backoff(multiplier = 2, random = true)
    )
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleRestockEvent(RestockEvent event) {
        try {
            RestockEventRequest request = new RestockEventRequest(event.getProductId());

            String body = objectMapper.writeValueAsString(request);

            String timestamp = String.valueOf(Instant.now().getEpochSecond());
            String requestId = UUID.randomUUID().toString();

            String signature = HmacSigner.sign(
                    secret,
                    timestamp + requestId + body
            );


            restClient.post()
                    .uri(notificationServerUrl + "/internal/notifications/restock")
                    .header("X-Client-Id", clientId)
                    .header("X-Timestamp", timestamp)
                    .header("X-Request-Id", requestId)
                    .header("X-Signature", signature)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(body)
                    .retrieve()
                    .toBodilessEntity();
        } catch (RestClientException e) {
            log.error("재입고 알림 전송 실패. productId={}", event.getProductId(), e);
            throw e;
        } catch (JsonProcessingException e) {
            log.error("재입고 이벤트 직렬화 실패. productId = {}", event.getProductId(), e);
        }
    }
}
