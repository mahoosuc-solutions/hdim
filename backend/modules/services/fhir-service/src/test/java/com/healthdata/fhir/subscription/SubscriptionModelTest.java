package com.healthdata.fhir.subscription;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.UUID;

import org.junit.jupiter.api.Test;

class SubscriptionModelTest {

    @Test
    void shouldEvaluateSubscriptionState() {
        FhirSubscription subscription = FhirSubscription.builder()
                .id(UUID.randomUUID())
                .tenantId("tenant-1")
                .criteria("Patient?active=true")
                .resourceType("Patient")
                .channelType(FhirSubscription.ChannelType.REST_HOOK)
                .status(FhirSubscription.SubscriptionStatus.ACTIVE)
                .resourceJson("{}")
                .build();

        assertThat(subscription.isActive()).isTrue();

        subscription.setEndTime(Instant.now().minusSeconds(5));
        assertThat(subscription.isActive()).isFalse();

        subscription.setStatus(FhirSubscription.SubscriptionStatus.OFF);
        subscription.setEndTime(null);
        assertThat(subscription.isActive()).isFalse();
    }

    @Test
    void shouldRecordNotificationOutcomes() {
        FhirSubscription subscription = FhirSubscription.builder()
                .id(UUID.randomUUID())
                .tenantId("tenant-1")
                .criteria("Patient?active=true")
                .resourceType("Patient")
                .channelType(FhirSubscription.ChannelType.WEBSOCKET)
                .status(FhirSubscription.SubscriptionStatus.ACTIVE)
                .resourceJson("{}")
                .build();

        subscription.recordError("timeout");
        assertThat(subscription.getErrorCount()).isEqualTo(1);
        assertThat(subscription.getLastError()).isEqualTo("timeout");

        subscription.recordSuccess();
        assertThat(subscription.getErrorCount()).isZero();
        assertThat(subscription.getLastError()).isNull();
        assertThat(subscription.getNotificationCount()).isEqualTo(1);
        assertThat(subscription.getLastNotificationAt()).isNotNull();

        for (int i = 0; i < 10; i++) {
            subscription.recordError("fail");
        }
        assertThat(subscription.getStatus()).isEqualTo(FhirSubscription.SubscriptionStatus.ERROR);
        assertThat(subscription.getReason()).isEqualTo("Too many consecutive errors");
    }

    @Test
    void shouldBuildNotificationPayloads() {
        FhirSubscription subscription = FhirSubscription.builder()
                .id(UUID.randomUUID())
                .tenantId("tenant-1")
                .criteria("Patient?active=true")
                .resourceType("Patient")
                .channelType(FhirSubscription.ChannelType.REST_HOOK)
                .status(FhirSubscription.SubscriptionStatus.ACTIVE)
                .notificationCount(5)
                .resourceJson("{}")
                .build();

        SubscriptionNotification created = SubscriptionNotification.created(subscription, "res-1", "payload");
        assertThat(created.getEventType()).isEqualTo(SubscriptionNotification.EventType.CREATED);
        assertThat(created.getResourceId()).isEqualTo("res-1");
        assertThat(created.getSequenceNumber()).isEqualTo(6);

        SubscriptionNotification updated = SubscriptionNotification.updated(subscription, "res-2", "payload");
        assertThat(updated.getEventType()).isEqualTo(SubscriptionNotification.EventType.UPDATED);

        SubscriptionNotification deleted = SubscriptionNotification.deleted(subscription, "res-3");
        assertThat(deleted.getEventType()).isEqualTo(SubscriptionNotification.EventType.DELETED);

        SubscriptionNotification heartbeat = SubscriptionNotification.heartbeat(subscription);
        assertThat(heartbeat.getEventType()).isEqualTo(SubscriptionNotification.EventType.HEARTBEAT);
        assertThat(heartbeat.getTenantId()).isEqualTo("tenant-1");
    }
}
