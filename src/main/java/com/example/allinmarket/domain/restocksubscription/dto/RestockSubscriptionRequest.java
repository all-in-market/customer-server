package com.example.allinmarket.domain.restocksubscription.dto;

import jakarta.validation.constraints.NotNull;

public record RestockSubscriptionRequest(
        @NotNull(message = "상품이 선택되지 않았습니다.")
        Long productId) {}
