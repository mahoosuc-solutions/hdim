package com.healthdata.eventrouter.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Entity
@Table(name = "dead_letter_events", indexes = {
    @Index(name = "idx_tenant_failed_at", columnList = "tenant_id, failed_at"),
    @Index(name = "idx_event_type", columnList = "event_type")
})
@Data
@NoArgsConstructor
public class DeadLetterEventEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "tenant_id", nullable = false)
    private String tenantId;

    @Column(name = "event_type", nullable = false)
    private String eventType;

    @Column(name = "source_topic")
    private String sourceTopic;

    @Column(name = "original_payload", columnDefinition = "TEXT")
    private String originalPayload;

    @Column(name = "failure_reason", columnDefinition = "TEXT")
    private String failureReason;

    @Column(name = "retry_count")
    private Integer retryCount = 0;

    @Column(name = "failed_at", nullable = false)
    private Instant failedAt;

    @Column(name = "last_retry_at")
    private Instant lastRetryAt;

    @PrePersist
    protected void onCreate() {
        failedAt = Instant.now();
    }
}
