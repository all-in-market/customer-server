package com.example.allinmarket.domain.restocksubscription.event;

import com.example.allinmarket.domain.restocksubscription.dto.RestockEventRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;
import org.springframework.web.client.RestClient;

@Component
@RequiredArgsConstructor
public class RestockEventListener {

    private final RestClient restClient;

    @Value("${notification-server.url}")
    private String notificationServerUrl;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleRestockEvent(RestockEvent event) {
        restClient.post()
                .uri(notificationServerUrl + "/internal/notifications/restock")
                .body(new RestockEventRequest(event.getProductId()))
                .retrieve()
                .toBodilessEntity();
    }
}
