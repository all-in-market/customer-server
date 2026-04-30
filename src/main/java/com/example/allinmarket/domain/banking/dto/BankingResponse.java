package com.example.allinmarket.domain.banking.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.math.BigDecimal;

@JsonIgnoreProperties(ignoreUnknown = true)
public record BankingResponse(
        boolean isSuccess,
        String transactionId,
        String payoutKey,
        BigDecimal amount,
        String errorMessage
) {

    public String getPayoutKey() {
        return payoutKey;
    }

    public BigDecimal getAmount() {
        return amount;
    }
}