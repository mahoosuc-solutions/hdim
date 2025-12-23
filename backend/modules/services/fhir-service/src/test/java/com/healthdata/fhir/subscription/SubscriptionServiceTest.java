package com.healthdata.fhir.subscription;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;

import com.fasterxml.jackson.databind.ObjectMapper;

@ExtendWith(MockitoExtension.class)
@DisplayName("Subscription Service Tests")
class SubscriptionServiceTest {

    @Mock
    private FhirSubscriptionRepository subscriptionRepository;

    @Mock
    private SubscriptionWebSocketHandler webSocketHandler;

    @Mock
    private RestTemplate restTemplate;

    private final ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();

    @Test
    @DisplayName("Should create and activate subscription")
    void shouldCreateAndActivateSubscription() {
        SubscriptionService service = new SubscriptionService(
                subscriptionRepository, webSocketHandler, restTemplate, objectMapper);
        UUID id = UUID.randomUUID();
        FhirSubscription subscription = FhirSubscription.builder()
                .id(id)
                .tenantId("tenant-1")
                .criteria("Patient?status=active")
                .resourceType("Patient")
                .channelType(FhirSubscription.ChannelType.WEBSOCKET)
                .resourceJson("{}")
                .build();
        when(subscriptionRepository.save(any(FhirSubscription.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(subscriptionRepository.findById(id)).thenReturn(Optional.of(subscription));

        FhirSubscription created = service.createSubscription(subscription);

        assertThat(created.getStatus()).isEqualTo(FhirSubscription.SubscriptionStatus.ACTIVE);
        verify(subscriptionRepository, org.mockito.Mockito.atLeastOnce()).save(any(FhirSubscription.class));
    }

    @Test
    @DisplayName("Should validate subscription fields")
    void shouldValidateSubscriptionFields() {
        SubscriptionService service = new SubscriptionService(
                subscriptionRepository, webSocketHandler, restTemplate, objectMapper);
        FhirSubscription subscription = FhirSubscription.builder()
                .tenantId("tenant-1")
                .resourceType("Patient")
                .resourceJson("{}")
                .build();

        assertThatThrownBy(() -> service.createSubscription(subscription))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("Should reject REST hook subscription without endpoint")
    void shouldRejectRestHookWithoutEndpoint() {
        SubscriptionService service = new SubscriptionService(
                subscriptionRepository, webSocketHandler, restTemplate, objectMapper);
        FhirSubscription subscription = FhirSubscription.builder()
                .tenantId("tenant-1")
                .criteria("Patient?status=active")
                .resourceType("Patient")
                .channelType(FhirSubscription.ChannelType.REST_HOOK)
                .resourceJson("{}")
                .build();

        assertThatThrownBy(() -> service.createSubscription(subscription))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Endpoint");
    }

    @Test
    @DisplayName("Should reject subscription without channel type")
    void shouldRejectSubscriptionWithoutChannelType() {
        SubscriptionService service = new SubscriptionService(
                subscriptionRepository, webSocketHandler, restTemplate, objectMapper);
        FhirSubscription subscription = FhirSubscription.builder()
                .tenantId("tenant-1")
                .criteria("Patient?status=active")
                .resourceType("Patient")
                .resourceJson("{}")
                .build();

        assertThatThrownBy(() -> service.createSubscription(subscription))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Channel type");
    }

    @Test
    @DisplayName("Should update subscription")
    void shouldUpdateSubscription() {
        SubscriptionService service = new SubscriptionService(
                subscriptionRepository, webSocketHandler, restTemplate, objectMapper);
        UUID id = UUID.randomUUID();
        FhirSubscription subscription = FhirSubscription.builder()
                .id(id)
                .tenantId("tenant-1")
                .criteria("Patient?status=active")
                .resourceType("Patient")
                .channelType(FhirSubscription.ChannelType.REST_HOOK)
                .channelEndpoint("http://callback")
                .resourceJson("{}")
                .build();
        when(subscriptionRepository.findById(id)).thenReturn(Optional.of(subscription));
        when(subscriptionRepository.save(any(FhirSubscription.class))).thenAnswer(invocation -> invocation.getArgument(0));

        FhirSubscription updates = FhirSubscription.builder()
                .criteria("Patient?status=inactive")
                .channelEndpoint("http://updated")
                .tag("tag")
                .build();
        FhirSubscription result = service.updateSubscription(id, updates, "tenant-1");

        assertThat(result.getCriteria()).isEqualTo("Patient?status=inactive");
        assertThat(result.getChannelEndpoint()).isEqualTo("http://updated");
    }

    @Test
    @DisplayName("Should update subscription headers and end time")
    void shouldUpdateSubscriptionHeadersAndEndTime() {
        SubscriptionService service = new SubscriptionService(
                subscriptionRepository, webSocketHandler, restTemplate, objectMapper);
        UUID id = UUID.randomUUID();
        FhirSubscription subscription = FhirSubscription.builder()
                .id(id)
                .tenantId("tenant-1")
                .criteria("Patient?status=active")
                .resourceType("Patient")
                .channelType(FhirSubscription.ChannelType.REST_HOOK)
                .channelEndpoint("http://callback")
                .resourceJson("{}")
                .build();
        when(subscriptionRepository.findById(id)).thenReturn(Optional.of(subscription));
        when(subscriptionRepository.save(any(FhirSubscription.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Instant endTime = Instant.now().plusSeconds(3600);
        FhirSubscription updates = FhirSubscription.builder()
                .channelHeaders(Map.of("Authorization", "Bearer token"))
                .endTime(endTime)
                .build();

        FhirSubscription result = service.updateSubscription(id, updates, "tenant-1");

        assertThat(result.getChannelHeaders()).containsKey("Authorization");
        assertThat(result.getEndTime()).isEqualTo(endTime);
    }

    @Test
    @DisplayName("Should reject update for mismatched tenant")
    void shouldRejectUpdateForMismatchedTenant() {
        SubscriptionService service = new SubscriptionService(
                subscriptionRepository, webSocketHandler, restTemplate, objectMapper);
        UUID id = UUID.randomUUID();
        FhirSubscription subscription = FhirSubscription.builder()
                .id(id)
                .tenantId("tenant-2")
                .criteria("Patient?status=active")
                .resourceType("Patient")
                .channelType(FhirSubscription.ChannelType.REST_HOOK)
                .channelEndpoint("http://callback")
                .resourceJson("{}")
                .build();
        when(subscriptionRepository.findById(id)).thenReturn(Optional.of(subscription));

        assertThatThrownBy(() -> service.updateSubscription(id, FhirSubscription.builder().build(), "tenant-1"))
                .isInstanceOf(SubscriptionService.SubscriptionNotFoundException.class);
    }

    @Test
    @DisplayName("Should send REST hook notification")
    void shouldSendRestHookNotification() {
        SubscriptionService service = new SubscriptionService(
                subscriptionRepository, webSocketHandler, restTemplate, objectMapper);
        UUID id = UUID.randomUUID();
        FhirSubscription subscription = FhirSubscription.builder()
                .id(id)
                .tenantId("tenant-1")
                .criteria("Patient?status=active")
                .resourceType("Patient")
                .channelType(FhirSubscription.ChannelType.REST_HOOK)
                .channelEndpoint("http://callback")
                .resourceJson("{}")
                .status(FhirSubscription.SubscriptionStatus.ACTIVE)
                .build();
        when(subscriptionRepository.findActiveSubscriptionsForResource(anyString(), anyString(), any()))
                .thenReturn(List.of(subscription));
        when(subscriptionRepository.save(any(FhirSubscription.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(restTemplate.postForEntity(any(String.class), any(), eq(String.class)))
                .thenReturn(ResponseEntity.ok().build());

        service.processResourceChange("tenant-1", "Patient", "res-1", Map.of("id", "res-1"),
                SubscriptionNotification.EventType.CREATED);

        verify(restTemplate).postForEntity(any(String.class), any(), eq(String.class));
    }

    @Test
    @DisplayName("Should include headers in REST hook notification")
    void shouldIncludeHeadersInRestHookNotification() {
        SubscriptionService service = new SubscriptionService(
                subscriptionRepository, webSocketHandler, restTemplate, objectMapper);
        UUID id = UUID.randomUUID();
        FhirSubscription subscription = FhirSubscription.builder()
                .id(id)
                .tenantId("tenant-1")
                .criteria("Patient?status=active")
                .resourceType("Patient")
                .channelType(FhirSubscription.ChannelType.REST_HOOK)
                .channelEndpoint("http://callback")
                .channelHeaders(Map.of("Authorization", "Bearer token"))
                .resourceJson("{}")
                .status(FhirSubscription.SubscriptionStatus.ACTIVE)
                .build();
        when(subscriptionRepository.findActiveSubscriptionsForResource(anyString(), anyString(), any()))
                .thenReturn(List.of(subscription));
        when(subscriptionRepository.save(any(FhirSubscription.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(restTemplate.postForEntity(any(String.class), any(), eq(String.class)))
                .thenReturn(ResponseEntity.ok().build());

        service.processResourceChange("tenant-1", "Patient", "res-1", Map.of("id", "res-1"),
                SubscriptionNotification.EventType.CREATED);

        ArgumentCaptor<org.springframework.http.HttpEntity> captor = ArgumentCaptor.forClass(org.springframework.http.HttpEntity.class);
        verify(restTemplate).postForEntity(eq("http://callback"), captor.capture(), eq(String.class));
        assertThat(captor.getValue().getHeaders().getFirst("Authorization")).isEqualTo("Bearer token");
    }

    @Test
    @DisplayName("Should mark subscription error on rest hook failure")
    void shouldMarkSubscriptionErrorOnRestHookFailure() {
        SubscriptionService service = new SubscriptionService(
                subscriptionRepository, webSocketHandler, restTemplate, objectMapper);
        UUID id = UUID.randomUUID();
        FhirSubscription subscription = FhirSubscription.builder()
                .id(id)
                .tenantId("tenant-1")
                .criteria("Patient?status=active")
                .resourceType("Patient")
                .channelType(FhirSubscription.ChannelType.REST_HOOK)
                .channelEndpoint("http://callback")
                .resourceJson("{}")
                .status(FhirSubscription.SubscriptionStatus.ACTIVE)
                .errorCount(9)
                .build();
        when(subscriptionRepository.findActiveSubscriptionsForResource(anyString(), anyString(), any()))
                .thenReturn(List.of(subscription));
        when(subscriptionRepository.save(any(FhirSubscription.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(restTemplate.postForEntity(any(String.class), any(), eq(String.class)))
                .thenThrow(new RuntimeException("fail"));

        service.processResourceChange("tenant-1", "Patient", "res-1", Map.of("id", "res-1"),
                SubscriptionNotification.EventType.CREATED);

        assertThat(subscription.getStatus()).isEqualTo(FhirSubscription.SubscriptionStatus.ERROR);
        assertThat(subscription.getReason()).isEqualTo("Too many consecutive errors");
    }

    @Test
    @DisplayName("Should send websocket notification")
    void shouldSendWebsocketNotification() {
        SubscriptionService service = new SubscriptionService(
                subscriptionRepository, webSocketHandler, restTemplate, objectMapper);
        UUID id = UUID.randomUUID();
        FhirSubscription subscription = FhirSubscription.builder()
                .id(id)
                .tenantId("tenant-1")
                .criteria("Patient?status=active")
                .resourceType("Patient")
                .channelType(FhirSubscription.ChannelType.WEBSOCKET)
                .resourceJson("{}")
                .status(FhirSubscription.SubscriptionStatus.ACTIVE)
                .build();
        when(subscriptionRepository.findActiveSubscriptionsForResource(anyString(), anyString(), any()))
                .thenReturn(List.of(subscription));
        when(subscriptionRepository.save(any(FhirSubscription.class))).thenAnswer(invocation -> invocation.getArgument(0));

        service.processResourceChange("tenant-1", "Patient", "res-1", Map.of("id", "res-1"),
                SubscriptionNotification.EventType.UPDATED);

        verify(webSocketHandler).sendNotification(eq("tenant-1"), any(SubscriptionNotification.class));
    }

    @Test
    @DisplayName("Should send email and message notifications")
    void shouldSendEmailAndMessageNotifications() {
        SubscriptionService service = new SubscriptionService(
                subscriptionRepository, webSocketHandler, restTemplate, objectMapper);
        FhirSubscription emailSubscription = FhirSubscription.builder()
                .id(UUID.randomUUID())
                .tenantId("tenant-1")
                .criteria("Patient?status=active")
                .resourceType("Patient")
                .channelType(FhirSubscription.ChannelType.EMAIL)
                .resourceJson("{}")
                .status(FhirSubscription.SubscriptionStatus.ACTIVE)
                .build();
        FhirSubscription messageSubscription = FhirSubscription.builder()
                .id(UUID.randomUUID())
                .tenantId("tenant-1")
                .criteria("Patient?status=active")
                .resourceType("Patient")
                .channelType(FhirSubscription.ChannelType.MESSAGE)
                .resourceJson("{}")
                .status(FhirSubscription.SubscriptionStatus.ACTIVE)
                .build();
        when(subscriptionRepository.findActiveSubscriptionsForResource(anyString(), anyString(), any()))
                .thenReturn(List.of(emailSubscription, messageSubscription));
        when(subscriptionRepository.save(any(FhirSubscription.class))).thenAnswer(invocation -> invocation.getArgument(0));

        service.processResourceChange("tenant-1", "Patient", "res-1", Map.of("id", "res-1"),
                SubscriptionNotification.EventType.UPDATED);

        assertThat(emailSubscription.getNotificationCount()).isEqualTo(1);
        assertThat(messageSubscription.getNotificationCount()).isEqualTo(1);
    }

    @Test
    @DisplayName("Should return subscription only for tenant")
    void shouldReturnSubscriptionOnlyForTenant() {
        SubscriptionService service = new SubscriptionService(
                subscriptionRepository, webSocketHandler, restTemplate, objectMapper);
        UUID id = UUID.randomUUID();
        FhirSubscription subscription = FhirSubscription.builder()
                .id(id)
                .tenantId("tenant-1")
                .criteria("Patient?status=active")
                .resourceType("Patient")
                .channelType(FhirSubscription.ChannelType.WEBSOCKET)
                .resourceJson("{}")
                .build();
        when(subscriptionRepository.findById(id)).thenReturn(Optional.of(subscription));

        assertThat(service.getSubscription(id, "tenant-1")).contains(subscription);
        assertThat(service.getSubscription(id, "tenant-2")).isEmpty();
    }

    @Test
    @DisplayName("Should list subscriptions")
    void shouldListSubscriptions() {
        SubscriptionService service = new SubscriptionService(
                subscriptionRepository, webSocketHandler, restTemplate, objectMapper);
        when(subscriptionRepository.findByTenantId("tenant-1")).thenReturn(List.of(
                FhirSubscription.builder().id(UUID.randomUUID()).tenantId("tenant-1").resourceJson("{}").build()));

        List<FhirSubscription> subscriptions = service.getSubscriptions("tenant-1");

        assertThat(subscriptions).hasSize(1);
    }

    @Test
    @DisplayName("Should delete subscription for tenant")
    void shouldDeleteSubscriptionForTenant() {
        SubscriptionService service = new SubscriptionService(
                subscriptionRepository, webSocketHandler, restTemplate, objectMapper);
        UUID id = UUID.randomUUID();
        FhirSubscription subscription = FhirSubscription.builder()
                .id(id)
                .tenantId("tenant-1")
                .resourceJson("{}")
                .build();
        when(subscriptionRepository.findById(id)).thenReturn(Optional.of(subscription));

        service.deleteSubscription(id, "tenant-1");

        verify(subscriptionRepository).delete(subscription);
    }

    @Test
    @DisplayName("Should reject delete for mismatched tenant")
    void shouldRejectDeleteForMismatchedTenant() {
        SubscriptionService service = new SubscriptionService(
                subscriptionRepository, webSocketHandler, restTemplate, objectMapper);
        UUID id = UUID.randomUUID();
        FhirSubscription subscription = FhirSubscription.builder()
                .id(id)
                .tenantId("tenant-2")
                .resourceJson("{}")
                .build();
        when(subscriptionRepository.findById(id)).thenReturn(Optional.of(subscription));

        assertThatThrownBy(() -> service.deleteSubscription(id, "tenant-1"))
                .isInstanceOf(SubscriptionService.SubscriptionNotFoundException.class);
    }

    @Test
    @DisplayName("Should send WebSocket notification")
    void shouldSendWebSocketNotification() {
        SubscriptionService service = new SubscriptionService(
                subscriptionRepository, webSocketHandler, restTemplate, objectMapper);
        UUID id = UUID.randomUUID();
        FhirSubscription subscription = FhirSubscription.builder()
                .id(id)
                .tenantId("tenant-1")
                .criteria("Patient?status=active")
                .resourceType("Patient")
                .channelType(FhirSubscription.ChannelType.WEBSOCKET)
                .resourceJson("{}")
                .status(FhirSubscription.SubscriptionStatus.ACTIVE)
                .build();
        when(subscriptionRepository.findActiveSubscriptionsForResource(anyString(), anyString(), any()))
                .thenReturn(List.of(subscription));
        when(subscriptionRepository.save(any(FhirSubscription.class))).thenAnswer(invocation -> invocation.getArgument(0));

        service.processResourceChange("tenant-1", "Patient", "res-1", Map.of("id", "res-1"),
                SubscriptionNotification.EventType.UPDATED);

        verify(webSocketHandler).sendNotification(eq("tenant-1"), any());
    }

    @Test
    @DisplayName("Should handle email notifications")
    void shouldHandleEmailNotifications() {
        SubscriptionService service = new SubscriptionService(
                subscriptionRepository, webSocketHandler, restTemplate, objectMapper);
        UUID id = UUID.randomUUID();
        FhirSubscription subscription = FhirSubscription.builder()
                .id(id)
                .tenantId("tenant-1")
                .criteria("Patient?status=active")
                .resourceType("Patient")
                .channelType(FhirSubscription.ChannelType.EMAIL)
                .resourceJson("{}")
                .status(FhirSubscription.SubscriptionStatus.ACTIVE)
                .build();
        when(subscriptionRepository.findActiveSubscriptionsForResource(anyString(), anyString(), any()))
                .thenReturn(List.of(subscription));
        when(subscriptionRepository.save(any(FhirSubscription.class))).thenAnswer(invocation -> invocation.getArgument(0));

        service.processResourceChange("tenant-1", "Patient", "res-1", Map.of("id", "res-1"),
                SubscriptionNotification.EventType.CREATED);

        verify(subscriptionRepository).save(subscription);
        verify(restTemplate, org.mockito.Mockito.never()).postForEntity(anyString(), any(), eq(String.class));
    }

    @Test
    @DisplayName("Should activate and deactivate subscription")
    void shouldActivateAndDeactivateSubscription() {
        SubscriptionService service = new SubscriptionService(
                subscriptionRepository, webSocketHandler, restTemplate, objectMapper);
        UUID id = UUID.randomUUID();
        FhirSubscription subscription = FhirSubscription.builder()
                .id(id)
                .tenantId("tenant-1")
                .resourceJson("{}")
                .build();
        when(subscriptionRepository.findById(id)).thenReturn(Optional.of(subscription));
        when(subscriptionRepository.save(any(FhirSubscription.class))).thenAnswer(invocation -> invocation.getArgument(0));

        service.activateSubscription(id);
        assertThat(subscription.getStatus()).isEqualTo(FhirSubscription.SubscriptionStatus.ACTIVE);

        service.deactivateSubscription(id, "disabled");
        assertThat(subscription.getStatus()).isEqualTo(FhirSubscription.SubscriptionStatus.OFF);
        assertThat(subscription.getReason()).isEqualTo("disabled");
    }

    @Test
    @DisplayName("Should cleanup expired subscriptions")
    void shouldCleanupExpiredSubscriptions() {
        SubscriptionService service = new SubscriptionService(
                subscriptionRepository, webSocketHandler, restTemplate, objectMapper);
        FhirSubscription subscription = FhirSubscription.builder()
                .id(UUID.randomUUID())
                .tenantId("tenant-1")
                .criteria("Patient?status=active")
                .resourceType("Patient")
                .channelType(FhirSubscription.ChannelType.REST_HOOK)
                .resourceJson("{}")
                .build();
        when(subscriptionRepository.findExpiredSubscriptions(any(Instant.class))).thenReturn(List.of(subscription));
        when(subscriptionRepository.save(any(FhirSubscription.class))).thenAnswer(invocation -> invocation.getArgument(0));

        service.cleanupExpiredSubscriptions();

        ArgumentCaptor<FhirSubscription> captor = ArgumentCaptor.forClass(FhirSubscription.class);
        verify(subscriptionRepository).save(captor.capture());
        assertThat(captor.getValue().getStatus()).isEqualTo(FhirSubscription.SubscriptionStatus.ENDED);
    }
}
