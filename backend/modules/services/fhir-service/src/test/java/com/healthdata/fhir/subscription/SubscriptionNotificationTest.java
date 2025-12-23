package com.healthdata.fhir.subscription;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("Subscription Notification Tests")
class SubscriptionNotificationTest {

    @Test
    @DisplayName("Should build created notification")
    void shouldBuildCreatedNotification() {
        FhirSubscription subscription = FhirSubscription.builder()
                .id(UUID.randomUUID())
                .tenantId("tenant-1")
                .resourceType("Patient")
                .notificationCount(0)
                .build();

        SubscriptionNotification notification = SubscriptionNotification.created(subscription, "res-1", new Object());

        assertThat(notification.getEventType()).isEqualTo(SubscriptionNotification.EventType.CREATED);
        assertThat(notification.getResourceId()).isEqualTo("res-1");
        assertThat(notification.getTenantId()).isEqualTo("tenant-1");
    }

    @Test
    @DisplayName("Should build heartbeat notification")
    void shouldBuildHeartbeatNotification() {
        FhirSubscription subscription = FhirSubscription.builder()
                .id(UUID.randomUUID())
                .tenantId("tenant-1")
                .build();

        SubscriptionNotification notification = SubscriptionNotification.heartbeat(subscription);

        assertThat(notification.getEventType()).isEqualTo(SubscriptionNotification.EventType.HEARTBEAT);
        assertThat(notification.getTenantId()).isEqualTo("tenant-1");
    }
}
