package com.example.allinmarket.domain.restocksubscription.event;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public class RestockEvent {
    private final Long productId;
}
