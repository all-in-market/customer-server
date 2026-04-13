package com.example.allinmarket.buyer.payment.dto.request;

import com.example.allinmarket.domain.payment.enums.MethodEnum;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record PaymentCreateRequest(

        @NotNull(message = "주문 ID는 필수입니다")
        @Positive(message = "주문 ID는 양수여야 합니다")
        Long orderId,

        @NotNull(message = "결제 수단 선택은 필수입니다")
        MethodEnum method
) {
}
