package com.example.allinmarket.domain.address.entity;

import com.example.allinmarket.buyer.entity.Buyer;
import com.example.allinmarket.common.entity.DeletableEntity;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "addresses")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Getter
public class Address extends DeletableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "buyer_id", nullable = false)
    private Buyer buyer;

    @Column(nullable = false, length = 50)
    private String recipient;

    @Column(nullable = false, length = 20)
    private String phone;

    @Column(nullable = false, length = 100)
    private String detail;

    @Column(nullable = false)
    private boolean isDefault = true;

    public static Address of(Buyer buyer, String recipient, String phone, String detail) {
        Address address = new Address();
        address.buyer = buyer;
        address.recipient = recipient;
        address.phone = phone;
        address.detail = detail;
        address.isDefault = true;

        return address;
    }

    public void softDelete() {
        /**
         * 삭제 전 검증 로직
         */
        delete();
    }
}
