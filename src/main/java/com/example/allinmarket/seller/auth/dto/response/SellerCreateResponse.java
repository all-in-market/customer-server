package com.example.allinmarket.seller.auth.dto.response;

import com.example.allinmarket.common.enums.UserRole;
import com.example.allinmarket.seller.entity.Seller;
import com.example.allinmarket.seller.enums.SellerStatus;

public record SellerCreateResponse(
        Long id,
        String email,
        String name,
        String phone,
        String storeName,
        String bizNumber,
        String bankAccount,
        SellerStatus status,
        UserRole role
) {
    public static SellerCreateResponse from (Seller seller) {
        return new SellerCreateResponse(
                seller.getId(),
                seller.getEmail(),
                seller.getName(),
                seller.getPhone(),
                seller.getStoreName(),
                seller.getBizNumber(),
                seller.getBankAccount(),
                seller.getStatus(),
                seller.getRole()
        );
    }
}
