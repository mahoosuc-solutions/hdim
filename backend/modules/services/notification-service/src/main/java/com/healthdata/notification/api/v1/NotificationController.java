package com.healthdata.notification.api.v1;

import com.healthdata.notification.api.v1.dto.*;
import com.healthdata.notification.application.NotificationService;
import com.healthdata.notification.application.PreferenceService;
import com.healthdata.notification.application.TemplateService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/**
 * REST API for notification management.
 *
 * Endpoints:
 * - POST /api/v1/notifications - Send notification
 * - GET /api/v1/notifications/{id} - Get notification status
 * - POST /api/v1/notifications/bulk - Send bulk notifications
 * - GET/POST /api/v1/templates - Manage templates
 * - GET/PUT /api/v1/preferences/{userId} - User preferences
 */
@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Notifications", description = "Notification management API")
public class NotificationController {

    private final NotificationService notificationService;
    private final TemplateService templateService;
    private final PreferenceService preferenceService;

    // ==================== NOTIFICATIONS ====================

    @PostMapping("/notifications")
    @Operation(summary = "Send a notification")
    @PreAuthorize("hasAnyRole('ADMIN', 'EVALUATOR', 'SYSTEM')")
    public ResponseEntity<NotificationResponse> sendNotification(
            @Valid @RequestBody SendNotificationRequest request,
            @RequestHeader("X-Tenant-ID") String tenantId) {

        log.info("Received notification request for recipient: {}", request.getRecipientId());
        NotificationResponse response = notificationService.sendNotification(request, tenantId);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/notifications/{id}")
    @Operation(summary = "Get notification by ID")
    @PreAuthorize("hasAnyRole('ADMIN', 'EVALUATOR', 'VIEWER')")
    public ResponseEntity<NotificationResponse> getNotification(
            @PathVariable UUID id,
            @RequestHeader("X-Tenant-ID") String tenantId) {

        return notificationService.getNotification(id, tenantId)
            .map(ResponseEntity::ok)
            .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/notifications")
    @Operation(summary = "Get notifications for a recipient")
    @PreAuthorize("hasAnyRole('ADMIN', 'EVALUATOR', 'VIEWER')")
    public ResponseEntity<Page<NotificationResponse>> getNotifications(
            @RequestParam String recipientId,
            @RequestHeader("X-Tenant-ID") String tenantId,
            Pageable pageable) {

        Page<NotificationResponse> notifications =
            notificationService.getNotificationsForRecipient(recipientId, tenantId, pageable);
        return ResponseEntity.ok(notifications);
    }

    @PostMapping("/notifications/bulk")
    @Operation(summary = "Send bulk notifications")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<NotificationResponse>> sendBulkNotifications(
            @Valid @RequestBody List<SendNotificationRequest> requests,
            @RequestHeader("X-Tenant-ID") String tenantId) {

        log.info("Received bulk notification request with {} items", requests.size());
        List<NotificationResponse> responses = notificationService.sendBulkNotifications(requests, tenantId);
        return ResponseEntity.ok(responses);
    }

    // ==================== TEMPLATES ====================

    @PostMapping("/templates")
    @Operation(summary = "Create a notification template")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<NotificationTemplateResponse> createTemplate(
            @Valid @RequestBody NotificationTemplateRequest request,
            @RequestHeader("X-Tenant-ID") String tenantId) {

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String createdBy = auth != null ? auth.getName() : "system";
        NotificationTemplateResponse response = templateService.createTemplate(request, tenantId, createdBy);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/templates/{id}")
    @Operation(summary = "Get template by ID")
    @PreAuthorize("hasAnyRole('ADMIN', 'EVALUATOR')")
    public ResponseEntity<NotificationTemplateResponse> getTemplate(
            @PathVariable UUID id,
            @RequestHeader("X-Tenant-ID") String tenantId) {

        return templateService.getTemplate(id, tenantId)
            .map(ResponseEntity::ok)
            .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/templates")
    @Operation(summary = "Get all templates")
    @PreAuthorize("hasAnyRole('ADMIN', 'EVALUATOR')")
    public ResponseEntity<Page<NotificationTemplateResponse>> getTemplates(
            @RequestHeader("X-Tenant-ID") String tenantId,
            Pageable pageable) {

        Page<NotificationTemplateResponse> templates = templateService.getTemplates(tenantId, pageable);
        return ResponseEntity.ok(templates);
    }

    @PutMapping("/templates/{id}")
    @Operation(summary = "Update a template")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<NotificationTemplateResponse> updateTemplate(
            @PathVariable UUID id,
            @Valid @RequestBody NotificationTemplateRequest request,
            @RequestHeader("X-Tenant-ID") String tenantId) {

        return templateService.updateTemplate(id, request, tenantId)
            .map(ResponseEntity::ok)
            .orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/templates/{id}")
    @Operation(summary = "Delete a template")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> deleteTemplate(
            @PathVariable UUID id,
            @RequestHeader("X-Tenant-ID") String tenantId) {

        boolean deleted = templateService.deleteTemplate(id, tenantId);
        return deleted ? ResponseEntity.noContent().build() : ResponseEntity.notFound().build();
    }

    // ==================== PREFERENCES ====================

    @GetMapping("/preferences/{userId}")
    @Operation(summary = "Get user notification preferences")
    @PreAuthorize("hasAnyRole('ADMIN', 'EVALUATOR') or #userId == authentication.name")
    public ResponseEntity<List<NotificationPreferenceResponse>> getUserPreferences(
            @PathVariable String userId,
            @RequestHeader("X-Tenant-ID") String tenantId) {

        List<NotificationPreferenceResponse> preferences = preferenceService.getUserPreferences(userId, tenantId);
        return ResponseEntity.ok(preferences);
    }

    @PutMapping("/preferences/{userId}")
    @Operation(summary = "Update user notification preference")
    @PreAuthorize("hasRole('ADMIN') or #userId == authentication.name")
    public ResponseEntity<NotificationPreferenceResponse> updatePreference(
            @PathVariable String userId,
            @Valid @RequestBody NotificationPreferenceRequest request,
            @RequestHeader("X-Tenant-ID") String tenantId) {

        NotificationPreferenceResponse response = preferenceService.upsertPreference(userId, request, tenantId);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/preferences/{userId}")
    @Operation(summary = "Delete all user preferences (GDPR)")
    @PreAuthorize("hasRole('ADMIN') or #userId == authentication.name")
    public ResponseEntity<Void> deleteUserPreferences(
            @PathVariable String userId,
            @RequestHeader("X-Tenant-ID") String tenantId) {

        preferenceService.deleteUserPreferences(userId, tenantId);
        return ResponseEntity.noContent().build();
    }
}
