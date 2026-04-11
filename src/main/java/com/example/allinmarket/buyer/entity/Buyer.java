package com.example.allinmarket.buyer.entity;

import com.example.allinmarket.common.entity.DeletableEntity;
import com.example.allinmarket.common.enums.ErrorEnum;
import com.example.allinmarket.common.enums.UserRole;
import com.example.allinmarket.common.exception.BaseException;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "buyers")
public class Buyer extends DeletableEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank
    @Column(nullable = false, length = 100, unique = true)
    private String email;

    @NotBlank
    @Column(nullable = false)
    private String password;

    @NotBlank
    @Column(nullable = false, length = 50)
    private String name;

    @NotBlank
    @Column(nullable = false, length = 20)
    private String phone;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private UserRole role = UserRole.BUYER;

    public static Buyer of(String email, String password, String name, String phone) {
        Buyer buyer = new Buyer();
        buyer.email = email;
        buyer.password = password;
        buyer.name = name;
        buyer.phone = phone;
        buyer.role = UserRole.BUYER;
        return buyer;
    }

    public void updateNameAndPhone(String name, String phone) {

        if (name == null || name.isBlank() || phone == null || phone.isBlank()) {
            throw new BaseException(ErrorEnum.INVALID_INPUT);
        }

        this.name = name;
        this.phone = phone;
    }
}
