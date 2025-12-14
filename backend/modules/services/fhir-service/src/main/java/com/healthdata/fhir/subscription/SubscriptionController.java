package com.healthdata.fhir.subscription;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/**
 * REST Controller for FHIR Subscription management.
 *
 * Provides CRUD operations for FHIR Subscriptions.
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/Subscription")
@RequiredArgsConstructor
public class SubscriptionController {

    private final SubscriptionService subscriptionService;
    private final ObjectMapper objectMapper;

    /**
     * Create a new subscription.
     */
    @PostMapping
    public ResponseEntity<FhirSubscription> createSubscription(
            @RequestHeader("X-Tenant-ID") String tenantId,
            @RequestBody SubscriptionRequest request) {

        log.info("Creating subscription for resource type: {}", request.getCriteria());

        FhirSubscription subscription = FhirSubscription.builder()
            .tenantId(tenantId)
            .criteria(request.getCriteria())
            .resourceType(extractResourceType(request.getCriteria()))
            .channelType(request.getChannel().getType())
            .channelEndpoint(request.getChannel().getEndpoint())
            .channelPayload(request.getChannel().getPayload())
            .channelHeaders(request.getChannel().getHeaders())
            .endTime(request.getEndTime())
            .tag(request.getTag())
            .contactEmail(request.getContactEmail())
            .resourceJson(serializeToJson(request))
            .build();

        FhirSubscription created = subscriptionService.createSubscription(subscription);

        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    /**
     * Get subscription by ID.
     */
    @GetMapping("/{id}")
    public ResponseEntity<FhirSubscription> getSubscription(
            @RequestHeader("X-Tenant-ID") String tenantId,
            @PathVariable UUID id) {

        return subscriptionService.getSubscription(id, tenantId)
            .map(ResponseEntity::ok)
            .orElse(ResponseEntity.notFound().build());
    }

    /**
     * Get all subscriptions for tenant.
     */
    @GetMapping
    public ResponseEntity<List<FhirSubscription>> getSubscriptions(
            @RequestHeader("X-Tenant-ID") String tenantId,
            @RequestParam(required = false) FhirSubscription.SubscriptionStatus status) {

        List<FhirSubscription> subscriptions;
        if (status != null) {
            subscriptions = subscriptionService.getSubscriptions(tenantId).stream()
                .filter(s -> s.getStatus() == status)
                .toList();
        } else {
            subscriptions = subscriptionService.getSubscriptions(tenantId);
        }

        return ResponseEntity.ok(subscriptions);
    }

    /**
     * Update subscription.
     */
    @PutMapping("/{id}")
    public ResponseEntity<FhirSubscription> updateSubscription(
            @RequestHeader("X-Tenant-ID") String tenantId,
            @PathVariable UUID id,
            @RequestBody SubscriptionRequest request) {

        FhirSubscription updates = FhirSubscription.builder()
            .criteria(request.getCriteria())
            .channelEndpoint(request.getChannel() != null ? request.getChannel().getEndpoint() : null)
            .channelHeaders(request.getChannel() != null ? request.getChannel().getHeaders() : null)
            .endTime(request.getEndTime())
            .tag(request.getTag())
            .build();

        FhirSubscription updated = subscriptionService.updateSubscription(id, updates, tenantId);
        return ResponseEntity.ok(updated);
    }

    /**
     * Delete subscription.
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteSubscription(
            @RequestHeader("X-Tenant-ID") String tenantId,
            @PathVariable UUID id) {

        subscriptionService.deleteSubscription(id, tenantId);
        return ResponseEntity.noContent().build();
    }

    /**
     * Activate subscription.
     */
    @PostMapping("/{id}/$activate")
    public ResponseEntity<Void> activateSubscription(
            @RequestHeader("X-Tenant-ID") String tenantId,
            @PathVariable UUID id) {

        subscriptionService.getSubscription(id, tenantId)
            .orElseThrow(() -> new SubscriptionService.SubscriptionNotFoundException(id));

        subscriptionService.activateSubscription(id);
        return ResponseEntity.ok().build();
    }

    /**
     * Deactivate subscription.
     */
    @PostMapping("/{id}/$deactivate")
    public ResponseEntity<Void> deactivateSubscription(
            @RequestHeader("X-Tenant-ID") String tenantId,
            @PathVariable UUID id,
            @RequestParam(defaultValue = "User requested deactivation") String reason) {

        subscriptionService.getSubscription(id, tenantId)
            .orElseThrow(() -> new SubscriptionService.SubscriptionNotFoundException(id));

        subscriptionService.deactivateSubscription(id, reason);
        return ResponseEntity.ok().build();
    }

    /**
     * Get subscription status.
     */
    @GetMapping("/{id}/$status")
    public ResponseEntity<SubscriptionStatusResponse> getStatus(
            @RequestHeader("X-Tenant-ID") String tenantId,
            @PathVariable UUID id) {

        FhirSubscription subscription = subscriptionService.getSubscription(id, tenantId)
            .orElseThrow(() -> new SubscriptionService.SubscriptionNotFoundException(id));

        SubscriptionStatusResponse response = SubscriptionStatusResponse.builder()
            .id(subscription.getId())
            .status(subscription.getStatus())
            .reason(subscription.getReason())
            .notificationCount(subscription.getNotificationCount())
            .errorCount(subscription.getErrorCount())
            .lastError(subscription.getLastError())
            .lastNotificationAt(subscription.getLastNotificationAt())
            .build();

        return ResponseEntity.ok(response);
    }

    /**
     * Extract resource type from criteria.
     */
    private String extractResourceType(String criteria) {
        if (criteria == null || criteria.isEmpty()) {
            return "Unknown";
        }
        // Criteria format: "ResourceType?params" or just "ResourceType"
        int queryIndex = criteria.indexOf('?');
        return queryIndex > 0 ? criteria.substring(0, queryIndex) : criteria;
    }

    /**
     * Serialize request to JSON for storage.
     */
    private String serializeToJson(SubscriptionRequest request) {
        try {
            return objectMapper.writeValueAsString(request);
        } catch (Exception e) {
            return "{}";
        }
    }

    /**
     * Exception handler for subscription not found.
     */
    @ExceptionHandler(SubscriptionService.SubscriptionNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleNotFound(SubscriptionService.SubscriptionNotFoundException e) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
            .body(new ErrorResponse("not_found", e.getMessage()));
    }

    /**
     * Exception handler for validation errors.
     */
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorResponse> handleValidationError(IllegalArgumentException e) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
            .body(new ErrorResponse("validation_error", e.getMessage()));
    }

    /**
     * Subscription request DTO.
     */
    @lombok.Data
    public static class SubscriptionRequest {
        private String criteria;
        private ChannelRequest channel;
        private java.time.Instant endTime;
        private String tag;
        private String contactEmail;

        @lombok.Data
        public static class ChannelRequest {
            private FhirSubscription.ChannelType type;
            private String endpoint;
            private String payload;
            private java.util.Map<String, String> headers;
        }
    }

    /**
     * Subscription status response DTO.
     */
    @lombok.Data
    @lombok.Builder
    public static class SubscriptionStatusResponse {
        private UUID id;
        private FhirSubscription.SubscriptionStatus status;
        private String reason;
        private long notificationCount;
        private int errorCount;
        private String lastError;
        private java.time.Instant lastNotificationAt;
    }

    /**
     * Error response DTO.
     */
    @lombok.Data
    @lombok.AllArgsConstructor
    public static class ErrorResponse {
        private String error;
        private String message;
    }
}
