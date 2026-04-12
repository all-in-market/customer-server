package com.example.allinmarket.buyer.address.dto.response;

import com.example.allinmarket.domain.address.entity.Address;

public record AddressDetailResponse(
        Long addressId,
        Long buyerId,
        String recipient,
        String phone,
        String detail,
        boolean isDefault
) {
    public static AddressDetailResponse from(Address address) {
        return new AddressDetailResponse(
                address.getId(),
                address.getBuyer().getId(),
                address.getRecipient(),
                address.getPhone(),
                address.getDetail(),
                address.isDefault()
        );
    }
}
