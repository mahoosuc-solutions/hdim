package com.healthdata.fhir.subscription;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

/**
 * Subscription notification payload.
 *
 * Sent to subscribers when a matching resource change occurs.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class SubscriptionNotification {

    /**
     * Unique notification ID.
     */
    @JsonProperty("id")
    private String id;

    /**
     * Subscription ID that triggered this notification.
     */
    @JsonProperty("subscriptionId")
    private UUID subscriptionId;

    /**
     * Type of event that triggered the notification.
     */
    @JsonProperty("eventType")
    private EventType eventType;

    /**
     * Timestamp when the event occurred.
     */
    @JsonProperty("timestamp")
    private Instant timestamp;

    /**
     * Resource type that was affected.
     */
    @JsonProperty("resourceType")
    private String resourceType;

    /**
     * Resource ID that was affected.
     */
    @JsonProperty("resourceId")
    private String resourceId;

    /**
     * Full URL to the resource.
     */
    @JsonProperty("resourceUrl")
    private String resourceUrl;

    /**
     * The full resource (if payload is full-resource).
     */
    @JsonProperty("resource")
    private Object resource;

    /**
     * Tenant ID for multi-tenant support.
     */
    @JsonProperty("tenantId")
    private String tenantId;

    /**
     * Sequential notification number for this subscription.
     */
    @JsonProperty("sequenceNumber")
    private long sequenceNumber;

    /**
     * Event types for subscription notifications.
     */
    public enum EventType {
        /**
         * Resource was created.
         */
        CREATED,

        /**
         * Resource was updated.
         */
        UPDATED,

        /**
         * Resource was deleted.
         */
        DELETED,

        /**
         * Heartbeat/keep-alive notification.
         */
        HEARTBEAT,

        /**
         * Subscription status changed.
         */
        STATUS_CHANGED
    }

    /**
     * Create a notification for a created resource.
     */
    public static SubscriptionNotification created(FhirSubscription subscription, String resourceId, Object resource) {
        return SubscriptionNotification.builder()
            .id(UUID.randomUUID().toString())
            .subscriptionId(subscription.getId())
            .eventType(EventType.CREATED)
            .timestamp(Instant.now())
            .resourceType(subscription.getResourceType())
            .resourceId(resourceId)
            .resource(resource)
            .tenantId(subscription.getTenantId())
            .sequenceNumber(subscription.getNotificationCount() + 1)
            .build();
    }

    /**
     * Create a notification for an updated resource.
     */
    public static SubscriptionNotification updated(FhirSubscription subscription, String resourceId, Object resource) {
        return SubscriptionNotification.builder()
            .id(UUID.randomUUID().toString())
            .subscriptionId(subscription.getId())
            .eventType(EventType.UPDATED)
            .timestamp(Instant.now())
            .resourceType(subscription.getResourceType())
            .resourceId(resourceId)
            .resource(resource)
            .tenantId(subscription.getTenantId())
            .sequenceNumber(subscription.getNotificationCount() + 1)
            .build();
    }

    /**
     * Create a notification for a deleted resource.
     */
    public static SubscriptionNotification deleted(FhirSubscription subscription, String resourceId) {
        return SubscriptionNotification.builder()
            .id(UUID.randomUUID().toString())
            .subscriptionId(subscription.getId())
            .eventType(EventType.DELETED)
            .timestamp(Instant.now())
            .resourceType(subscription.getResourceType())
            .resourceId(resourceId)
            .tenantId(subscription.getTenantId())
            .sequenceNumber(subscription.getNotificationCount() + 1)
            .build();
    }

    /**
     * Create a heartbeat notification.
     */
    public static SubscriptionNotification heartbeat(FhirSubscription subscription) {
        return SubscriptionNotification.builder()
            .id(UUID.randomUUID().toString())
            .subscriptionId(subscription.getId())
            .eventType(EventType.HEARTBEAT)
            .timestamp(Instant.now())
            .tenantId(subscription.getTenantId())
            .build();
    }
}
