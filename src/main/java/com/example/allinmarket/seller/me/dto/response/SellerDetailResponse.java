package com.example.allinmarket.seller.me.dto.response;

import com.example.allinmarket.common.enums.UserRole;
import com.example.allinmarket.seller.entity.Seller;
import com.example.allinmarket.seller.enums.SellerStatus;

public record SellerDetailResponse(
        // 비밀번호 제외 모든 필드 표시
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
    public static SellerDetailResponse from (Seller seller) {
        return new SellerDetailResponse(
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
