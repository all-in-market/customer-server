package com.example.allinmarket.seller.auth.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record SellerCreateRequest(

        @Email @Size(max = 100, message = "이메일은 100자를 초과할 수 없습니다.")
        String email,

        @NotBlank @Size(min = 8, max = 20, message = "비밀번호는 8자 이상 20자 이하여야 합니다.")
        String password,

        @NotBlank @Size(max = 50, message = "이름은 50자를 초과할 수 없습니다.")
        String name,

        @NotBlank @Size(max = 20, message = "전화번호는 20자를 초과할 수 없습니다.")
        String phone,

        @NotBlank @Size(max = 100, message = "사업자명은 100자를 초과할 수 없습니다.")
        String storeName,

        @NotBlank @Size(max = 20, message = "사업자등록번호는 20자를 초과할 수 없습니다.")
        String bizNumber,

        @NotBlank @Size(max = 50, message = "계좌번호는 50자를 초과할 수 없습니다.")
        String bankAccount
) {}
