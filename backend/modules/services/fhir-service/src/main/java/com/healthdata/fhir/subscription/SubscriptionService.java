package com.healthdata.fhir.subscription;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Service for managing FHIR Subscriptions.
 *
 * Handles:
 * - Subscription CRUD operations
 * - Matching resources against subscription criteria
 * - Sending notifications via configured channels
 * - Subscription lifecycle management
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SubscriptionService {

    private final FhirSubscriptionRepository subscriptionRepository;
    private final SubscriptionWebSocketHandler webSocketHandler;
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    /**
     * Create a new subscription.
     */
    @Transactional
    public FhirSubscription createSubscription(FhirSubscription subscription) {
        log.info("Creating subscription for resource type: {}", subscription.getResourceType());

        // Validate subscription
        validateSubscription(subscription);

        // Set initial status
        subscription.setStatus(FhirSubscription.SubscriptionStatus.REQUESTED);

        FhirSubscription saved = subscriptionRepository.save(subscription);

        // Auto-activate if criteria is valid
        activateSubscription(saved.getId());

        return saved;
    }

    /**
     * Get subscription by ID.
     */
    public Optional<FhirSubscription> getSubscription(UUID id, String tenantId) {
        return subscriptionRepository.findById(id)
            .filter(s -> s.getTenantId().equals(tenantId));
    }

    /**
     * Get all subscriptions for a tenant.
     */
    public List<FhirSubscription> getSubscriptions(String tenantId) {
        return subscriptionRepository.findByTenantId(tenantId);
    }

    /**
     * Update subscription.
     */
    @Transactional
    public FhirSubscription updateSubscription(UUID id, FhirSubscription updates, String tenantId) {
        FhirSubscription subscription = subscriptionRepository.findById(id)
            .filter(s -> s.getTenantId().equals(tenantId))
            .orElseThrow(() -> new SubscriptionNotFoundException(id));

        // Update allowed fields
        if (updates.getCriteria() != null) {
            subscription.setCriteria(updates.getCriteria());
        }
        if (updates.getChannelEndpoint() != null) {
            subscription.setChannelEndpoint(updates.getChannelEndpoint());
        }
        if (updates.getChannelHeaders() != null) {
            subscription.setChannelHeaders(updates.getChannelHeaders());
        }
        if (updates.getEndTime() != null) {
            subscription.setEndTime(updates.getEndTime());
        }
        if (updates.getTag() != null) {
            subscription.setTag(updates.getTag());
        }

        return subscriptionRepository.save(subscription);
    }

    /**
     * Delete subscription.
     */
    @Transactional
    public void deleteSubscription(UUID id, String tenantId) {
        FhirSubscription subscription = subscriptionRepository.findById(id)
            .filter(s -> s.getTenantId().equals(tenantId))
            .orElseThrow(() -> new SubscriptionNotFoundException(id));

        subscriptionRepository.delete(subscription);
        log.info("Deleted subscription: {}", id);
    }

    /**
     * Activate a subscription.
     */
    @Transactional
    public void activateSubscription(UUID id) {
        FhirSubscription subscription = subscriptionRepository.findById(id)
            .orElseThrow(() -> new SubscriptionNotFoundException(id));

        subscription.setStatus(FhirSubscription.SubscriptionStatus.ACTIVE);
        subscription.setReason("Subscription activated");
        subscriptionRepository.save(subscription);

        log.info("Activated subscription: {}", id);
    }

    /**
     * Deactivate a subscription.
     */
    @Transactional
    public void deactivateSubscription(UUID id, String reason) {
        FhirSubscription subscription = subscriptionRepository.findById(id)
            .orElseThrow(() -> new SubscriptionNotFoundException(id));

        subscription.setStatus(FhirSubscription.SubscriptionStatus.OFF);
        subscription.setReason(reason);
        subscriptionRepository.save(subscription);

        log.info("Deactivated subscription: {} - {}", id, reason);
    }

    /**
     * Process a resource change and notify matching subscriptions.
     */
    @Async
    public void processResourceChange(
            String tenantId,
            String resourceType,
            String resourceId,
            Object resource,
            SubscriptionNotification.EventType eventType) {

        log.debug("Processing resource change: {}/{} - {}", resourceType, resourceId, eventType);

        List<FhirSubscription> subscriptions = subscriptionRepository.findActiveSubscriptionsForResource(
            tenantId, resourceType, Instant.now());

        for (FhirSubscription subscription : subscriptions) {
            if (matchesCriteria(subscription, resource)) {
                sendNotification(subscription, resourceId, resource, eventType);
            }
        }
    }

    /**
     * Send notification to subscription channel.
     */
    private void sendNotification(
            FhirSubscription subscription,
            String resourceId,
            Object resource,
            SubscriptionNotification.EventType eventType) {

        SubscriptionNotification notification = SubscriptionNotification.builder()
            .id(UUID.randomUUID().toString())
            .subscriptionId(subscription.getId())
            .eventType(eventType)
            .timestamp(Instant.now())
            .resourceType(subscription.getResourceType())
            .resourceId(resourceId)
            .resource(resource)
            .tenantId(subscription.getTenantId())
            .sequenceNumber(subscription.getNotificationCount() + 1)
            .build();

        try {
            switch (subscription.getChannelType()) {
                case REST_HOOK -> sendRestHookNotification(subscription, notification);
                case WEBSOCKET -> sendWebSocketNotification(subscription, notification);
                case EMAIL -> sendEmailNotification(subscription, notification);
                case MESSAGE -> sendMessageNotification(subscription, notification);
            }

            subscription.recordSuccess();
            subscriptionRepository.save(subscription);

        } catch (Exception e) {
            log.error("Failed to send notification for subscription {}: {}",
                subscription.getId(), e.getMessage());
            subscription.recordError(e.getMessage());
            subscriptionRepository.save(subscription);
        }
    }

    /**
     * Send REST webhook notification.
     */
    private void sendRestHookNotification(
            FhirSubscription subscription,
            SubscriptionNotification notification) throws Exception {

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        if (subscription.getChannelHeaders() != null) {
            subscription.getChannelHeaders().forEach(headers::add);
        }

        String body = objectMapper.writeValueAsString(notification);
        HttpEntity<String> request = new HttpEntity<>(body, headers);

        restTemplate.postForEntity(
            subscription.getChannelEndpoint(),
            request,
            String.class
        );

        log.debug("Sent REST webhook notification to: {}", subscription.getChannelEndpoint());
    }

    /**
     * Send WebSocket notification.
     */
    private void sendWebSocketNotification(
            FhirSubscription subscription,
            SubscriptionNotification notification) throws Exception {

        webSocketHandler.sendNotification(subscription.getTenantId(), notification);
        log.debug("Sent WebSocket notification for subscription: {}", subscription.getId());
    }

    /**
     * Send email notification.
     */
    private void sendEmailNotification(
            FhirSubscription subscription,
            SubscriptionNotification notification) {
        // Email implementation would go here
        log.info("Email notification for subscription {} - not implemented", subscription.getId());
    }

    /**
     * Send FHIR message notification.
     */
    private void sendMessageNotification(
            FhirSubscription subscription,
            SubscriptionNotification notification) {
        // FHIR Messaging implementation would go here
        log.info("Message notification for subscription {} - not implemented", subscription.getId());
    }

    /**
     * Check if resource matches subscription criteria.
     *
     * Simple implementation - in production, use FHIR search parameter parsing.
     */
    private boolean matchesCriteria(FhirSubscription subscription, Object resource) {
        // For now, match on resource type only
        // A full implementation would parse the criteria string and evaluate against resource
        return true;
    }

    /**
     * Validate subscription fields.
     */
    private void validateSubscription(FhirSubscription subscription) {
        if (subscription.getCriteria() == null || subscription.getCriteria().isEmpty()) {
            throw new IllegalArgumentException("Subscription criteria is required");
        }
        if (subscription.getChannelType() == null) {
            throw new IllegalArgumentException("Channel type is required");
        }
        if (subscription.getChannelType() == FhirSubscription.ChannelType.REST_HOOK
                && (subscription.getChannelEndpoint() == null || subscription.getChannelEndpoint().isEmpty())) {
            throw new IllegalArgumentException("Endpoint is required for REST webhook channel");
        }
    }

    /**
     * Clean up expired subscriptions.
     */
    @Scheduled(fixedRate = 60000) // Every minute
    @Transactional
    public void cleanupExpiredSubscriptions() {
        List<FhirSubscription> expired = subscriptionRepository.findExpiredSubscriptions(Instant.now());
        for (FhirSubscription subscription : expired) {
            subscription.setStatus(FhirSubscription.SubscriptionStatus.ENDED);
            subscription.setReason("Subscription expired");
            subscriptionRepository.save(subscription);
            log.info("Expired subscription: {}", subscription.getId());
        }
    }

    /**
     * Exception for subscription not found.
     */
    public static class SubscriptionNotFoundException extends RuntimeException {
        public SubscriptionNotFoundException(UUID id) {
            super("Subscription not found: " + id);
        }
    }
}
