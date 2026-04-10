package com.example.allinmarket.buyer.auth.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record BuyerLoginRequest(
        @Email(message = "이메일 형식이 올바르지 않습니다.")
        @NotBlank(message = "이메일은 필수 입력 사항 입니다.")
        @Size(max = 100, message = "이메일은 최대 100자 까지 입력 가능합니다.")
        String email,

        @NotBlank(message = "비밀번호는 필수 입력 사항 입니다.")
        @Size(min = 8, max = 20, message = "비밀번호는 8자 이상 20자 이하여야 합니다.")
        String password
) {
}
