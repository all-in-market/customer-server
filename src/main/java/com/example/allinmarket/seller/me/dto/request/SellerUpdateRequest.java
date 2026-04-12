package com.example.allinmarket.seller.me.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import org.hibernate.validator.constraints.Length;

// 의도적으로 dto에 null값 허용하여 부분적 수정이 가능하도록 함
public record SellerUpdateRequest (

        @Email(message = "이메일 형식이 올바르지 않습니다.")
        @Length(max = 100, message = "이메일은 100자를 초과할 수 없습니다.")
        String email,

        @Length(min = 8, max = 20, message = "비밀번호는 8자 이상 20자 이하여야 합니다.")
        String password,

        @Length(max = 50, message = "이름은 50자를 초과할 수 없습니다.")
        String name,

        @Length(max = 20, message = "전화번호는 20자를 초과할 수 없습니다.")
        String phone,

        @Length(max = 100, message = "사업자명은 100자를 초과할 수 없습니다.")
        String storeName,

        @Length(max = 20, message = "사업자등록번호는 20자를 초과할 수 없습니다.")
        String bizNumber,

        @Length(max = 50, message = "계좌번호는 50자를 초과할 수 없습니다.")
        String bankAccount
) {}
