package com.healthdata.fhir.subscription;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

/**
 * FHIR R4 Subscription resource entity.
 *
 * Implements FHIR Subscriptions (R4) for real-time notifications
 * when resources matching criteria are created/updated/deleted.
 *
 * Channel types supported:
 * - rest-hook: HTTP POST callback
 * - websocket: WebSocket push
 * - email: Email notifications
 * - message: FHIR Messaging
 */
@Entity
@Table(name = "fhir_subscriptions")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FhirSubscription {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID id;

    /**
     * Tenant ID for multi-tenant isolation.
     */
    @Column(name = "tenant_id", nullable = false)
    private String tenantId;

    /**
     * Subscription status.
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private SubscriptionStatus status;

    /**
     * Human-readable reason for the current status.
     */
    @Column(name = "reason")
    private String reason;

    /**
     * FHIR search-style criteria for matching resources.
     * Example: "Patient?identifier=http://example.com|123"
     */
    @Column(name = "criteria", nullable = false, length = 2000)
    private String criteria;

    /**
     * Resource type being watched.
     */
    @Column(name = "resource_type", nullable = false)
    private String resourceType;

    /**
     * Channel type for notifications.
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "channel_type", nullable = false)
    private ChannelType channelType;

    /**
     * Endpoint URL for rest-hook channels.
     */
    @Column(name = "channel_endpoint", length = 1000)
    private String channelEndpoint;

    /**
     * MIME type for payloads.
     */
    @Column(name = "channel_payload")
    @Builder.Default
    private String channelPayload = "application/fhir+json";

    /**
     * HTTP headers for rest-hook channel.
     */
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "channel_headers", columnDefinition = "jsonb")
    private Map<String, String> channelHeaders;

    /**
     * Subscription end time (when it expires).
     */
    @Column(name = "end_time")
    private Instant endTime;

    /**
     * Client-provided tag for subscription identification.
     */
    @Column(name = "tag")
    private String tag;

    /**
     * Contact email for subscription owner.
     */
    @Column(name = "contact_email")
    private String contactEmail;

    /**
     * Number of consecutive errors.
     */
    @Column(name = "error_count")
    @Builder.Default
    private int errorCount = 0;

    /**
     * Last error message.
     */
    @Column(name = "last_error", length = 1000)
    private String lastError;

    /**
     * Last successful notification time.
     */
    @Column(name = "last_notification_at")
    private Instant lastNotificationAt;

    /**
     * Total notification count.
     */
    @Column(name = "notification_count")
    @Builder.Default
    private long notificationCount = 0;

    /**
     * Full FHIR resource JSON.
     */
    @Column(name = "resource_json", columnDefinition = "jsonb", nullable = false)
    private String resourceJson;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Version
    @Column(name = "version")
    private Long version;

    /**
     * FHIR Subscription status values.
     */
    public enum SubscriptionStatus {
        /**
         * Client has requested subscription but server hasn't activated.
         */
        REQUESTED,

        /**
         * Subscription is active and notifications are being sent.
         */
        ACTIVE,

        /**
         * Subscription has errors and is temporarily disabled.
         */
        ERROR,

        /**
         * Subscription was turned off by client or server.
         */
        OFF,

        /**
         * Subscription reached the end time or was explicitly ended.
         */
        ENDED
    }

    /**
     * Channel types for notifications.
     */
    public enum ChannelType {
        /**
         * HTTP POST to endpoint URL.
         */
        REST_HOOK,

        /**
         * WebSocket push to connected clients.
         */
        WEBSOCKET,

        /**
         * Email notification.
         */
        EMAIL,

        /**
         * FHIR Messaging bundle.
         */
        MESSAGE
    }

    /**
     * Check if subscription is active.
     */
    public boolean isActive() {
        if (status != SubscriptionStatus.ACTIVE) {
            return false;
        }
        if (endTime != null && Instant.now().isAfter(endTime)) {
            return false;
        }
        return true;
    }

    /**
     * Record a successful notification.
     */
    public void recordSuccess() {
        this.lastNotificationAt = Instant.now();
        this.notificationCount++;
        this.errorCount = 0;
        this.lastError = null;
    }

    /**
     * Record a notification error.
     */
    public void recordError(String errorMessage) {
        this.errorCount++;
        this.lastError = errorMessage;
        if (this.errorCount >= 10) {
            this.status = SubscriptionStatus.ERROR;
            this.reason = "Too many consecutive errors";
        }
    }
}
