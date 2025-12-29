package com.healthdata.notification.api.v1.dto;

import com.healthdata.notification.domain.model.NotificationChannel;
import com.healthdata.notification.domain.model.NotificationStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

/**
 * Response containing notification details and status.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class NotificationResponse {

    private UUID id;
    private String recipientId;
    private NotificationChannel channel;
    private String subject;
    private NotificationStatus status;
    private Integer priority;
    private Instant sentAt;
    private Instant deliveredAt;
    private String errorMessage;
    private Integer retryCount;
    private String externalId;
    private Map<String, Object> metadata;
    private Instant createdAt;
    private Instant updatedAt;
}
