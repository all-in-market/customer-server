package com.example.allinmarket.buyer.entity;

import com.example.allinmarket.common.entity.DeletableEntity;
import com.example.allinmarket.common.enums.UserRole;
import jakarta.persistence.*;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.validator.constraints.Length;

@Getter
@Entity
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "buyers")
public class Buyer extends DeletableEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank
    @Length(max = 100)
    private String email;

    @NotBlank
    private String password;

    @NotBlank
    @Length(max = 50)
    private String name;

    @NotBlank
    @Length(max = 20)
    private String phone;

    @NotNull
    @Enumerated(EnumType.STRING)
    private UserRole role;

    public static Buyer of(String email, String password, String name, String phone, UserRole role) {
        Buyer buyer = new Buyer();
        buyer.email = email;
        buyer.password = password;
        buyer.name = name;
        buyer.phone = phone;
        buyer.role = role;
        return buyer;
    }
}
