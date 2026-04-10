package com.example.allinmarket.buyer.auth.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import org.hibernate.validator.constraints.Length;

public record BuyerSignupRequest(
        @Email
        @NotBlank
        String email,

        @NotBlank
        @Length(max = 255)
        String password,

        @NotBlank
        @Length(max = 50)
        String name,

        @NotBlank
        @Length(max = 20)
        @Pattern(regexp = "^\\d{2,3}-\\d{3,4}-\\d{4}$")
        String phone
) {
}
