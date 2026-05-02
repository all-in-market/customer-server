package com.example.allinmarket.domain.restocksubscription.event;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
@RequiredArgsConstructor
public class RestockEventListener {

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleRestockEvent(RestockEvent event) {
        // TODO: 아래 sout 삭제
        System.out.println("재입고 이벤트 발생: productId = " + event.getProductId());
    }
}
