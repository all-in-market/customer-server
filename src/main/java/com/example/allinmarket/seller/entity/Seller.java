package com.example.allinmarket.seller.entity;

import com.example.allinmarket.common.entity.DeletableEntity;
import com.example.allinmarket.common.enums.UserRole;
import com.example.allinmarket.seller.enums.SellerStatus;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "sellers")
public class Seller extends DeletableEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(length = 100, nullable = false, unique = true)
    private String email;

    @Column(length = 255, nullable = false)
    private String password;

    @Column(length = 50, nullable = false)
    private String name;

    @Column(length = 20, nullable = false)
    private String phone;

    @Column(name = "store_name", length = 100, nullable = false)
    private String storeName;

    @Column(name = "biz_number", length = 20, nullable = false, unique = true)
    private String bizNumber;

    @Column(name = "bank_account", length = 50)
    private String bankAccount;

    @Column(length = 20, nullable = false)
    @Enumerated(EnumType.STRING)
    private SellerStatus status;

    @Column(length = 20, nullable = false)
    @Enumerated(EnumType.STRING)
    private UserRole role;

    public static Seller of(
            String email,
            String password,
            String name,
            String phone,
            String storeName,
            String bizNumber,
            String bankAccount) {
        Seller seller = new Seller();

        seller.email = email;
        seller.password = password;
        seller.name = name;
        seller.phone = phone;
        seller.storeName = storeName;
        seller.bizNumber = bizNumber;
        seller.bankAccount = bankAccount;
        seller.status = SellerStatus.PENDING;
        seller.role = UserRole.SELLER;

        return seller;
    }
}
