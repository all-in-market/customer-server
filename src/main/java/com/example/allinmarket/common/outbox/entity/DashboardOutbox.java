package com.example.allinmarket.common.outbox.entity;

import com.example.allinmarket.common.entity.CreatableEntity;
import com.example.allinmarket.common.outbox.enums.OutboxEventType;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;


@Entity
@Table(name = "dashboard_outbox")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class DashboardOutbox extends CreatableEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private OutboxEventType eventType;

    @Column(nullable = false)
    private Long aggregateId;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String payload;

    @Column(nullable = false)
    private boolean processed;

    @Column(nullable = false)
    private int retryCount;

    public static DashboardOutbox of(OutboxEventType eventType, Long aggregateId, String payload) {

        DashboardOutbox dashboardOutbox = new DashboardOutbox();
        dashboardOutbox.eventType = eventType;
        dashboardOutbox.aggregateId = aggregateId;
        dashboardOutbox.payload = payload;
        dashboardOutbox.processed = false;
        dashboardOutbox.retryCount = 0;

        return dashboardOutbox;
    }

    public void markProcessed() {
        this.processed = true;
    }

    public void increaseRetryCount() {
        this.retryCount++;
    }
}
