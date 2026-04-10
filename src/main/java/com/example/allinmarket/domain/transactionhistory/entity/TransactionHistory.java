package com.example.allinmarket.domain.transactionhistory.entity;

import com.example.allinmarket.common.entity.CreatableEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "transaction_histories")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Getter
public class TransactionHistory extends CreatableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;



}
