package com.healthdata.notification.api.v1;

import com.healthdata.notification.api.v1.dto.ProviderPreferenceRequest;
import com.healthdata.notification.api.v1.dto.ProviderPreferenceResponse;
import com.healthdata.notification.application.ProviderPreferenceService;
import com.healthdata.notification.domain.model.NotificationType;
import com.healthdata.notification.domain.model.ProviderNotificationPreference;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * REST API for provider notification preferences.
 * Issue #148: Smart Notification Preferences
 */
@RestController
@RequestMapping("/api/v1/providers/{providerId}/notifications/preferences")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Provider Notification Preferences",
     description = "Manage notification preferences for providers")
public class ProviderPreferenceController {

    private final ProviderPreferenceService preferenceService;

    /**
     * Get all notification preferences for a provider.
     */
    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'PROVIDER') or @providerSecurity.isProvider(#providerId)")
    @Operation(summary = "Get provider notification preferences",
               description = "Returns all notification type preferences for a provider")
    public ResponseEntity<List<ProviderPreferenceResponse>> getProviderPreferences(
            @Parameter(description = "Provider's unique identifier")
            @PathVariable UUID providerId,
            @Parameter(description = "Tenant identifier")
            @RequestHeader("X-Tenant-ID") String tenantId,
            @Parameter(description = "Include defaults for missing types")
            @RequestParam(defaultValue = "true") boolean includeDefaults) {

        log.info("Getting notification preferences for provider {} in tenant {}",
            providerId, tenantId);

        List<ProviderNotificationPreference> preferences;
        if (includeDefaults) {
            preferences = preferenceService.getOrCreateAllPreferences(tenantId, providerId);
        } else {
            preferences = preferenceService.getProviderPreferences(tenantId, providerId);
        }

        List<ProviderPreferenceResponse> responses = preferences.stream()
            .map(ProviderPreferenceResponse::fromEntity)
            .toList();

        return ResponseEntity.ok(responses);
    }

    /**
     * Get preference for a specific notification type.
     */
    @GetMapping("/{notificationType}")
    @PreAuthorize("hasAnyRole('ADMIN', 'PROVIDER') or @providerSecurity.isProvider(#providerId)")
    @Operation(summary = "Get preference for notification type",
               description = "Returns preference settings for a specific notification type")
    public ResponseEntity<ProviderPreferenceResponse> getPreferenceByType(
            @Parameter(description = "Provider's unique identifier")
            @PathVariable UUID providerId,
            @Parameter(description = "Notification type")
            @PathVariable NotificationType notificationType,
            @Parameter(description = "Tenant identifier")
            @RequestHeader("X-Tenant-ID") String tenantId) {

        ProviderNotificationPreference preference =
            preferenceService.getOrCreatePreference(tenantId, providerId, notificationType);

        return ResponseEntity.ok(ProviderPreferenceResponse.fromEntity(preference));
    }

    /**
     * Update preference for a notification type.
     */
    @PutMapping("/{notificationType}")
    @PreAuthorize("hasAnyRole('ADMIN', 'PROVIDER') or @providerSecurity.isProvider(#providerId)")
    @Operation(summary = "Update notification preference",
               description = "Update preference settings for a notification type")
    public ResponseEntity<ProviderPreferenceResponse> updatePreference(
            @Parameter(description = "Provider's unique identifier")
            @PathVariable UUID providerId,
            @Parameter(description = "Notification type")
            @PathVariable NotificationType notificationType,
            @Parameter(description = "Tenant identifier")
            @RequestHeader("X-Tenant-ID") String tenantId,
            @Valid @RequestBody ProviderPreferenceRequest request) {

        log.info("Updating notification preference for provider {} type {} in tenant {}",
            providerId, notificationType, tenantId);

        ProviderNotificationPreference preference = preferenceService.updatePreference(
            tenantId,
            providerId,
            notificationType,
            request.getEnabled(),
            request.getDeliveryMethod(),
            request.getAdditionalDeliveryMethods(),
            request.getMinUrgency(),
            request.getDigestEnabled(),
            request.getDigestFrequency()
        );

        return ResponseEntity.ok(ProviderPreferenceResponse.fromEntity(preference));
    }

    /**
     * Bulk update multiple preferences.
     */
    @PutMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'PROVIDER') or @providerSecurity.isProvider(#providerId)")
    @Operation(summary = "Bulk update preferences",
               description = "Update multiple notification preferences at once")
    public ResponseEntity<List<ProviderPreferenceResponse>> bulkUpdatePreferences(
            @Parameter(description = "Provider's unique identifier")
            @PathVariable UUID providerId,
            @Parameter(description = "Tenant identifier")
            @RequestHeader("X-Tenant-ID") String tenantId,
            @Valid @RequestBody List<ProviderPreferenceRequest> requests) {

        log.info("Bulk updating {} notification preferences for provider {} in tenant {}",
            requests.size(), providerId, tenantId);

        List<ProviderPreferenceService.PreferenceUpdateRequest> updates = requests.stream()
            .map(req -> ProviderPreferenceService.PreferenceUpdateRequest.builder()
                .notificationType(req.getNotificationType())
                .enabled(req.getEnabled())
                .deliveryMethod(req.getDeliveryMethod())
                .additionalDeliveryMethods(req.getAdditionalDeliveryMethods())
                .minUrgency(req.getMinUrgency())
                .digestEnabled(req.getDigestEnabled())
                .digestFrequency(req.getDigestFrequency())
                .build())
            .toList();

        List<ProviderNotificationPreference> preferences =
            preferenceService.bulkUpdatePreferences(tenantId, providerId, updates);

        List<ProviderPreferenceResponse> responses = preferences.stream()
            .map(ProviderPreferenceResponse::fromEntity)
            .toList();

        return ResponseEntity.ok(responses);
    }

    /**
     * Reset preferences to defaults.
     */
    @PostMapping("/reset")
    @PreAuthorize("hasAnyRole('ADMIN', 'PROVIDER') or @providerSecurity.isProvider(#providerId)")
    @Operation(summary = "Reset to defaults",
               description = "Reset all notification preferences to default values")
    public ResponseEntity<List<ProviderPreferenceResponse>> resetToDefaults(
            @Parameter(description = "Provider's unique identifier")
            @PathVariable UUID providerId,
            @Parameter(description = "Tenant identifier")
            @RequestHeader("X-Tenant-ID") String tenantId) {

        log.info("Resetting notification preferences to defaults for provider {} in tenant {}",
            providerId, tenantId);

        List<ProviderNotificationPreference> preferences =
            preferenceService.resetToDefaults(tenantId, providerId);

        List<ProviderPreferenceResponse> responses = preferences.stream()
            .map(ProviderPreferenceResponse::fromEntity)
            .toList();

        return ResponseEntity.ok(responses);
    }

    /**
     * Get available notification types with metadata.
     */
    @GetMapping("/types")
    @Operation(summary = "Get notification types",
               description = "Returns all available notification types with metadata")
    public ResponseEntity<List<Map<String, Object>>> getNotificationTypes() {
        List<Map<String, Object>> types = Arrays.stream(NotificationType.values())
            .map(type -> Map.<String, Object>of(
                "type", type.name(),
                "displayName", type.getDisplayName(),
                "description", type.getDescription(),
                "alwaysEnabled", type.isAlwaysEnabled()
            ))
            .toList();

        return ResponseEntity.ok(types);
    }
}
