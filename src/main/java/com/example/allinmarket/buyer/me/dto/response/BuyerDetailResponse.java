package com.example.allinmarket.buyer.me.dto.response;

import com.example.allinmarket.buyer.entity.Buyer;
import com.example.allinmarket.common.enums.UserRole;

public record BuyerDetailResponse(
        Long buyerId,
        String email,
        String name,
        String phone,
        UserRole role
) {
    public static BuyerDetailResponse from(Buyer buyer) {
        return new BuyerDetailResponse(
                buyer.getId(),
                buyer.getEmail(),
                buyer.getName(),
                buyer.getPhone(),
                buyer.getRole()
        );
    }
}
