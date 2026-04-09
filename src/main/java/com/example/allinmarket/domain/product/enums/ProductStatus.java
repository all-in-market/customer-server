package com.example.allinmarket.domain.product.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum ProductStatus {
    ON_SALE("ON_SALE"),
    HIDDEN("HIDDEN");

    private final String status;
}

