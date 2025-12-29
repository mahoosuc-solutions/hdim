package com.healthdata.notification.application;

import com.healthdata.notification.api.v1.dto.NotificationResponse;
import com.healthdata.notification.api.v1.dto.SendNotificationRequest;
import com.healthdata.notification.domain.model.*;
import com.healthdata.notification.domain.repository.NotificationPreferenceRepository;
import com.healthdata.notification.domain.repository.NotificationRepository;
import com.healthdata.notification.domain.repository.NotificationTemplateRepository;
import com.healthdata.notification.infrastructure.providers.EmailProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Core notification service handling message creation, preference checking, and delivery routing.
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class NotificationService {

    private final NotificationRepository notificationRepository;
    private final NotificationTemplateRepository templateRepository;
    private final NotificationPreferenceRepository preferenceRepository;
    private final EmailProvider emailProvider;

    private static final Pattern TEMPLATE_VAR_PATTERN = Pattern.compile("\\{\\{(\\w+)}}");

    /**
     * Send a notification to a recipient.
     */
    @Transactional
    public NotificationResponse sendNotification(SendNotificationRequest request, String tenantId) {
        log.info("Processing notification request for recipient: {}, channel: {}",
            request.getRecipientId(), request.getChannel());

        // Check user preferences if enabled
        if (Boolean.TRUE.equals(request.getCheckPreferences())) {
            Optional<NotificationPreference> pref = preferenceRepository
                .findByUserIdAndChannelAndTenantId(request.getRecipientId(), request.getChannel(), tenantId);

            if (pref.isPresent() && !pref.get().getEnabled()) {
                log.info("Notification blocked: user {} has disabled {} channel",
                    request.getRecipientId(), request.getChannel());
                return createBlockedResponse(request, "User has disabled this channel");
            }

            // Check quiet hours
            if (Boolean.TRUE.equals(request.getRespectQuietHours()) && pref.isPresent()) {
                NotificationPreference preference = pref.get();
                LocalTime currentTime = LocalTime.now(ZoneId.of(preference.getTimezone()));
                if (preference.isInQuietHours(currentTime)) {
                    log.info("Notification queued: user {} is in quiet hours", request.getRecipientId());
                    // Queue for later delivery
                    return createQueuedNotification(request, tenantId, "Queued for quiet hours");
                }
            }
        }

        // Resolve template and generate content
        String subject = request.getSubject();
        String body = request.getBody();

        if (request.getTemplateId() != null || request.getTemplateName() != null) {
            Optional<NotificationTemplate> template = resolveTemplate(request, tenantId);
            if (template.isPresent()) {
                subject = processTemplate(template.get().getSubjectTemplate(), request.getTemplateVariables());
                body = processTemplate(template.get().getBodyTemplate(), request.getTemplateVariables());
            }
        }

        // Create notification record
        Notification notification = Notification.builder()
            .tenantId(tenantId)
            .recipientId(request.getRecipientId())
            .recipientEmail(request.getRecipientEmail())
            .recipientPhone(request.getRecipientPhone())
            .channel(request.getChannel())
            .templateId(request.getTemplateId())
            .subject(subject)
            .body(body)
            .priority(request.getPriority())
            .metadata(request.getMetadata())
            .status(NotificationStatus.PENDING)
            .build();

        notification = notificationRepository.save(notification);

        // Send asynchronously
        sendAsync(notification);

        return mapToResponse(notification);
    }

    /**
     * Send notification asynchronously.
     */
    @Async
    @Transactional
    public void sendAsync(Notification notification) {
        try {
            notification.setStatus(NotificationStatus.SENDING);
            notificationRepository.save(notification);

            String externalId = switch (notification.getChannel()) {
                case EMAIL -> emailProvider.send(
                    notification.getRecipientEmail(),
                    notification.getSubject(),
                    notification.getBody()
                );
                case SMS -> sendSms(notification);
                case PUSH -> sendPush(notification);
                case IN_APP -> storeInApp(notification);
            };

            notification.markSent(externalId);
            log.info("Notification {} sent successfully via {}", notification.getId(), notification.getChannel());

        } catch (Exception e) {
            log.error("Failed to send notification {}: {}", notification.getId(), e.getMessage());
            notification.markFailed(e.getMessage());
            notification.incrementRetry();
        }

        notificationRepository.save(notification);
    }

    /**
     * Get notification by ID.
     */
    public Optional<NotificationResponse> getNotification(UUID id, String tenantId) {
        return notificationRepository.findByIdAndTenantId(id, tenantId)
            .map(this::mapToResponse);
    }

    /**
     * Get notifications for a recipient.
     */
    public Page<NotificationResponse> getNotificationsForRecipient(String recipientId, String tenantId, Pageable pageable) {
        return notificationRepository.findByRecipientIdAndTenantId(recipientId, tenantId, pageable)
            .map(this::mapToResponse);
    }

    /**
     * Send bulk notifications.
     */
    @Transactional
    public List<NotificationResponse> sendBulkNotifications(List<SendNotificationRequest> requests, String tenantId) {
        return requests.stream()
            .map(request -> sendNotification(request, tenantId))
            .toList();
    }

    // Private helper methods

    private Optional<NotificationTemplate> resolveTemplate(SendNotificationRequest request, String tenantId) {
        if (request.getTemplateId() != null) {
            return templateRepository.findByIdAndTenantId(request.getTemplateId(), tenantId);
        }
        if (request.getTemplateName() != null) {
            return templateRepository.findByNameAndChannelAndTenantId(
                request.getTemplateName(), request.getChannel(), tenantId);
        }
        return Optional.empty();
    }

    private String processTemplate(String template, Map<String, Object> variables) {
        if (template == null || variables == null) {
            return template;
        }

        StringBuffer result = new StringBuffer();
        Matcher matcher = TEMPLATE_VAR_PATTERN.matcher(template);

        while (matcher.find()) {
            String varName = matcher.group(1);
            Object value = variables.get(varName);
            String replacement = value != null ? value.toString() : "";
            matcher.appendReplacement(result, Matcher.quoteReplacement(replacement));
        }
        matcher.appendTail(result);

        return result.toString();
    }

    private NotificationResponse createBlockedResponse(SendNotificationRequest request, String reason) {
        return NotificationResponse.builder()
            .recipientId(request.getRecipientId())
            .channel(request.getChannel())
            .status(NotificationStatus.CANCELLED)
            .errorMessage(reason)
            .build();
    }

    private NotificationResponse createQueuedNotification(SendNotificationRequest request, String tenantId, String reason) {
        Notification notification = Notification.builder()
            .tenantId(tenantId)
            .recipientId(request.getRecipientId())
            .channel(request.getChannel())
            .status(NotificationStatus.QUEUED)
            .build();
        notification = notificationRepository.save(notification);
        return mapToResponse(notification);
    }

    private String sendSms(Notification notification) {
        // TODO: Implement SMS provider (Twilio/SNS)
        log.warn("SMS sending not yet implemented");
        return "sms-" + UUID.randomUUID();
    }

    private String sendPush(Notification notification) {
        // TODO: Implement push notification provider (Firebase/APNs)
        log.warn("Push notification sending not yet implemented");
        return "push-" + UUID.randomUUID();
    }

    private String storeInApp(Notification notification) {
        // In-app notifications are stored in DB and fetched by clients
        return "in-app-" + notification.getId();
    }

    private NotificationResponse mapToResponse(Notification notification) {
        return NotificationResponse.builder()
            .id(notification.getId())
            .recipientId(notification.getRecipientId())
            .channel(notification.getChannel())
            .subject(notification.getSubject())
            .status(notification.getStatus())
            .priority(notification.getPriority())
            .sentAt(notification.getSentAt())
            .deliveredAt(notification.getDeliveredAt())
            .errorMessage(notification.getErrorMessage())
            .retryCount(notification.getRetryCount())
            .externalId(notification.getExternalId())
            .metadata(notification.getMetadata())
            .createdAt(notification.getCreatedAt())
            .updatedAt(notification.getUpdatedAt())
            .build();
    }
}
