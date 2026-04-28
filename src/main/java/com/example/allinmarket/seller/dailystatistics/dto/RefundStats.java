package com.example.allinmarket.seller.dailystatistics.dto;

import java.math.BigDecimal;

public record RefundStats (
        Long totalRefunds,
        BigDecimal refundAmount
) {
}
