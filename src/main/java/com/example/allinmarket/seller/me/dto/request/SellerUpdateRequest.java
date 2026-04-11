package com.example.allinmarket.seller.me.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import org.hibernate.validator.constraints.Length;

public record SellerUpdateRequest (

        @Email
        @Length(max = 100, message = "이메일은 100자를 초과할 수 없습니다.")
        String email,

        @NotBlank
        @Length(min = 8, max = 20, message = "비밀번호는 8자 이상 20자 이하여야 합니다.")
        String password,

        @NotBlank
        @Length(max = 50, message = "이름은 50자를 초과할 수 없습니다.")
        String name,

        @NotBlank
        @Length(max = 20, message = "전화번호는 20자를 초과할 수 없습니다.")
        String phone,

        @NotBlank
        @Length(max = 100, message = "사업자명은 100자를 초과할 수 없습니다.")
        String storeName,

        @NotBlank
        @Length(max = 20, message = "사업자등록번호는 20자를 초과할 수 없습니다.")
        String bizNumber,

        @NotBlank
        @Length(max = 50, message = "계좌번호는 50자를 초과할 수 없습니다.")
        String bankAccount
) {}
