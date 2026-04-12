package com.example.allinmarket.buyer.address.dto.request;

import jakarta.validation.constraints.Size;

public record AddressUpdateRequest(
        @Size(max = 50, message = "이름은 50자를 초과할 수 없습니다.")
        String recipient,

        @Size(max = 20, message = "전화번호는 20자를 초과할 수 없습니다.")
        String phone,

        @Size(max = 100, message = "주소지는 100자를 초과할 수 없습니다.")
        String detail,

        Boolean isDefault
) {
}
