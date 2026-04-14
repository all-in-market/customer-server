package com.example.allinmarket.domain.refund.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum ReasonEnum {
    CHANGE_OF_MIND("고객 단순 변심"),
    WRONG_ITEM("잘못된 상품"),
    DAMAGED("상품 손상"),
    PAYMENT_AMOUNT_MISMATCH("주문 금액과 실결제 금액이 상이");

    private final String reason;
}
