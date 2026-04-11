package com.example.allinmarket.buyer.me.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record BuyerUpdateRequest(
        @Size(max = 50, message = "이름은 50자를 초과할 수 없습니다.")
        String name,

        @Size(max = 20, message = "전화번호는 20자를 초과할 수 없습니다.")
        String phone
) {
}
