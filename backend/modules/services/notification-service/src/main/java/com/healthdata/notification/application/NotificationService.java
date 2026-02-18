package com.healthdata.notification.application;

import com.healthdata.metrics.HealthcareMetrics;
import com.healthdata.notification.domain.model.*;
import com.healthdata.notification.domain.repository.NotificationRepository;
import com.healthdata.notification.domain.repository.NotificationPreferenceRepository;
import com.healthdata.notification.infrastructure.providers.NotificationProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.*;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class NotificationService {

    private final NotificationRepository notificationRepository;
    private final NotificationPreferenceRepository preferenceRepository;
    private final TemplateService templateService;
    private final ChannelRouter channelRouter;
    private final HealthcareMetrics healthcareMetrics;

    /**
     * Send a notification using a template.
     */
    public Notification sendNotification(SendNotificationRequest request) {
        log.info("Sending notification to {} via {} for tenant {}", 
            request.getRecipientId(), request.getChannel(), request.getTenantId());

        // Check user preferences
        if (!isNotificationAllowed(request.getTenantId(), request.getRecipientId(), request.getChannel())) {
            log.info("Notification blocked by user preferences for {} on channel {}", 
                request.getRecipientId(), request.getChannel());
            return createBlockedNotification(request, "Blocked by user preferences");
        }

        // Resolve template if provided
        String subject = request.getSubject();
        String body = request.getBody();
        
        if (request.getTemplateCode() != null) {
            NotificationTemplate template = templateService.getTemplateByCode(
                request.getTenantId(), request.getTemplateCode()
            ).orElseThrow(() -> new IllegalArgumentException(
                "Template not found: " + request.getTemplateCode()
            ));
            
            subject = templateService.renderTemplate(template.getSubjectTemplate(), request.getVariables());
            body = templateService.renderTemplate(template.getBodyTemplate(), request.getVariables());
        }

        // Create notification entity
        Notification notification = Notification.builder()
            .tenantId(request.getTenantId())
            .recipientId(request.getRecipientId())
            .recipientEmail(request.getRecipientEmail())
            .recipientPhone(request.getRecipientPhone())
            .channel(request.getChannel())
            .templateId(request.getTemplateId())
            .subject(subject)
            .body(body)
            .priority(request.getPriority() != null ? request.getPriority() : NotificationPriority.NORMAL)
            .scheduledAt(request.getScheduledAt())
            .metadata(request.getMetadata())
            .correlationId(request.getCorrelationId())
            .createdBy(request.getCreatedBy())
            .status(NotificationStatus.PENDING)
            .build();

        notification = notificationRepository.save(notification);

        // Send immediately if not scheduled
        if (notification.getScheduledAt() == null || notification.getScheduledAt().isBefore(Instant.now())) {
            sendNotificationAsync(notification);
        }

        return notification;
    }

    /**
     * Send notification asynchronously.
     */
    @Async
    public void sendNotificationAsync(Notification notification) {
        try {
            notification.setStatus(NotificationStatus.SENDING);
            notificationRepository.save(notification);

            NotificationProvider provider = channelRouter.getProvider(notification.getChannel());
            provider.send(notification);

            notification.markAsSent();
            notificationRepository.save(notification);
            
            log.info("Notification {} sent successfully via {}", notification.getId(), notification.getChannel());
            healthcareMetrics.recordMessageProcessed(notification.getChannel().name(), true);
        } catch (Exception e) {
            log.error("Failed to send notification {}: {}", notification.getId(), e.getMessage());
            notification.markAsFailed(e.getMessage());
            notificationRepository.save(notification);
            healthcareMetrics.recordMessageProcessed(
                notification.getChannel() != null ? notification.getChannel().name() : "UNKNOWN", false);
        }
    }

    /**
     * Send bulk notifications.
     */
    public List<Notification> sendBulkNotifications(List<SendNotificationRequest> requests) {
        return requests.stream()
            .map(this::sendNotification)
            .toList();
    }

    /**
     * Get notification by ID.
     */
    @Transactional(readOnly = true)
    public Optional<Notification> getNotification(UUID id, String tenantId) {
        return notificationRepository.findByIdAndTenantId(id, tenantId);
    }

    /**
     * Get notifications for a tenant with pagination.
     */
    @Transactional(readOnly = true)
    public Page<Notification> getNotifications(String tenantId, Pageable pageable) {
        return notificationRepository.findByTenantId(tenantId, pageable);
    }

    /**
     * Get notifications for a recipient.
     */
    @Transactional(readOnly = true)
    public Page<Notification> getNotificationsForRecipient(
            String tenantId, String recipientId, Pageable pageable) {
        return notificationRepository.findByTenantIdAndRecipientId(tenantId, recipientId, pageable);
    }

    /**
     * Retry failed notifications.
     */
    public void retryFailedNotifications() {
        List<Notification> failedNotifications = notificationRepository
            .findRetryableNotifications(NotificationStatus.FAILED);
        
        for (Notification notification : failedNotifications) {
            if (notification.canRetry()) {
                log.info("Retrying notification {}, attempt {}", 
                    notification.getId(), notification.getRetryCount() + 1);
                notification.setStatus(NotificationStatus.PENDING);
                notificationRepository.save(notification);
                sendNotificationAsync(notification);
            }
        }
    }

    /**
     * Process scheduled notifications.
     */
    public void processScheduledNotifications() {
        List<Notification> pendingNotifications = notificationRepository
            .findPendingNotificationsToSend(Instant.now());
        
        for (Notification notification : pendingNotifications) {
            sendNotificationAsync(notification);
        }
    }

    /**
     * Check if notification is allowed based on user preferences.
     */
    private boolean isNotificationAllowed(String tenantId, String recipientId, NotificationChannel channel) {
        Optional<NotificationPreference> preference = preferenceRepository
            .findByTenantIdAndUserIdAndChannel(tenantId, recipientId, channel);
        
        if (preference.isEmpty()) {
            return true; // Default to allowed if no preference set
        }

        NotificationPreference pref = preference.get();
        
        if (!pref.getEnabled()) {
            return false;
        }

        // Check quiet hours
        if (pref.isInQuietHours(LocalTime.now(ZoneId.of(pref.getTimezone())))) {
            return false;
        }

        return true;
    }

    private Notification createBlockedNotification(SendNotificationRequest request, String reason) {
        return Notification.builder()
            .tenantId(request.getTenantId())
            .recipientId(request.getRecipientId())
            .channel(request.getChannel())
            .subject(request.getSubject())
            .body(request.getBody() != null ? request.getBody() : "")
            .status(NotificationStatus.CANCELLED)
            .errorMessage(reason)
            .createdBy(request.getCreatedBy())
            .build();
    }

    /**
     * Request DTO for sending notifications.
     */
    @lombok.Data
    @lombok.Builder
    @lombok.NoArgsConstructor
    @lombok.AllArgsConstructor
    public static class SendNotificationRequest {
        private String tenantId;
        private String recipientId;
        private String recipientEmail;
        private String recipientPhone;
        private NotificationChannel channel;
        private String templateCode;
        private UUID templateId;
        private String subject;
        private String body;
        private NotificationPriority priority;
        private Instant scheduledAt;
        private Map<String, Object> variables;
        private Map<String, Object> metadata;
        private String correlationId;
        private String createdBy;
    }
}
