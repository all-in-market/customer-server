package com.example.allinmarket.common.outbox.entity;

import com.example.allinmarket.common.entity.CreatableEntity;
import com.example.allinmarket.domain.transactionhistory.enums.TransactionType;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(name = "history_outboxes")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class HistoryOutBox extends CreatableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TransactionType type;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String payload;

    @Column(nullable = false)
    private boolean processed = false;

    @Column(nullable = false)
    private int retryCount = 0;

    public static HistoryOutBox of(TransactionType type, String payload) {
        HistoryOutBox outBox = new HistoryOutBox();
        outBox.type = type;
        outBox.payload = payload;
        return outBox;
    }

    public void markProcessed() {
        this.processed = true;
    }

    public void incrementRetryCount() {
        this.retryCount++;
    }
}
