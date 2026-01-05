package com.healthdata.fhir.subscription;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;
import com.healthdata.audit.annotations.Audited;
import com.healthdata.audit.models.AuditAction;

/**
 * REST Controller for FHIR Subscription management.
 *
 * Provides CRUD operations for FHIR Subscriptions supporting
 * REST webhook and WebSocket notification channels.
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/Subscription")
@RequiredArgsConstructor
@Tag(name = "Subscription", description = "FHIR Subscriptions for real-time notifications")
@SecurityRequirement(name = "smart-oauth2")
public class SubscriptionController {

    private final SubscriptionService subscriptionService;
    private final ObjectMapper objectMapper;

    @Operation(
        summary = "Create Subscription",
        description = "Creates a new FHIR Subscription for real-time notifications on resource changes.",
        operationId = "createSubscription"
    )
    @ApiResponses({
        @ApiResponse(responseCode = "201", description = "Subscription created successfully"),
        @ApiResponse(responseCode = "400", description = "Invalid subscription request"),
        @ApiResponse(responseCode = "401", description = "Unauthorized")
    })
    @Audited(action = AuditAction.CREATE, includeRequestPayload = false, includeResponsePayload = false)
    @PostMapping
    public ResponseEntity<FhirSubscription> createSubscription(
            @Parameter(description = "Tenant ID for multi-tenant isolation", required = true)
            @RequestHeader("X-Tenant-ID") String tenantId,
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                description = "Subscription configuration including criteria and notification channel",
                required = true
            )
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

    @Operation(
        summary = "Get Subscription",
        description = "Retrieves a subscription by its ID.",
        operationId = "getSubscription"
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Subscription found"),
        @ApiResponse(responseCode = "404", description = "Subscription not found")
    })
    @Audited(action = AuditAction.READ, includeRequestPayload = false, includeResponsePayload = false)
    @GetMapping("/{id}")
    public ResponseEntity<FhirSubscription> getSubscription(
            @Parameter(description = "Tenant ID for multi-tenant isolation", required = true)
            @RequestHeader("X-Tenant-ID") String tenantId,
            @Parameter(description = "Subscription ID", required = true)
            @PathVariable UUID id) {

        return subscriptionService.getSubscription(id, tenantId)
            .map(ResponseEntity::ok)
            .orElse(ResponseEntity.notFound().build());
    }

    @Operation(
        summary = "List Subscriptions",
        description = "Retrieves all subscriptions for the tenant, optionally filtered by status.",
        operationId = "listSubscriptions"
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "List of subscriptions")
    })
    @Audited(action = AuditAction.READ, includeRequestPayload = false, includeResponsePayload = false)
    @GetMapping
    public ResponseEntity<List<FhirSubscription>> getSubscriptions(
            @Parameter(description = "Tenant ID for multi-tenant isolation", required = true)
            @RequestHeader("X-Tenant-ID") String tenantId,
            @Parameter(description = "Filter by subscription status")
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
    @Audited(action = AuditAction.UPDATE, includeRequestPayload = false, includeResponsePayload = false)
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

    @Operation(
        summary = "Delete Subscription",
        description = "Deletes a subscription by its ID.",
        operationId = "deleteSubscription"
    )
    @ApiResponses({
        @ApiResponse(responseCode = "204", description = "Subscription deleted successfully"),
        @ApiResponse(responseCode = "404", description = "Subscription not found")
    })
    @Audited(action = AuditAction.DELETE, includeRequestPayload = false, includeResponsePayload = false)
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteSubscription(
            @Parameter(description = "Tenant ID for multi-tenant isolation", required = true)
            @RequestHeader("X-Tenant-ID") String tenantId,
            @Parameter(description = "Subscription ID", required = true)
            @PathVariable UUID id) {

        subscriptionService.deleteSubscription(id, tenantId);
        return ResponseEntity.noContent().build();
    }

    @Operation(
        summary = "Activate Subscription",
        description = "Activates a subscription so it starts receiving notifications.",
        operationId = "activateSubscription"
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Subscription activated"),
        @ApiResponse(responseCode = "404", description = "Subscription not found")
    })
    @Audited(action = AuditAction.CREATE, includeRequestPayload = false, includeResponsePayload = false)
    @PostMapping("/{id}/$activate")
    public ResponseEntity<Void> activateSubscription(
            @Parameter(description = "Tenant ID for multi-tenant isolation", required = true)
            @RequestHeader("X-Tenant-ID") String tenantId,
            @Parameter(description = "Subscription ID", required = true)
            @PathVariable UUID id) {

        subscriptionService.getSubscription(id, tenantId)
            .orElseThrow(() -> new SubscriptionService.SubscriptionNotFoundException(id));

        subscriptionService.activateSubscription(id);
        return ResponseEntity.ok().build();
    }

    @Operation(
        summary = "Deactivate Subscription",
        description = "Deactivates a subscription so it stops receiving notifications.",
        operationId = "deactivateSubscription"
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Subscription deactivated"),
        @ApiResponse(responseCode = "404", description = "Subscription not found")
    })
    @Audited(action = AuditAction.CREATE, includeRequestPayload = false, includeResponsePayload = false)
    @PostMapping("/{id}/$deactivate")
    public ResponseEntity<Void> deactivateSubscription(
            @Parameter(description = "Tenant ID for multi-tenant isolation", required = true)
            @RequestHeader("X-Tenant-ID") String tenantId,
            @Parameter(description = "Subscription ID", required = true)
            @PathVariable UUID id,
            @Parameter(description = "Reason for deactivation")
            @RequestParam(defaultValue = "User requested deactivation") String reason) {

        subscriptionService.getSubscription(id, tenantId)
            .orElseThrow(() -> new SubscriptionService.SubscriptionNotFoundException(id));

        subscriptionService.deactivateSubscription(id, reason);
        return ResponseEntity.ok().build();
    }

    @Operation(
        summary = "Get Subscription Status",
        description = "Retrieves detailed status information for a subscription including notification counts and errors.",
        operationId = "getSubscriptionStatus"
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Subscription status retrieved"),
        @ApiResponse(responseCode = "404", description = "Subscription not found")
    })
    @Audited(action = AuditAction.READ, includeRequestPayload = false, includeResponsePayload = false)
    @GetMapping("/{id}/$status")
    public ResponseEntity<SubscriptionStatusResponse> getStatus(
            @Parameter(description = "Tenant ID for multi-tenant isolation", required = true)
            @RequestHeader("X-Tenant-ID") String tenantId,
            @Parameter(description = "Subscription ID", required = true)
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
