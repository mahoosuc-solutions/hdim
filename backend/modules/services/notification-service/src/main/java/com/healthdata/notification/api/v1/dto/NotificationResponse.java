package com.healthdata.notification.api.v1.dto;

import com.healthdata.notification.domain.model.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NotificationResponse {

    private UUID id;
    private String tenantId;
    private String recipientId;
    private String recipientEmail;
    private NotificationChannel channel;
    private String subject;
    private String body;
    private NotificationStatus status;
    private NotificationPriority priority;
    private Instant scheduledAt;
    private Instant sentAt;
    private Instant deliveredAt;
    private String errorMessage;
    private Integer retryCount;
    private String correlationId;
    private Map<String, Object> metadata;
    private Instant createdAt;
    private Instant updatedAt;

    public static NotificationResponse fromEntity(Notification notification) {
        return NotificationResponse.builder()
            .id(notification.getId())
            .tenantId(notification.getTenantId())
            .recipientId(notification.getRecipientId())
            .recipientEmail(notification.getRecipientEmail())
            .channel(notification.getChannel())
            .subject(notification.getSubject())
            .body(notification.getBody())
            .status(notification.getStatus())
            .priority(notification.getPriority())
            .scheduledAt(notification.getScheduledAt())
            .sentAt(notification.getSentAt())
            .deliveredAt(notification.getDeliveredAt())
            .errorMessage(notification.getErrorMessage())
            .retryCount(notification.getRetryCount())
            .correlationId(notification.getCorrelationId())
            .metadata(notification.getMetadata())
            .createdAt(notification.getCreatedAt())
            .updatedAt(notification.getUpdatedAt())
            .build();
    }
}
