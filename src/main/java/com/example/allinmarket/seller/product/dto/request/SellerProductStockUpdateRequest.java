package com.example.allinmarket.seller.product.dto.request;

import jakarta.validation.constraints.PositiveOrZero;

public record SellerProductStockUpdateRequest(
        @PositiveOrZero(message = "재고는 0 이상이어야 합니다")
        int stock
) {
}
