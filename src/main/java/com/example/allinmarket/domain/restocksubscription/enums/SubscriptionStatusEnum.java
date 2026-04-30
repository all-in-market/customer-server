package com.example.allinmarket.domain.restocksubscription.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum SubscriptionStatusEnum {
    ACTIVE("발송 대기 중"),
    SENT("발송 완료"),
    CANCELLED("고객 취소"),
    EXPIRED("일정 기간 경과 후 만료");

    public final String description;
}
