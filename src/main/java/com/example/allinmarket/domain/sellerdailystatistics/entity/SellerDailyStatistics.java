package com.example.allinmarket.domain.sellerdailystatistics.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(name = "seller_daily_statistics")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class SellerDailyStatistics {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
}
