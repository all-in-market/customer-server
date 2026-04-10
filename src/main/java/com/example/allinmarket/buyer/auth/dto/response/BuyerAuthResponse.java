package com.example.allinmarket.buyer.auth.dto.response;

import com.example.allinmarket.buyer.entity.Buyer;

public record BuyerAuthResponse(
        String email,
        String name,
        String phone
) {
    public static BuyerAuthResponse from(Buyer buyer) {
        return new BuyerAuthResponse(
                buyer.getEmail(),
                buyer.getName(),
                buyer.getPhone()
        );
    }
}
