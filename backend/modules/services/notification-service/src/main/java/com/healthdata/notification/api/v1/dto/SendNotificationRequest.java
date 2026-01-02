package com.healthdata.notification.api.v1.dto;

import com.healthdata.notification.domain.model.NotificationChannel;
import com.healthdata.notification.domain.model.NotificationPriority;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
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
public class SendNotificationRequest {

    @NotBlank(message = "Recipient ID is required")
    private String recipientId;

    private String recipientEmail;

    private String recipientPhone;

    @NotNull(message = "Channel is required")
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
}
