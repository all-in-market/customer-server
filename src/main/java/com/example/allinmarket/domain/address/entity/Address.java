package com.example.allinmarket.domain.address.entity;

import com.example.allinmarket.buyer.entity.Buyer;
import com.example.allinmarket.common.entity.DeletableEntity;
import com.example.allinmarket.common.entity.ModifiableEntity;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "addresses")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Getter
public class Address extends ModifiableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "buyer_id", nullable = false)
    private Buyer buyer;

    @NotBlank
    @Column(nullable = false, length = 50)
    private String recipient;

    @NotBlank
    @Column(nullable = false, length = 20)
    private String phone;

    @NotBlank
    @Column(nullable = false, length = 100)
    private String detail;

    @Column(name = "is_default", nullable = false)
    private boolean isDefault = false;

    public static Address of(Buyer buyer, String recipient, String phone, String detail) {
        Address address = new Address();
        address.buyer = buyer;
        address.recipient = recipient;
        address.phone = phone;
        address.detail = detail;
        address.isDefault = false;

        return address;
    }
}
