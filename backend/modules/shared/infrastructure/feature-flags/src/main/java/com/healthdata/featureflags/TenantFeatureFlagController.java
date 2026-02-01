package com.healthdata.featureflags;

import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * Tenant Feature Flag Controller
 *
 * REST API for managing tenant feature flags.
 *
 * Security:
 * - GET endpoints: ADMIN, EVALUATOR, ANALYST roles
 * - PUT endpoints: ADMIN only
 *
 * HIPAA Compliance:
 * - Multi-tenant isolation via X-Tenant-ID header
 * - Audit trail via service layer
 */
@RestController
@RequestMapping("/api/v1/tenant-features")
@RequiredArgsConstructor
@Slf4j
public class TenantFeatureFlagController {

    private final TenantFeatureFlagService featureFlagService;

    /**
     * Get all feature flags for a tenant
     *
     * @param tenantId Tenant ID from X-Tenant-ID header
     * @return List of feature flags
     */
    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'EVALUATOR', 'ANALYST')")
    public ResponseEntity<List<FeatureFlagResponse>> getFeatureFlags(
            @RequestHeader("X-Tenant-ID") String tenantId) {
        log.info("GET /api/v1/tenant-features - tenantId={}", tenantId);

        List<FeatureFlagResponse> flags = featureFlagService.getFeatureFlags(tenantId)
                .stream()
                .map(this::toResponse)
                .toList();

        return ResponseEntity.ok(flags);
    }

    /**
     * Get a specific feature flag
     *
     * @param tenantId   Tenant ID from X-Tenant-ID header
     * @param featureKey Feature key (e.g., "twilio-sms-reminders")
     * @return Feature flag details
     */
    @GetMapping("/{featureKey}")
    @PreAuthorize("hasAnyRole('ADMIN', 'EVALUATOR', 'ANALYST')")
    public ResponseEntity<FeatureFlagResponse> getFeatureFlag(
            @RequestHeader("X-Tenant-ID") String tenantId,
            @PathVariable String featureKey) {
        log.info("GET /api/v1/tenant-features/{} - tenantId={}", featureKey, tenantId);

        boolean enabled = featureFlagService.isFeatureEnabled(tenantId, featureKey);
        Map<String, Object> config = featureFlagService.getFeatureConfig(tenantId, featureKey);

        FeatureFlagResponse response = new FeatureFlagResponse();
        response.setFeatureKey(featureKey);
        response.setEnabled(enabled);
        response.setConfig(config);

        return ResponseEntity.ok(response);
    }

    /**
     * Enable a feature for a tenant
     *
     * @param tenantId   Tenant ID from X-Tenant-ID header
     * @param featureKey Feature key
     * @param request    Feature configuration
     * @param userId     User ID from X-User-ID header (for audit trail)
     * @return Success response
     */
    @PutMapping("/{featureKey}/enable")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<MessageResponse> enableFeature(
            @RequestHeader("X-Tenant-ID") String tenantId,
            @PathVariable String featureKey,
            @RequestBody(required = false) FeatureFlagConfigRequest request,
            @RequestHeader("X-User-ID") String userId) {
        log.info("PUT /api/v1/tenant-features/{}/enable - tenantId={}, userId={}", featureKey, tenantId, userId);

        Map<String, Object> config = request != null ? request.getConfig() : Map.of();
        featureFlagService.enableFeature(tenantId, featureKey, config, userId);

        return ResponseEntity.ok(new MessageResponse("Feature enabled successfully"));
    }

    /**
     * Disable a feature for a tenant
     *
     * @param tenantId   Tenant ID from X-Tenant-ID header
     * @param featureKey Feature key
     * @param userId     User ID from X-User-ID header (for audit trail)
     * @return Success response
     */
    @PutMapping("/{featureKey}/disable")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<MessageResponse> disableFeature(
            @RequestHeader("X-Tenant-ID") String tenantId,
            @PathVariable String featureKey,
            @RequestHeader("X-User-ID") String userId) {
        log.info("PUT /api/v1/tenant-features/{}/disable - tenantId={}, userId={}", featureKey, tenantId, userId);

        featureFlagService.disableFeature(tenantId, featureKey, userId);

        return ResponseEntity.ok(new MessageResponse("Feature disabled successfully"));
    }

    /**
     * Update feature configuration without changing enabled status
     *
     * @param tenantId   Tenant ID from X-Tenant-ID header
     * @param featureKey Feature key
     * @param request    New configuration
     * @param userId     User ID from X-User-ID header (for audit trail)
     * @return Success response
     */
    @PutMapping("/{featureKey}/config")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<MessageResponse> updateFeatureConfig(
            @RequestHeader("X-Tenant-ID") String tenantId,
            @PathVariable String featureKey,
            @RequestBody FeatureFlagConfigRequest request,
            @RequestHeader("X-User-ID") String userId) {
        log.info("PUT /api/v1/tenant-features/{}/config - tenantId={}, userId={}", featureKey, tenantId, userId);

        featureFlagService.updateFeatureConfig(tenantId, featureKey, request.getConfig(), userId);

        return ResponseEntity.ok(new MessageResponse("Feature configuration updated successfully"));
    }

    /**
     * Exception handler for FeatureFlagDisabledException
     */
    @ExceptionHandler(FeatureFlagDisabledException.class)
    public ResponseEntity<ErrorResponse> handleFeatureFlagDisabled(FeatureFlagDisabledException ex) {
        log.warn("Feature flag disabled: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(new ErrorResponse("FEATURE_DISABLED", ex.getMessage()));
    }

    // Helper methods

    private FeatureFlagResponse toResponse(TenantFeatureFlagEntity entity) {
        FeatureFlagResponse response = new FeatureFlagResponse();
        response.setFeatureKey(entity.getFeatureKey());
        response.setEnabled(entity.getEnabled());
        response.setConfig(featureFlagService.getFeatureConfig(entity.getTenantId(), entity.getFeatureKey()));
        response.setCreatedAt(entity.getCreatedAt());
        response.setUpdatedAt(entity.getUpdatedAt());
        return response;
    }

    // DTOs

    @Data
    public static class FeatureFlagResponse {
        private String featureKey;
        private Boolean enabled;
        private Map<String, Object> config;
        private java.time.Instant createdAt;
        private java.time.Instant updatedAt;
    }

    @Data
    public static class FeatureFlagConfigRequest {
        private Map<String, Object> config;
    }

    @Data
    public static class MessageResponse {
        private String message;

        public MessageResponse(String message) {
            this.message = message;
        }
    }

    @Data
    public static class ErrorResponse {
        private String code;
        private String message;

        public ErrorResponse(String code, String message) {
            this.code = code;
            this.message = message;
        }
    }
}
