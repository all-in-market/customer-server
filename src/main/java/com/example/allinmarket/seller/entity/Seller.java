package com.example.allinmarket.seller.entity;

import com.example.allinmarket.common.entity.DeletableEntity;
import com.example.allinmarket.common.enums.ErrorEnum;
import com.example.allinmarket.common.enums.UserRole;
import com.example.allinmarket.common.exception.BaseException;
import com.example.allinmarket.domain.banking.enums.BankCode;
import com.example.allinmarket.seller.enums.SellerStatus;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
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

    @NotBlank
    @Column(length = 100, unique = true, nullable = false)
    private String email;

    @NotBlank
    @Column(length = 255)
    private String password;

    @NotBlank
    @Column(length = 50)
    private String name;

    @NotBlank
    @Column(length = 20)
    private String phone;

    @NotBlank
    @Column(name = "store_name", length = 100)
    private String storeName;

    @NotBlank
    @Column(name = "biz_number", length = 20, unique = true)
    private String bizNumber;

    @NotBlank
    @Column(name = "bank_code", length = 30)
    private String bankCode;

    @NotBlank
    @Column(name = "bank_account", length = 50)
    private String bankAccount;

    @Column(length = 20, nullable = false)
    @Enumerated(EnumType.STRING)
    private SellerStatus status = SellerStatus.PENDING;

    @Column(length = 20, nullable = false)
    @Enumerated(EnumType.STRING)
    private UserRole role = UserRole.SELLER;

    public static Seller of(
            String email,
            String password,
            String name,
            String phone,
            String storeName,
            String bizNumber,
            String bankCode,
            String bankAccount) {
        Seller seller = new Seller();

        seller.email = email;
        seller.password = password;
        seller.name = name;
        seller.phone = phone;
        seller.storeName = storeName;
        seller.bizNumber = bizNumber;
        seller.bankCode = bankCode;
        seller.bankAccount = bankAccount;
        seller.status = SellerStatus.PENDING;
        seller.role = UserRole.SELLER;

        return seller;
    }

    public void updateEmail(String email) {
        this.email = email;
    }

    public void updatePassword(String password) {
        this.password = password;
    }

    public void updateName(String name) {
        this.name = name;
    }

    public void updatePhone(String phone) {
        this.phone = phone;
    }

    public void updateStoreName(String storeName) {
        this.storeName = storeName;
    }

    public void updateBizNumber(String bizNumber) {
        this.bizNumber = bizNumber;
    }

    public void updateBankCode(String bankCode) {
        if (!BankCode.valueOf(bankCode).name().equals(bankCode)) {
            throw new BaseException(ErrorEnum.BANK_CODE_INVALID);
        }
        this.bankCode = bankCode;
    }

    public void updateBankAccount(String bankAccount) {
        this.bankAccount = bankAccount;
    }

    public void updateStatus(SellerStatus status) {
        this.status = status;
    }
}
