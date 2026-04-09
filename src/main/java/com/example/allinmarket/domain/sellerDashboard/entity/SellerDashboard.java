package com.example.allinmarket.domain.sellerDashboard.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(name = "seller_dashboard")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class SellerDashboard {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
}
