package com.example.allinmarket.buyer.refund.dto.request;

import com.example.allinmarket.domain.refund.enums.ReasonEnum;
import jakarta.validation.constraints.NotNull;

public record RefundCreateRequest(

        @NotNull(message = "환불 사유는 필수입니다.")
        ReasonEnum reason,

        String description
) {
}
