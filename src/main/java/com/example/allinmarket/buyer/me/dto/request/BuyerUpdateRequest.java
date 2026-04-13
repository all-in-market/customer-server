package com.example.allinmarket.buyer.me.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record BuyerUpdateRequest(

        @Size(max = 100, message = "이메일은 최대 100자 까지 입력 가능합니다.")
        String email,

        @Size(min = 8, max = 20, message = "비밀번호는 8자 이상 20자 이하여야 합니다.")
        String password,

        @Size(max = 50, message = "이름은 50자를 초과할 수 없습니다.")
        String name,

        @Size(max = 20, message = "전화번호는 20자를 초과할 수 없습니다.")
        String phone
) {
}
