package com.example.allinmarket.common.outbox.payload;


import java.time.LocalDate;

public record DashboardUpdatePayload(
        Long orderId,
        LocalDate statDate
) {
}
