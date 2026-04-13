package com.example.allinmarket.buyer.payment.client.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.math.BigDecimal;

@JsonIgnoreProperties(ignoreUnknown = true)
public record PortOnePaymentResponse(
        String status,
        String id,
        String transactionId,
        String merchantId,
        String storeId,
        Method method,
        Channel channel,
        String version,
        String requestedAt,
        String updatedAt,
        String statusChangedAt,
        String orderName,
        Amount amount,
        String currency,
        Customer customer,
        String paidAt,
        String pgTxId
) {

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Method(
            String type,
            Card card
    ) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Card(
            String publisher,
            String issuer,
            String brand,
            String type,
            String ownerType,
            String bin,
            String name,
            String number
    ) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Channel(
            String id,
            String key,
            String name,
            String pgProvider,
            String type
    ) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Amount(
            BigDecimal total,
            BigDecimal taxFree,
            BigDecimal vat,
            BigDecimal supply,
            BigDecimal discount,
            BigDecimal paid,
            BigDecimal cancelled,
            BigDecimal cancelledTaxFree
    ) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Customer(
            String id,
            String name,
            String email,
            String phoneNumber
    ) {
    }

    public String getPaymentId() {
        return id;
    }

    public String getStatus() {
        return status;
    }

    public Amount getAmount() {
        return amount;
    }

    public BigDecimal getTotalAmount() {
        return amount != null ? amount.total() : null;
    }

    public boolean isPaid() {
        return "PAID".equalsIgnoreCase(status);
    }

    public boolean isCancelled() {
        return "CANCELLED".equalsIgnoreCase(status);
    }

    public boolean isFailed() {
        return "FAILED".equalsIgnoreCase(status);
    }
}