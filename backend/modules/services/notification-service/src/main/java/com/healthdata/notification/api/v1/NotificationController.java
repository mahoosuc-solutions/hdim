package com.healthdata.notification.api.v1;

import com.healthdata.notification.api.v1.dto.*;
import com.healthdata.notification.application.NotificationService;
import com.healthdata.notification.domain.model.Notification;
import com.healthdata.notification.domain.model.NotificationChannel;
import com.healthdata.notification.domain.model.NotificationStatus;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/notifications")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Notifications", description = "Notification management endpoints")
public class NotificationController {

    private final NotificationService notificationService;

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN', 'SYSTEM')")
    @Operation(summary = "Send a notification")
    public ResponseEntity<NotificationResponse> sendNotification(
            @Valid @RequestBody SendNotificationRequest request,
            @RequestHeader("X-Tenant-ID") String tenantId,
            Authentication authentication) {
        
        log.info("Sending notification to {} via {} for tenant {}", 
            request.getRecipientId(), request.getChannel(), tenantId);

        NotificationService.SendNotificationRequest serviceRequest = 
            NotificationService.SendNotificationRequest.builder()
                .tenantId(tenantId)
                .recipientId(request.getRecipientId())
                .recipientEmail(request.getRecipientEmail())
                .recipientPhone(request.getRecipientPhone())
                .channel(request.getChannel())
                .templateCode(request.getTemplateCode())
                .templateId(request.getTemplateId())
                .subject(request.getSubject())
                .body(request.getBody())
                .priority(request.getPriority())
                .scheduledAt(request.getScheduledAt())
                .variables(request.getVariables())
                .metadata(request.getMetadata())
                .correlationId(request.getCorrelationId())
                .createdBy(authentication.getName())
                .build();

        Notification notification = notificationService.sendNotification(serviceRequest);
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(NotificationResponse.fromEntity(notification));
    }

    @PostMapping("/bulk")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN', 'SYSTEM')")
    @Operation(summary = "Send bulk notifications")
    public ResponseEntity<BulkNotificationResponse> sendBulkNotifications(
            @Valid @RequestBody BulkNotificationRequest request,
            @RequestHeader("X-Tenant-ID") String tenantId,
            Authentication authentication) {
        
        log.info("Sending {} bulk notifications for tenant {}", 
            request.getNotifications().size(), tenantId);

        List<NotificationService.SendNotificationRequest> serviceRequests = 
            request.getNotifications().stream()
                .map(r -> NotificationService.SendNotificationRequest.builder()
                    .tenantId(tenantId)
                    .recipientId(r.getRecipientId())
                    .recipientEmail(r.getRecipientEmail())
                    .recipientPhone(r.getRecipientPhone())
                    .channel(r.getChannel())
                    .templateCode(r.getTemplateCode())
                    .templateId(r.getTemplateId())
                    .subject(r.getSubject())
                    .body(r.getBody())
                    .priority(r.getPriority())
                    .scheduledAt(r.getScheduledAt())
                    .variables(r.getVariables())
                    .metadata(r.getMetadata())
                    .correlationId(r.getCorrelationId())
                    .createdBy(authentication.getName())
                    .build())
                .toList();

        List<Notification> notifications = notificationService.sendBulkNotifications(serviceRequests);
        
        List<NotificationResponse> responses = notifications.stream()
            .map(NotificationResponse::fromEntity)
            .toList();

        long successCount = notifications.stream()
            .filter(n -> n.getStatus() != NotificationStatus.FAILED && 
                        n.getStatus() != NotificationStatus.CANCELLED)
            .count();

        return ResponseEntity.ok(BulkNotificationResponse.builder()
            .totalRequested(request.getNotifications().size())
            .successCount((int) successCount)
            .failedCount((int) (notifications.size() - successCount))
            .notifications(responses)
            .build());
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN', 'VIEWER')")
    @Operation(summary = "Get notification by ID")
    public ResponseEntity<NotificationResponse> getNotification(
            @PathVariable UUID id,
            @RequestHeader("X-Tenant-ID") String tenantId) {
        
        return notificationService.getNotification(id, tenantId)
            .map(n -> ResponseEntity.ok(NotificationResponse.fromEntity(n)))
            .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN', 'VIEWER')")
    @Operation(summary = "Get notifications for tenant")
    public ResponseEntity<Page<NotificationResponse>> getNotifications(
            @RequestHeader("X-Tenant-ID") String tenantId,
            @RequestParam(required = false) String recipientId,
            @RequestParam(required = false) NotificationStatus status,
            @RequestParam(required = false) NotificationChannel channel,
            Pageable pageable) {
        
        Page<Notification> notifications;
        
        if (recipientId != null) {
            notifications = notificationService.getNotificationsForRecipient(
                tenantId, recipientId, pageable);
        } else {
            notifications = notificationService.getNotifications(tenantId, pageable);
        }

        return ResponseEntity.ok(notifications.map(NotificationResponse::fromEntity));
    }
}
