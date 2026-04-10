package com.example.allinmarket.buyer.order.dto.request;

import jakarta.validation.constraints.*;

import java.util.List;

public record OrderCreateRequest(
        @NotEmpty(message = "주문할 장바구니 상품은 최소 1개 이상이어야 합니다")
        List<Long> cartItemIds,

        @NotNull(message = "배송지 ID는 필수입니다")
        @Positive(message = "배송지 ID는 양수여야 합니다")
        Long addressId
) {
}