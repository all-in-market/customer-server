package com.example.allinmarket.domain.restocksubscription.dto;

import com.example.allinmarket.domain.restocksubscription.entity.RestockSubscription;
import com.example.allinmarket.domain.restocksubscription.enums.SubscriptionStatusEnum;

public record RestockSubscriptionDetailResponse(
        Long productId,
        SubscriptionStatusEnum status,
        String message) {

    public static RestockSubscriptionDetailResponse from(RestockSubscription subscription) {
        return new RestockSubscriptionDetailResponse(
                subscription.getProductId(),
                subscription.getStatus(),
                "재입고 알림이 신청되었습니다.");
    }
}
