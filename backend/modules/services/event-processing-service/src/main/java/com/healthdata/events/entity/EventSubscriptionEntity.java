package com.healthdata.events.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

/**
 * Event Subscription Entity
 *
 * Stores event listener subscriptions and delivery metadata.
 */
@Entity
@Table(name = "event_subscriptions")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EventSubscriptionEntity {

    @Id
    @Column(name = "id", nullable = false)
    private UUID id;

    @Column(name = "tenant_id", nullable = false, length = 64)
    private String tenantId;

    @Column(name = "subscription_name", nullable = false, length = 255)
    private String subscriptionName;

    @Column(name = "event_types", nullable = false, columnDefinition = "TEXT")
    private String eventTypes;

    @Column(name = "filter_expression", columnDefinition = "TEXT")
    private String filterExpression;

    @Column(name = "endpoint_url", nullable = false, length = 512)
    private String endpointUrl;

    @Column(name = "endpoint_type", nullable = false, length = 50)
    private String endpointType;

    @Column(name = "active", nullable = false)
    @Builder.Default
    private Boolean active = true;

    @Column(name = "retry_policy", columnDefinition = "TEXT")
    private String retryPolicy;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Column(name = "last_delivery_at")
    private Instant lastDeliveryAt;

    @Column(name = "delivery_count", nullable = false)
    @Builder.Default
    private Long deliveryCount = 0L;

    @Column(name = "failure_count", nullable = false)
    @Builder.Default
    private Long failureCount = 0L;

    @PrePersist
    protected void onCreate() {
        if (id == null) {
            id = UUID.randomUUID();
        }
        Instant now = Instant.now();
        createdAt = now;
        updatedAt = now;
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = Instant.now();
    }
}
