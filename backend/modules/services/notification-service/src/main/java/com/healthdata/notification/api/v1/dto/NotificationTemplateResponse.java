package com.healthdata.notification.api.v1.dto;

import com.healthdata.notification.domain.model.NotificationChannel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Response containing notification template details.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class NotificationTemplateResponse {

    private UUID id;
    private String name;
    private String description;
    private NotificationChannel channel;
    private String subjectTemplate;
    private String bodyTemplate;
    private List<String> variables;
    private Boolean active;
    private Integer version;
    private Instant createdAt;
    private Instant updatedAt;
    private String createdBy;
}
