package com.healthdata.notification.api.v1;

import com.healthdata.notification.api.v1.dto.*;
import com.healthdata.notification.application.PreferenceService;
import com.healthdata.notification.domain.model.NotificationPreference;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/preferences")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Preferences", description = "User notification preferences")
public class PreferenceController {

    private final PreferenceService preferenceService;

    @GetMapping("/{userId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN') or #userId == authentication.name")
    @Operation(summary = "Get user preferences")
    public ResponseEntity<List<PreferenceResponse>> getUserPreferences(
            @PathVariable String userId,
            @RequestHeader("X-Tenant-ID") String tenantId) {
        
        List<NotificationPreference> preferences = 
            preferenceService.getUserPreferences(tenantId, userId);
        
        List<PreferenceResponse> responses = preferences.stream()
            .map(PreferenceResponse::fromEntity)
            .toList();

        return ResponseEntity.ok(responses);
    }

    @PutMapping("/{userId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN') or #userId == authentication.name")
    @Operation(summary = "Set user preference for a channel")
    public ResponseEntity<PreferenceResponse> setPreference(
            @PathVariable String userId,
            @Valid @RequestBody PreferenceRequest request,
            @RequestHeader("X-Tenant-ID") String tenantId) {
        
        log.info("Setting preference for user {} channel {} in tenant {}", 
            userId, request.getChannel(), tenantId);

        NotificationPreference preference = preferenceService.setPreference(
            tenantId, 
            userId, 
            request.getChannel(),
            request.getEnabled() != null ? request.getEnabled() : true,
            request.getEmail(),
            request.getPhone()
        );

        // Update additional settings
        if (request.getQuietHoursEnabled() != null || 
            request.getQuietHoursStart() != null ||
            request.getQuietHoursEnd() != null ||
            request.getTimezone() != null) {
            
            preference = preferenceService.updatePreference(
                preference.getId(),
                PreferenceService.UpdatePreferenceRequest.builder()
                    .quietHoursEnabled(request.getQuietHoursEnabled())
                    .quietHoursStart(request.getQuietHoursStart())
                    .quietHoursEnd(request.getQuietHoursEnd())
                    .timezone(request.getTimezone())
                    .build()
            );
        }

        return ResponseEntity.ok(PreferenceResponse.fromEntity(preference));
    }

    @DeleteMapping("/{userId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
    @Operation(summary = "Delete all preferences for a user")
    public ResponseEntity<Void> deleteUserPreferences(
            @PathVariable String userId,
            @RequestHeader("X-Tenant-ID") String tenantId) {
        
        log.info("Deleting all preferences for user {} in tenant {}", userId, tenantId);
        preferenceService.deleteUserPreferences(tenantId, userId);
        return ResponseEntity.noContent().build();
    }
}
