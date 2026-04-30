package com.example.allinmarket.domain.restocksubscription.dto;

import com.example.allinmarket.domain.restocksubscription.enums.SubscriptionStatusEnum;

public record RestockSubscriptionDetailResponse(
        Long productId,
        SubscriptionStatusEnum status,
        String message) {}
